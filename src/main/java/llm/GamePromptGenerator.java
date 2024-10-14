package llm;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import games.GameType;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class GamePromptGenerator {
    public enum TaskType {
        Heuristic;
        public String getTaskTest(GameType gameType, int nPlayers, String className) {
            if (this == Heuristic) {
                return "You are playing the board game " + gameType.name() + ". Your job is to write the evaluation logic to help an AI play this game. Don't leave parts unfinished or TODOs.\n" +
                        "Write it all in a Java class called " + className + ", with only a single function with this signature:\n" +
                        " - public double evaluateState(core.AbstractGameState gameState, int playerId)\n" +
                        "The variable gameState is the current state of the game, and playerId is the ID of the player we evaluate the state for. " +
                        "The return value is an estimate of the value of the state. This must be between 0.0 and 1.0. " +
                        "0.0 means we have no chance of winning, 0.50 means we have a 50% chance of winning, and 1.0 means we have won the game.\n" +
                        "The first thing you'll do is cast the abstract game state variable" +
                        "to the specific one we need: " + gameType.getGameStateClass().getSimpleName() + ".\n Write the contents of this function, so that we give a higher numeric " +
                        "evaluation to those game states that are beneficial to the player with the received playerId as id. " +
                        "Take into account the whole game state and possible opponent moves. There are " + (nPlayers-1) + " other players in the game.";
            }
            return "";
        }
    }

    public static String createLLMTaskPrompt(TaskType taskType, GameType gameType, int nPlayers, String className) {
        String result = "";

        // Task information
        result += "This is your task: " + taskType.getTaskTest(gameType, nPlayers, className);

        // Rulebook manual
        String rules = gameType.loadRulebook();
        result += "This is the description of the board game " + gameType.name() + ": " + rules + "\n";

        // API, game-type specific
        result += "You can use the following API to complete the task:\n";

        File sourceFile = new File("src/main/java/games/" + gameType.name().toLowerCase() + "/" + gameType.getGameStateClass().getSimpleName() + ".java");  // todo check

        // Extract methods using reflection
        // TODO: Have a specific list for core methods and enums (i.e. GameResult) + extract from game package.
        Map<String, List<Method>> methods = getAllMethods(gameType.getGameStateClass());

        // Extract Javadocs using JavaParser
        Map<String, String> javadocs = extractJavadocs(sourceFile);

        Map<String, String> fullClasses = new HashMap<>();
        for (String cl : methods.keySet()) {
            for (Method method : methods.get(cl)) {
                String fullMethod = method.toString().substring(method.toString().lastIndexOf(" "), method.toString().lastIndexOf("("));
                String methodAndPackage = fullMethod.substring(0, fullMethod.lastIndexOf("."));
                fullClasses.put(cl, methodAndPackage.trim());
            }
        }

        // Map methods to Javadocs
        for (String cl : methods.keySet()) {
            result += "From class " + fullClasses.get(cl) + ":\n";
            for (Method method : methods.get(cl)) {

                String signature = getMethodSignature(method);
                if (javadocs != null && javadocs.containsKey(signature)) {
                    result += " - " + signature + ": " + javadocs.get(signature) + "\n";
                } else {
                    result += " - " + signature + "\n";
                }

            }
        }

        result += "Assume all the other classes are implemented, and do not include a main function. Add all the import statements required.\n";

        return result;
    }

    public static String createLLMFeedbackPrompt(TaskType taskType, GameType gameType, int nPlayers, String className, String code) {
        String text = """
                The current best heuristic code is below.
                ```java
                %s
                ```
                Your task is to generate a new heuristic function that is better than the current one.
                A better heuristic will have a higher win rate and/or have shorter and less complex code.
                
                """;
        String result = String.format(text, code);
        String taskText = createLLMTaskPrompt(taskType, gameType, nPlayers, className);
        return result+taskText;
    }

    public static String createLLMErrorPrompt(TaskType taskType, GameType gameType, int nPlayers, String className, String code, String error) {
        String text = """
                A previous attempt at this task created the class below.
                This class had failed to compile correctly.
                ```java
                %s
                ```
                The error message is:
                %s
                
                Your immediate task is to rewrite this code to compile correctly.
                """;
        String result = String.format(text, code, error);
        String taskText = createLLMTaskPrompt(taskType, gameType, nPlayers, className);
        return taskText + result;
    }

    public static Map<String, List<Method>> getAllMethods(Class<?> clazz) {
        Map<String, List<Method>> methods = new HashMap<>();
        ArrayList<Class<?>> clazzez = new ArrayList<>();
        extractMethods(clazz, methods, clazzez);
        return methods;
    }

    static List<String> packagesToIgnore = List.of("java.lang", "java.util", "core", "core.components", "core.actions");
    static List<String> classesToOverride = List.of("GridBoard", "Deck", "PartialObservableDeck", "Dice");

    private static void extractMethods(Class<?> clazz, Map<String, List<Method>> methods, List<Class<?>> visitedClasses) {
        if (clazz == null || clazz == Object.class || clazz.isEnum() || visitedClasses.contains(clazz)) {
            System.out.println("Skipping " + clazz);
            return;
        }
        if (packagesToIgnore.contains(clazz.getPackageName()) && !classesToOverride.contains(clazz.getSimpleName())) {
            System.out.println("Ignoring " + clazz);
            return;
        }
        System.out.println("Extracting methods from " + clazz);

        visitedClasses.add(clazz);
        List<Method> methodList = new ArrayList<>();
        Set<String> objectMethodNames = getObjectMethodNames();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isPrivate(method.getModifiers()) && !objectMethodNames.contains(method.getName()) &&
                    method.getName().startsWith("get") &&
                    !method.getName().equals("get") &&
                    !method.getName().contains("String")) {
                methodList.add(method);
            }
        }

        if (!methodList.isEmpty()) {
            methods.put(clazz.getSimpleName(), methodList);
            // We also need to extract classes from the argument lists of the methods

            for (Method method : methodList) {
                for (Class<?> parameterType : method.getParameterTypes()) {
                    extractMethods(parameterType, methods, visitedClasses);
                }
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) &&
                    !field.getType().isPrimitive()) {
                extractMethods(field.getType(), methods, visitedClasses);
            }
        }

        extractMethods(clazz.getSuperclass(), methods, visitedClasses);
    }

    private static Set<String> getObjectMethodNames() {
        Set<String> methodNames = new HashSet<>();
        for (Method method : Object.class.getDeclaredMethods()) {
            methodNames.add(method.getName());
        }
        return methodNames;
    }

    public static Map<String, String> extractJavadocs(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            CompilationUnit cu = new JavaParser().parse(in).getResult().get();
            Map<String, String> javadocs = new HashMap<>();

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                cls.findAll(MethodDeclaration.class).forEach(method -> {
                    String signature = method.getDeclarationAsString(false, false, false);
                    method.getJavadoc().ifPresent(javadoc -> javadocs.put(signature, javadoc.getDescription().toText()));
                });
            });

            return javadocs;
        } catch (Exception e) {
            return null;
        }
    }

    private static String getMethodSignature(Method method) {
        StringBuilder signature = new StringBuilder(method.getReturnType().getSimpleName());
        signature.append(" ").append(method.getName()).append("(");
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                signature.append(", ");
            }
            signature.append(parameterTypes[i].getSimpleName());
        }
        signature.append(")");
        return signature.toString();
    }
}

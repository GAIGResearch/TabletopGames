package llm;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import games.GameType;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.*;
import java.util.*;

public class GamePromptGenerator {
    public enum TaskType {
        Heuristic;

        public String getTaskTest(GameType gameType, int nPlayers, String className) {
            if (this == Heuristic) {
                return "You are playing the board game " + gameType.name() + ". Your job is to write the evaluation logic to help an AI play this game. " +
                        "Don't leave parts unfinished or TODOs. Do not include any package name.\n" +
                        "Write it all in a Java class called " + className + ", with only a single function with this signature:\n" +
                        "\tpublic double evaluateState(core.AbstractGameState gameState, int playerId)\n" +
                        "The variable gameState is the current state of the game, and playerId is the ID of the player we evaluate the state for. " +
                        "The return value is an estimate of the value of the state. This must be between 0.0 and 1.0. " +
                        "0.0 means we have no chance of winning, 0.50 means we have a 50% chance of winning, and 1.0 means we have won the game.\n" +
                        "You do not need to check for the end of the game, as the game engine will do that and only call evaluateState() if the game is still in progress.\n" +
                        "The first thing you'll do is cast the abstract game state variable " +
                        "to the specific one we need: " + gameType.getGameStateClass().getSimpleName() + ".\n Write the contents of this function, so that we give a higher numeric " +
                        "evaluation to those game states that are beneficial to playerId. " +
                        "Ths first player has a playerId of 0, the second player has a playerId of 1, and so on. " +
                        "There are a total of " + nPlayers + " players in the game.\n";
            }
            return "";
        }
    }

    // Packages to ignore when extracting methods and javadoc to pass to the LLM
    static List<String> packagesToIgnore = List.of("java.lang", "java.util", "core", "core.actions", "core.components", "evaluation.optimisation",
            "java.util.function", "java.util.stream", "java.awt");
    // ... except for these classes in those otherwise ignored packages
    static List<String> classesToOverride = List.of("GridBoard", "Deck", "PartialObservableDeck",
            "GraphBoardWithEdges", "BoardNodeWithEdges",
            "Dice", "FrenchCard");

    public String createLLMTaskPrompt(TaskType taskType, GameType gameType, int nPlayers, String className, boolean includeRules) {
        StringBuilder result = new StringBuilder();

        // Task information
        result.append("This is your task: \n").append(taskType.getTaskTest(gameType, nPlayers, className));

        if (includeRules) {
            // Rulebook manual
            String rules = gameType.loadRulebook();
            result.append("This is the description of the board game ").append(gameType.name()).append(": \n").append(rules).append("\n");
        }
        // API, game-type specific
        result.append("\nAs well as standard java libraries you must use the following API to complete the task:\n");

        // Extract methods using reflection
        List<ClassData> classData = getAllMethods(gameType.getGameStateClass());

        for (ClassData data : classData) {
            // First, extract Javadocs using JavaParser
            String fullClassName = data.clazz.getPackageName() + "." + data.clazz.getSimpleName();
            File sourceFile = new File("src/main/java/" + fullClassName.replaceAll("\\.", "/") + ".java");
            Map<String, String> javadocs = extractJavadocs(sourceFile);

            result.append("\nClass API for ").append(data.clazz.getTypeName());
            // then check for generics in definition
            if (data.clazz.getTypeParameters().length > 0) {
                result.append("<");
                for (int i = 0; i < data.clazz.getTypeParameters().length; i++) {
                    if (i > 0) {
                        result.append(", ");
                    }
                    result.append(data.clazz.getTypeParameters()[i].getGenericDeclaration().getSimpleName());
                }
                result.append(">");
            }

            if (data.superClass != null && !data.superClass.equals("java.lang.Object")) {
                result.append(" extends ").append(data.superClass);
            }
            result.append("\n");
            //          if (!data.implementedInterfaces.isEmpty()) {
            //              result.append("Implements: ").append(data.implementedInterfaces).append("\n");
            //          }
            if (javadocs != null && javadocs.containsKey(data.clazz.getSimpleName())) {
                result.append("Javadoc: ").append(javadocs.get(data.clazz.getSimpleName())).append("\n");
            }
            if (data.classEnumsAsString != null) {
                result.append("Enum Values: ").append(data.classEnumsAsString).append("\n");
            }
            // Check for any public fields (excluding any enums)
            for (Field field : data.clazz.getDeclaredFields()) {
                if (Modifier.isPublic(field.getModifiers()) && !field.isEnumConstant()) {
                    result.append("\tpublic  ").append(field.getType().getSimpleName()).append(" ").append(field.getName()).append("\n");
                }
            }
            // then add public methods
            for (Method method : data.classMethods) {
                if (method.getModifiers() == Modifier.PUBLIC) {
                    String signature = getMethodSignature(method);
                    if (javadocs != null && javadocs.containsKey(signature)) {
                        result.append("\tpublic ").append(signature).append(": ").append(javadocs.get(signature)).append("\n");
                    } else {
                        result.append("\tpublic ").append(signature).append("\n");
                    }
                }
            }
        }

        result.append("\nDo not use any methods on the classes in the API unless they are explicitly defined above.\n")
                .append("Do not include a main function in the class you write. Add all the import statements required.\n");

        return result.toString();
    }

    public String createLLMFeedbackPrompt(TaskType taskType, GameType gameType, int nPlayers, String className, String code) {
        String text = """
                The current best heuristic code is below.
                ```java
                %s
                ```
                Your task is to generate a new heuristic function that is better than the current one.
                A better heuristic will have a higher win rate and/or have shorter and less complex code.
                You should aim to improve the heuristic function by modifying the existing code, changing relative weights, or adding (or removing) new features.
                
                """;
        String result = String.format(text, code);
        String taskText = createLLMTaskPrompt(taskType, gameType, nPlayers, className, true);
        return result + taskText;
    }

    public String createLLMErrorPrompt(TaskType taskType, GameType gameType, int nPlayers, String className, String code, String error) {
        String text = """
                
                A previous attempt at this task created the class below.
                This class failed to compile correctly.
                
                %s
                
                The compilation error message is:
                %s
                
                Your immediate task is to rewrite this code to compile correctly.
                """;
        String result = String.format(text, code, error);
        String taskText = createLLMTaskPrompt(taskType, gameType, nPlayers, className, false);
        return taskText + result;
    }

    public static class ClassData {
        public Class<?> clazz;
        public List<Method> classMethods;
        public String classEnumsAsString;
        public String superClass;
        public List<String> implementedInterfaces;
    }

    public List<ClassData> getAllMethods(Class<?> clazz) {
        Map<String, List<Method>> methods = new HashMap<>();
        Map<String, String> enums = new HashMap<>();
        ArrayList<Class<?>> clazzez = new ArrayList<>();
        extractMethods(clazz, methods, enums, clazzez);
        List<ClassData> retValue = new ArrayList<>();
        for (Class<?> cl : clazzez) {
            retValue.add(new ClassData() {{
                clazz = cl;
                classMethods = methods.get(cl.getSimpleName());
                classEnumsAsString = enums.get(cl.getSimpleName());
                if (cl.getSuperclass() != null) superClass = cl.getSuperclass().getName();
                implementedInterfaces = getObjectInterfaces(cl);
            }});
        }
        return retValue;
    }

    @SuppressWarnings("unchecked")
    private void extractMethods(Class<?> clazz, Map<String, List<Method>> methods, Map<String, String> enumValues, List<Class<?>> visitedClasses) {
        if (clazz == null || clazz.isPrimitive() || clazz.isArray() || clazz == Object.class || visitedClasses.contains(clazz)) {
            return;
        }
        if (packagesToIgnore.contains(clazz.getPackageName()) && !classesToOverride.contains(clazz.getSimpleName())) {
            //    System.out.println("Ignoring " + clazz);
            return;
        }

        System.out.println("Extracting methods from " + clazz);

        visitedClasses.add(clazz);
        List<Method> methodList = new ArrayList<>();
        Set<String> objectMethodNames = getObjectMethodNames();

        for (Method method : clazz.getDeclaredMethods()) {
            if (!Modifier.isPrivate(method.getModifiers()) && !objectMethodNames.contains(method.getName()) &&
                    (method.getName().startsWith("get") || method.getName().startsWith("is")) &&
                    !method.getName().equals("get") &&
                    !method.getName().contains("String")) {
                methodList.add(method);
            }
        }
        if (clazz.isEnum()) {
            // We extract a list of the possible enum values
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) clazz;
            Enum<?>[] enumConstants = enumClass.getEnumConstants();
            StringBuilder values = new StringBuilder();
            for (Enum<?> enumConstant : enumConstants) {
                values.append(enumConstant.name()).append(", ");
            }
            // remove the last comma
            values.delete(values.length() - 2, values.length());
            enumValues.put(clazz.getSimpleName(), values.toString());
        }

        methods.put(clazz.getSimpleName(), methodList);
        if (!methodList.isEmpty()) {
            // We also need to extract classes from the argument lists of the methods
            // and from the return types of the methods

            for (Method method : methodList) {
                if (method.getReturnType() != Void.TYPE) {
                    Class<?> returnType = method.getReturnType();
                    while (returnType.isArray()) {
                        returnType = returnType.getComponentType();
                    }
                    // take account of array possibilities
                    extractMethods(returnType, methods, enumValues, visitedClasses);

                    // then we also need to take account of generics
                    if (method.getGenericReturnType() instanceof ParameterizedType pType) {
                        for (Type type : pType.getActualTypeArguments()) {
                            if (type instanceof Class<?> parameterType) {
                                extractMethods(parameterType, methods, enumValues, visitedClasses);
                            }
                        }
                    }

                }
                for (Class<?> parameterType : method.getParameterTypes()) {
                    extractMethods(parameterType, methods, enumValues, visitedClasses);
                }
            }
        }

        for (Field field : clazz.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) &&
                    !field.getType().isPrimitive()) {
                extractMethods(field.getType(), methods, enumValues, visitedClasses);
            }
        }

        extractMethods(clazz.getSuperclass(), methods, enumValues, visitedClasses);
    }

    private Set<String> getObjectMethodNames() {
        Set<String> methodNames = new HashSet<>();
        for (Method method : Object.class.getDeclaredMethods()) {
            methodNames.add(method.getName());
        }
        return methodNames;
    }

    private List<String> getObjectInterfaces(Class<?> clazz) {
        List<String> interfaces = new ArrayList<>();
        for (Class<?> interf : clazz.getInterfaces()) {
            interfaces.add(interf.getName());
        }
        return interfaces;
    }

    public Map<String, String> extractJavadocs(File file) {
        try {
            FileInputStream in = new FileInputStream(file);
            CompilationUnit cu = new JavaParser().parse(in).getResult().get();
            Map<String, String> javadocs = new HashMap<>();

            cu.findAll(ClassOrInterfaceDeclaration.class).forEach(cls -> {
                cls.getJavadoc().ifPresent(javadoc -> javadocs.put(cls.getNameAsString(), javadoc.getDescription().toText()));
                cls.findAll(MethodDeclaration.class).forEach(method -> {
                    String signature = method.getDeclarationAsString(false, false, true);
                    method.getJavadoc().ifPresent(javadoc -> javadocs.put(signature, javadoc.getDescription().toText()));
                });
            });

            return javadocs;
        } catch (Exception e) {
            return null;
        }
    }

    private String getMethodSignature(Method method) {
        String returnType = getTypeDescription((method.getGenericReturnType()));
        StringBuilder signature = new StringBuilder(returnType);
        signature.append(" ").append(method.getName());
        if (method.getTypeParameters().length > 0) {
            signature.append("<");
            for (int i = 0; i < method.getTypeParameters().length; i++) {
                if (i > 0) {
                    signature.append(", ");
                }
                signature.append(method.getTypeParameters()[i].getClass().getSimpleName());
            }
            signature.append(">");
        }

        signature.append("(");
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (i > 0) {
                signature.append(", ");
            }
            signature.append(getTypeDescription(params[i].getParameterizedType()));
            signature.append(" ").append(params[i].getName());
        }
        signature.append(")");
        return signature.toString();
    }

    /**
     * Get a description of the type, including generics if present
     * However we only return the simple name + generics.
     * We don't include the package name.
     */
    private String getTypeDescription(Type type) {
        if (type instanceof Class<?> clazz) {
            return clazz.getSimpleName();
        }
        if (type instanceof ParameterizedType pType) {
            Type rawType = pType.getRawType();
            StringBuilder result = new StringBuilder();
            if (rawType instanceof Class<?> rawClass) {
                result.append(rawClass.getSimpleName());
            } else {
                result.append(rawType.getTypeName());
            }
            result.append("<");
            for (int i = 0; i < pType.getActualTypeArguments().length; i++) {
                if (i > 0) {
                    result.append(", ");
                }
                result.append(getTypeDescription(pType.getActualTypeArguments()[i]));
            }
            result.append(">");
            return result.toString();
        }
        return type.getTypeName();
    }

    public static void main(String[] args) {
        GamePromptGenerator generator = new GamePromptGenerator();
        System.out.println(generator.createLLMTaskPrompt(TaskType.Heuristic, GameType.Dominion, 2, "TicTacToeHeuristic", false));
    }
}

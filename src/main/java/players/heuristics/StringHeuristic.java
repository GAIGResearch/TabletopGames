package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.loveletter.LoveLetterGameState;
import games.tictactoe.TicTacToeGameState;

import javax.tools.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

public class StringHeuristic implements IStateHeuristic {

    private String className = "TicTacToeEvaluator";
    private String fileName = "llm/" + className + ".java";

    private String str;

    Object heuristicClass;
    Method heuristicFunction;


    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getHeuristicCode() {
        return str;
    }

    public void setHeuristicCode(String s) {
        this.str = s;
        compile();
    }

    public StringHeuristic(String fileName, String className) {
        this.fileName = fileName;
        this.className = className;
        loadFile();
        compile();
    }

    public StringHeuristic() {
        loadFile();
        compile();
    }

    private void loadFile() {

        // Read 'str' as whole text in fileName file:
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
            str = stringBuilder.toString();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void compile() {
        // Method string
        //String className = fileName.replaceAll(".*/(.*?)\\.java", "$1");
        String sourceCode = str;

        // Compile source code
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // Create a file object for the source code
        JavaFileObject javaFileObject = new SimpleJavaFileObject(
                URI.create("string:///" + className + ".java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return sourceCode;
            }
        };

        // Prepare a custom diagnostic listener that ignores notes on annotations
        DiagnosticListener<JavaFileObject> diagnosticListener = new DiagnosticListener<JavaFileObject>() {
            @Override
            public void report(Diagnostic<? extends JavaFileObject> diagnostic) {
                if (diagnostic.getKind() != Diagnostic.Kind.NOTE) {
                    System.out.println(diagnostic.getMessage(null));
                } else {
                    System.out.println("Heuristic loaded: " + fileName);
                }
            }
        };

        // Compile the source code
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnosticListener, null, null, List.of(javaFileObject));
        if (!task.call()) {
            throw new RuntimeException("Compilation failed.");
        }

        // Load the compiled class
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File("").toURI().toURL() });

            Class<?> dynamicClass = classLoader.loadClass(className);

            // Create an instance of the compiled class
            heuristicClass = dynamicClass.getDeclaredConstructor().newInstance();

            // Find and invoke the method using reflection
            if(className.equalsIgnoreCase("TicTacToeEvaluator"))
                heuristicFunction = dynamicClass.getMethod("evaluateState", TicTacToeGameState.class, int.class);
            else if(className.equalsIgnoreCase("LoveLetterEvaluator"))
                heuristicFunction = dynamicClass.getMethod("evaluateState", LoveLetterGameState.class, int.class);

        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        try {
            return (double) heuristicFunction.invoke(heuristicClass, gs, playerId);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public StateHeuristicType getType() {
        return StateHeuristicType.StringHeuristic;
    }
}

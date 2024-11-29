package players.heuristics;

import com.fasterxml.jackson.databind.annotation.NoClass;
import core.AbstractGameState;
import core.CoreConstants;
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

    private final String className;
    private final String fileName;

    private String str;

    Object heuristicClass;
    Method heuristicFunction;


    public String getFileName() {
        return fileName;
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
        // Replace class name in the source code
        String sourceCode = str.replaceAll("public class .*? \\{", "public class " + className + " {");

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

        // Compile the source code
        DiagnosticCollector<JavaFileObject> diagnosticsCollector = new DiagnosticCollector<JavaFileObject>();

        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager,
                diagnosticsCollector, null, null,
                List.of(javaFileObject));

        boolean success = task.call();
        if (!success) {
            StringBuilder sb = new StringBuilder();
            List<Diagnostic<? extends JavaFileObject>> diagnostics = diagnosticsCollector.getDiagnostics();
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {
                if (diagnostic.getKind() != Diagnostic.Kind.NOTE) {
                    // read error details from the diagnostic object
                    sb.append(diagnostic.getMessage(null)).append("\n");
                }
            }
            String error = String.format("Compilation error: %s", sb);
            throw new RuntimeException(error);
        } else {
            System.out.println("Heuristic loaded: " + fileName);
        }

        // Load the compiled class
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[]{new File("").toURI().toURL()});

            Class<?> dynamicClass = classLoader.loadClass(className);

            // Create an instance of the compiled class
            heuristicClass = dynamicClass.getDeclaredConstructor().newInstance();

            // Find and invoke the method using reflection
            heuristicFunction = dynamicClass.getMethod("evaluateState", AbstractGameState.class, int.class);

            classLoader.close();
        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | IOException | NoClassDefFoundError e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        CoreConstants.GameResult playerResult = gs.getPlayerResults()[playerId];
        if (playerResult == CoreConstants.GameResult.LOSE_GAME)
            return 0;
        if (playerResult == CoreConstants.GameResult.DRAW_GAME)
            return 0.5;
        if (playerResult == CoreConstants.GameResult.WIN_GAME)
            return 1;

        try {
            return (double) heuristicFunction.invoke(heuristicClass, gs, playerId);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Error invoking heuristic function as it returns a null value : ", e);
        }
    }

    @Override
    public String toString() {
        return "StringHeuristic: " + fileName;
    }
}

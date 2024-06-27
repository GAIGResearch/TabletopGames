package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
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
import java.util.Arrays;

public class StringHeuristic implements IStateHeuristic {

    private String fileName;
    private String str;

    Object heuristicClass;
    Method heuristicFunction;

    public StringHeuristic(String fileName) {
        // Read 'str' as whole text in fileName file:
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
            str = stringBuilder.toString();
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Method string
        String className = fileName.replaceAll(".*/(.*?)\\.java", "$1");
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

        // Compile the source code
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, Arrays.asList(javaFileObject));
        if (!task.call()) {
            throw new RuntimeException("Compilation failed.");
        }

        // Load the compiled class
//        ClassLoader classLoader = ToolProvider.getSystemToolClassLoader();
        try {
            URLClassLoader classLoader = URLClassLoader.newInstance(new URL[] { new File("").toURI().toURL() });

            Class<?> dynamicClass = classLoader.loadClass(className);

            // Create an instance of the compiled class
            heuristicClass = dynamicClass.getDeclaredConstructor().newInstance();

            // Find and invoke the method using reflection
            heuristicFunction = dynamicClass.getMethod("evaluateState", TicTacToeGameState.class, int.class);

        } catch (ClassNotFoundException | InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException | MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        try {
            return (double) heuristicFunction.invoke(heuristicClass, (TicTacToeGameState)gs, playerId);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
}

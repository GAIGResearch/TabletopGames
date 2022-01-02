package core.interfaces;

import core.Game;
import evaluation.TunableParameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public interface IGameHeuristic {


    /**
     * Returns a score for the game that should be maximised. It is anticipated that this will be
     * used after a game has completed (but that is not mandatory).
     * We use the Game instead of the AbstractGameState (see IStateHeuristic) because we may want
     * access to the Players in a Game, for example if we are tuning a game to have a Random player
     * do well (OK, that's a rather forced example, but you get the drift).
     * @param game - game state to evaluate and score.
     * @return - value of given state.
     */
    double evaluateGame(Game game);


    @SuppressWarnings("unchecked")
    static IGameHeuristic loadFromFile(String filename) {
        try {
            FileReader reader = new FileReader(filename);
            JSONParser jsonParser = new JSONParser();
            JSONObject rawData = (JSONObject) jsonParser.parse(reader);
            // We expect a class field to tell us the Class to use
            // then a set of parameter values
            String cl = (String) rawData.getOrDefault("class", "");
            JSONArray argArray = (JSONArray) rawData.getOrDefault("args", Collections.emptyList());
            Class<?>[] argClasses = new Class[argArray.size()];
            Object[] args = new Object[argArray.size()];
            for (int i = 0; i < argClasses.length; i++) {
                Object arg = argArray.get(i);
                args[i] = arg;
                if (arg instanceof Long) {
                    argClasses[i] = int.class;
                    args[i] = ((Long) arg).intValue();
                } else if (arg instanceof Double) {
                    argClasses[i] = double.class;
                } else if (arg instanceof String) {
                    argClasses[i] = String.class;
                } else {
                    throw new AssertionError("Unexpected arg in " + filename + " : " + arg);
                }
            }
            Class<?> clazz = Class.forName(cl);
            Constructor<?> constructor = clazz.getConstructor(argClasses);
            return (IGameHeuristic) constructor.newInstance(args);
        } catch (FileNotFoundException e) {
            throw new AssertionError("File not found to load IGameHeuristic : " + filename);
        } catch (IOException e) {
            throw new AssertionError("Problem reading file " + filename + " : " + e);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new AssertionError("Problem parsing JSON in " + filename);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Unknown class in " + filename + " : " + e);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            throw new AssertionError("No matching constructor for class found using " + filename);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
            throw new AssertionError("Error constructing class using " + filename);
        }
    }

}

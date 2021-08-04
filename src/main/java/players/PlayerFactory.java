package players;

import core.AbstractPlayer;
import evaluation.TunableParameters;
import org.apache.commons.math3.analysis.function.Abs;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.mcts.BasicMCTSPlayer;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.rmhc.RMHCParams;
import players.rmhc.RMHCPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.io.*;
import java.lang.reflect.Constructor;

/**
 * Factory class for creating AbstractPlayers from JSON configuration file.
 * All three methods return an AbstractPlayer, with the configuration defined in a JSON file, a JSONObject
 * or a JSON-format String respectively.
 * <p>
 * The crucial property in the JSON file is class:
 * "class" : "players.mcts.MCTSParams"
 * "class" : "players.simple.RandomPlayer"
 * "class" : "players.simple.OSLAPlayer"
 * <p>
 * This must either be the class name of an AbstractPlayer implementation which has a no-argument constructor
 * (OSLAPlayer and RandomPlayer in the example above)
 * or the class name of a TunableParameters implementation which returns an AbstractPlayer from instantiate()
 * <p>
 * "random" and "osla" require no further properties.
 * "heuristic" requires a further property of:
 * "class" : "<fullNameOfClassThatImplementsAbstractPlayerWithANoArgumentConstructor>"
 */
public class PlayerFactory {

    private static final JSONParser parser = new JSONParser();

    private static AbstractPlayer fromJSONFile(FileReader reader, String fileName) {
        try {
            JSONObject json = (JSONObject) parser.parse(reader);
            AbstractPlayer retValue = fromJSONObject(json);
            retValue.setName(fileName.substring(0, fileName.indexOf(".")));
            return retValue;
        } catch (IOException | ParseException e) {
            throw new AssertionError("Error processing file " + fileName + " : " + e.getMessage());
        }
    }

    public static AbstractPlayer fromJSONString(String json) {
        try {
            return fromJSONObject((JSONObject) parser.parse(json));
        } catch (ParseException e) {
            throw new AssertionError("Error processing JSON string " + e.getMessage());
        }
    }

    public static AbstractPlayer fromJSONObject(JSONObject json) {
        // first of all we check for algorithm
        Object algo = json.get("class");
        if (!(algo instanceof String))
            throw new AssertionError("No valid class property in JSON file");

        String className = (String) algo;

        Object instantiatedObject;
        try {
            Class<?> clazz = Class.forName(className);
            Constructor<?> constructor = clazz.getConstructor();
            instantiatedObject = constructor.newInstance();
        } catch (Exception e) {
            System.out.println("Error loading class " + className + " : " + e.getMessage());
            throw new AssertionError("Error loading Class");
        }

        if (instantiatedObject instanceof TunableParameters) {
            TunableParameters params = (TunableParameters) instantiatedObject;
            TunableParameters.loadFromJSON(params, json);
            return (AbstractPlayer) params.instantiate();
        }
        return (AbstractPlayer) instantiatedObject;
    }

    /**
     * This provides the main access point for generating a new AbstractPlayer
     * The input can be one of a few things:
     * 1) A JSON file - in which case this is used to generate a player using fromJSONFile()
     * 2) A simple String with any of:
     * "mcts", "rmhc", "osla", "random", "className"
     * The first four of these will return the appropriate player with default parameters
     * Anything else is interpreted as a class name that implements AbstractPlayer with a no-argument constructor
     *
     * @param data
     * @return
     */
    public static AbstractPlayer createPlayer(String data) {
        // The idea here is that we first check to see if data is a filename.
        // If it is then we go the JSON route
        // If not then we now support a short-hand method for some simple defaults

        try {
            FileReader reader = new FileReader(data);
            return fromJSONFile(reader, data);
        } catch (FileNotFoundException e) {
            // this is fine...we move along
        }
        // if we get here then the file does not exist

        String input = data.toLowerCase();
        switch (input) {
            case "random":
                return new RandomPlayer();
            case "osla":
                return new OSLAPlayer();
            case "mcts":
//                return new MCTSPlayer(new MCTSParams(System.currentTimeMillis()));
                return new BasicMCTSPlayer();
            case "rmhc":
                return new RMHCPlayer(new RMHCParams(System.currentTimeMillis()));
            default:
                throw new AssertionError("Unknown player key : " + input);
        }
    }

}

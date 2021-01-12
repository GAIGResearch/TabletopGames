package players;

import core.AbstractPlayer;
import evaluation.TunableParameters;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
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
 * The crucial property in the JSON file is algorithm:
 * "algorithm" : "mcts"
 * <p>
 * This can take any value for which PlayerParams exists that can be instantiated from a JSON file, plus:
 * - "random"
 * - "osla"
 * - "heuristic"
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
            return fromJSONObject(json);
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
        Object algo = json.get("algorithm");
        if (!(algo instanceof String))
            throw new AssertionError("No valid algorithm property in JSON file");

        String playerType = (String) algo;

        switch (playerType.toLowerCase()) {
            case "random":
                return new RandomPlayer();
            case "osla":
                return new OSLAPlayer();
            case "mcts":
                MCTSParams mctsParams = new MCTSParams(System.currentTimeMillis());
                TunableParameters.loadFromJSON(mctsParams, json);
                return new MCTSPlayer(mctsParams);
            case "rmhc":
                RMHCParams rmhcParams = new RMHCParams(System.currentTimeMillis());
                TunableParameters.loadFromJSON(rmhcParams, json);
                return new RMHCPlayer(rmhcParams);
            case "heuristic":
                String className = (String) json.get("class");
                if (className == null)
                    throw new AssertionError("No class name specified for heuristic agent");
                try {
                    Class<?> clazz = Class.forName(className);
                    Constructor<?> constructor = clazz.getConstructor();
                    return (AbstractPlayer) constructor.newInstance();
                } catch (Exception e) {
                    System.out.println("Error loading heuristic class " + className + " : " + e.getMessage());
                    throw new AssertionError("Error loading Class");
                }
            default:
                throw new AssertionError("Abstract Player type not supported from JSON : " + playerType);
        }
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
                return new MCTSPlayer(new MCTSParams(System.currentTimeMillis()));
            case "rmhc":
                return new RMHCPlayer(new RMHCParams(System.currentTimeMillis()));
            default:
                try {
                    Class<?> clazz = Class.forName(data);
                    Constructor<?> constructor = clazz.getConstructor();
                    return (AbstractPlayer) constructor.newInstance();
                } catch (Exception e) {
                    System.out.println("Error loading heuristic class " + data + " : " + e.getMessage());
                    throw new AssertionError("Error loading Class");
                }
        }
    }

}

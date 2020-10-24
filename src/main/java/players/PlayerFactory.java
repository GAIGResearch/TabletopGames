package players;

import core.AbstractPlayer;
import org.json.simple.*;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;

import java.io.*;

public class PlayerFactory {

    private static final JSONParser parser = new JSONParser();

    public static AbstractPlayer fromJSONFile(String jsonFile) {
        try {
            FileReader reader = new FileReader(jsonFile);
            JSONObject json = (JSONObject) parser.parse(reader);
            return fromJSONObject(json);
        } catch (IOException | ParseException e) {
            throw new AssertionError("Error processing file " + jsonFile + " : " + e.getMessage());
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
                return new MCTSPlayer(MCTSParams.fromJSON(json));
            case "rmhc":
                throw new AssertionError("RMHC from JSON Not yet implemented");
            case "heuristic":
                String className = (String) json.get("class");
                if (className == null)
                    throw new AssertionError("No class name specified for heuristic agent");
                try {
                    return (AbstractPlayer) Class.forName(className).newInstance();
                } catch (Exception e) {
                    System.out.println("Error loading heuristic class " + className + " : " + e.getMessage());
                    throw new AssertionError("Error loading Class");
                }
            default:
                throw new AssertionError("Abstract Player type not supported from JSON : " + playerType);
        }

        // then we can populate the relevant Params if it needs it
    }

}

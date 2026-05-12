package games.backgammon;

import core.AbstractParameters;
import core.components.Dice;
import core.components.Token;
import evaluation.optimisation.TunableParameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import utilities.JSONUtils;

import java.util.*;

public class BGStateJSON {

    public static void loadFromJSON(BGGameState state, JSONObject json) {
        JSONObject abstractGS = (JSONObject) json.get("abstractGameState");
        state.loadAbstractGameStateFromJSON(abstractGS);
        String phaseStr = (String) abstractGS.get("gamePhase");
        state.setGamePhase(BGGamePhase.valueOf(phaseStr));

        state.piecesBorneOff = JSONUtils.intArrayFromJSON((JSONArray) json.get("piecesBorneOff"));
        state.blots = JSONUtils.intArrayFromJSON((JSONArray) json.get("blots"));
        state.diceUsed = JSONUtils.booleanArrayFromJSON((JSONArray) json.get("diceUsed"));
        state.availableDiceValues = JSONUtils.intArrayFromJSON((JSONArray) json.get("availableDiceValues"));

        JSONArray diceArray = (JSONArray) json.get("dice");
        state.dice = new Dice[diceArray.size()];
        for (int i = 0; i < diceArray.size(); i++) {
            state.dice[i] = new Dice((JSONObject) diceArray.get(i));
        }

        JSONArray countersArray = (JSONArray) json.get("counters");
        state.counters = new ArrayList<>();
        Map<Integer, Token> tokenMap = new HashMap<>();
        for (int i = 0; i < countersArray.size(); i++) {
            JSONArray pointArray = (JSONArray) countersArray.get(i);
            List<Token> pointTokens = new ArrayList<>();
            for (int j = 0; j < pointArray.size(); j++) {
                Token t = Token.loadFromJSON((JSONObject) pointArray.get(j));
                pointTokens.add(t);
                tokenMap.put(t.getComponentID(), t);
            }
            state.counters.add(pointTokens);
        }

        JSONArray movedArray = (JSONArray) json.get("movedThisTurn");
        state.movedThisTurn = new ArrayList<>();
        for (Object o : movedArray) {
            int id = ((Number) o).intValue();
            if (tokenMap.containsKey(id)) {
                state.movedThisTurn.add(tokenMap.get(id));
            }
        }

        state.playerTrackMapping = JSONUtils.intMatrixFromJSON((JSONArray) json.get("playerTrackMapping"));
    }

    public static JSONObject toJSON(BGGameState state) {
        JSONObject json = new JSONObject();
        json.put("class", "games.backgammon.BGGameState");

        // this includes parameters and game phase
        json.put("abstractGameState", state.abstractGameStateToJSON());
    //    json.put("gameParams", ((TunableParameters<?>) state.getGameParameters()).instanceToJSON(true, new HashMap<>()));

        json.put("piecesBorneOff", JSONUtils.intArrayToJSON(state.piecesBorneOff));
        json.put("blots", JSONUtils.intArrayToJSON(state.blots));
        json.put("diceUsed", JSONUtils.booleanArrayToJSON(state.diceUsed));
        json.put("availableDiceValues", JSONUtils.intArrayToJSON(state.availableDiceValues));

        JSONArray dice = new JSONArray();
        for (Dice d : state.dice) {
            dice.add(d.toJSON());
        }
        json.put("dice", dice);

        JSONArray counters = new JSONArray();
        for (List<Token> point : state.counters) {
            JSONArray pointArray = new JSONArray();
            for (Token t : point) {
                pointArray.add(t.toJSON());
            }
            counters.add(pointArray);
        }
        json.put("counters", counters);

        JSONArray movedThisTurn = new JSONArray();
        for (Token t : state.movedThisTurn) {
            movedThisTurn.add(t.getComponentID());
        }
        json.put("movedThisTurn", movedThisTurn);
        json.put("playerTrackMapping", JSONUtils.intMatrixToJSON(state.playerTrackMapping));

        return json;
    }
}

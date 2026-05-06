package games.backgammon;

import core.CoreConstants;
import core.components.Dice;
import core.components.Token;
import core.components.Component;
import evaluation.optimisation.TunableParameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import utilities.JSONUtils;
import java.util.*;

public class BGStateJSON {

    public static void loadFromJSON(BGGameState state, JSONObject json) {
        try {
            JSONObject abstractGS = (JSONObject) json.get("abstractGameState");
            state.loadAbstractGameStateFromJSON(abstractGS);
            String phaseStr = (String) abstractGS.get("gamePhase");
            try {
                state.setGamePhase(BGGamePhase.valueOf(phaseStr));
            } catch (Exception e) {
                // If this fails, then it was either DefaultGamePhase (already handled) or something else
            }
        } catch (Exception e) {
            throw e;
        }

        state.piecesBorneOff = JSONUtils.intArrayFromJSON((JSONArray) json.get("piecesBorneOff"));
        state.blots = JSONUtils.intArrayFromJSON((JSONArray) json.get("blots"));
        state.diceUsed = JSONUtils.booleanArrayFromJSON((JSONArray) json.get("diceUsed"));
        state.availableDiceValues = JSONUtils.intArrayFromJSON((JSONArray) json.get("availableDiceValues"));

        JSONArray diceArray = (JSONArray) json.get("dice");
        state.dice = new Dice[diceArray.size()];
        for (int i = 0; i < diceArray.size(); i++) {
            JSONObject dJSON = (JSONObject) diceArray.get(i);
            int nSides = ((Number) dJSON.get("nSides")).intValue();
            Dice d;
            if (dJSON.containsKey("pdf")) {
                JSONArray pdfArray = (JSONArray) dJSON.get("pdf");
                double[] pdf = new double[pdfArray.size()];
                for (int j = 0; j < pdfArray.size(); j++) pdf[j] = ((Number) pdfArray.get(j)).doubleValue();
                d = new Dice(pdf);
            } else {
                d = new Dice(nSides);
            }
            d.setValue(((Number) dJSON.get("value")).intValue());
            setComponentID(d, ((Number) dJSON.get("id")).intValue());
            state.dice[i] = d;
        }

        JSONArray countersArray = (JSONArray) json.get("counters");
        state.counters = new ArrayList<>();
        Map<Integer, Token> tokenMap = new HashMap<>();
        for (int i = 0; i < countersArray.size(); i++) {
            JSONArray pointArray = (JSONArray) countersArray.get(i);
            List<Token> pointTokens = new ArrayList<>();
            for (int j = 0; j < pointArray.size(); j++) {
                JSONObject tJSON = (JSONObject) pointArray.get(j);
                Token t = new Token((String) tJSON.get("name"), ((Number) tJSON.get("id")).intValue());
                t.setOwnerId(((Number) tJSON.get("ownerId")).intValue());
                t.setTokenType((String) tJSON.get("tokenType"));
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
        json.put("gameParams", ((TunableParameters)state.getGameParameters()).instanceToJSON(false, new HashMap<>()));

        json.put("abstractGameState", state.abstractGameStateToJSON());

        json.put("piecesBorneOff", JSONUtils.intArrayToJSON(state.piecesBorneOff));
        json.put("blots", JSONUtils.intArrayToJSON(state.blots));
        json.put("diceUsed", JSONUtils.booleanArrayToJSON(state.diceUsed));
        json.put("availableDiceValues", JSONUtils.intArrayToJSON(state.availableDiceValues));

        JSONArray dice = new JSONArray();
        for (Dice d : state.dice) {
            JSONObject dJSON = new JSONObject();
            dJSON.put("value", d.getValue());
            dJSON.put("nSides", d.getNumberOfSides());
            double[] pdf = d.getPdf();
            if (pdf != null && pdf.length > 0) {
                JSONArray pdfArray = new JSONArray();
                for (double p : pdf) pdfArray.add(p);
                dJSON.put("pdf", pdfArray);
            }
            dJSON.put("id", d.getComponentID());
            dice.add(dJSON);
        }
        json.put("dice", dice);

        JSONArray counters = new JSONArray();
        for (List<Token> point : state.counters) {
            JSONArray pointArray = new JSONArray();
            for (Token t : point) {
                JSONObject tJSON = new JSONObject();
                tJSON.put("name", t.getComponentName());
                tJSON.put("ownerId", t.getOwnerId());
                tJSON.put("id", t.getComponentID());
                tJSON.put("tokenType", t.getTokenType());
                pointArray.add(tJSON);
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

    private static void setComponentID(Component c, int id) {
        try {
            java.lang.reflect.Field f = Component.class.getDeclaredField("componentID");
            f.setAccessible(true);
            f.set(c, id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

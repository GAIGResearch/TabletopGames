package games.backgammon;

import core.CoreConstants;
import core.components.Dice;
import core.components.Token;
import core.components.Component;
import evaluation.optimisation.TunableParameters;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

public class BGStateJSON {

    public static BGGameState fromJSON(JSONObject json) {
        BGGameState retValue = new BGGameState(
                TunableParameters.loadFromJSON(new BGParameters(), (JSONObject) json.get("gameParams")),
                ((Number) json.get("nPlayers")).intValue());
        loadFromJSON(retValue, json);
        return retValue;
    }

    public static void loadFromJSON(BGGameState state, JSONObject json) {
        try {
            state.setGameStatus(CoreConstants.GameResult.valueOf((String) json.get("gameStatus")));
            String phaseStr = (String) json.get("gamePhase");
            try {
                state.setGamePhase(BGGamePhase.valueOf(phaseStr));
            } catch (Exception e) {
                state.setGamePhase(CoreConstants.DefaultGamePhase.valueOf(phaseStr));
            }
            state.setFirstPlayer(((Number) json.get("firstPlayer")).intValue());
            state.setTurnOwner(((Number) json.get("turnOwner")).intValue());
            state.setAbstractFields(
                    ((Number) json.get("roundCounter")).intValue(),
                    ((Number) json.get("turnCounter")).intValue(),
                    ((Number) json.get("gameTick")).intValue(),
                    json.containsKey("gameID") ? ((Number) json.get("gameID")).intValue() : -1
            );
        } catch (Exception e) {
            throw e;
        }

        JSONArray playerResults = (JSONArray) json.get("playerResults");
        for (int i = 0; i < playerResults.size(); i++) {
            String resStr = (String) playerResults.get(i);
            CoreConstants.GameResult res = CoreConstants.GameResult.valueOf(resStr);
            state.setPlayerResult(res, i);
        }

        state.piecesBorneOff = intArrayFromJSON((JSONArray) json.get("piecesBorneOff"));
        state.blots = intArrayFromJSON((JSONArray) json.get("blots"));
        state.diceUsed = booleanArrayFromJSON((JSONArray) json.get("diceUsed"));
        state.availableDiceValues = intArrayFromJSON((JSONArray) json.get("availableDiceValues"));

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

        state.playerTrackMapping = intMatrixFromJSON((JSONArray) json.get("playerTrackMapping"));
    }

    public static JSONObject toJSON(BGGameState state) {
        JSONObject json = new JSONObject();
        json.put("gameParams", ((TunableParameters)state.getGameParameters()).instanceToJSON(false, new HashMap<>()));
        json.put("nPlayers", state.getNPlayers());

        json.put("gameStatus", state.getGameStatus().name());
        if (state.getGamePhase() instanceof Enum) {
            json.put("gamePhase", ((Enum) state.getGamePhase()).name());
        } else {
            json.put("gamePhase", state.getGamePhase().toString());
        }
        json.put("turnOwner", state.getTurnOwner());
        json.put("turnCounter", state.getTurnCounter());
        json.put("roundCounter", state.getRoundCounter());
        json.put("gameTick", state.getGameTick());
        json.put("firstPlayer", state.getFirstPlayer());
        json.put("gameID", state.getGameID());

        JSONArray playerResults = new JSONArray();
        for (CoreConstants.GameResult res : state.getPlayerResults()) {
            playerResults.add(res.name());
        }
        json.put("playerResults", playerResults);

        json.put("piecesBorneOff", intArrayToJSON(state.piecesBorneOff));
        json.put("blots", intArrayToJSON(state.blots));
        json.put("diceUsed", booleanArrayToJSON(state.diceUsed));
        json.put("availableDiceValues", intArrayToJSON(state.availableDiceValues));

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
        json.put("playerTrackMapping", intMatrixToJSON(state.playerTrackMapping));

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

    private static JSONArray intArrayToJSON(int[] array) {
        JSONArray res = new JSONArray();
        if (array != null) {
            for (int i : array) res.add(i);
        }
        return res;
    }

    private static int[] intArrayFromJSON(JSONArray array) {
        if (array == null) return null;
        int[] res = new int[array.size()];
        for (int i = 0; i < array.size(); i++) res[i] = ((Number) array.get(i)).intValue();
        return res;
    }

    private static JSONArray booleanArrayToJSON(boolean[] array) {
        JSONArray res = new JSONArray();
        if (array != null) {
            for (boolean b : array) res.add(b);
        }
        return res;
    }

    private static boolean[] booleanArrayFromJSON(JSONArray array) {
        if (array == null) return null;
        boolean[] res = new boolean[array.size()];
        for (int i = 0; i < array.size(); i++) res[i] = (boolean) array.get(i);
        return res;
    }

    private static JSONArray intMatrixToJSON(int[][] matrix) {
        JSONArray res = new JSONArray();
        if (matrix != null) {
            for (int[] row : matrix) res.add(intArrayToJSON(row));
        }
        return res;
    }

    private static int[][] intMatrixFromJSON(JSONArray array) {
        if (array == null) return null;
        int[][] res = new int[array.size()][];
        for (int i = 0; i < array.size(); i++) res[i] = intArrayFromJSON((JSONArray) array.get(i));
        return res;
    }
}

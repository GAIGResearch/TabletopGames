package games.connect4;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import games.GameType;
import org.json.simple.JSONObject;
import org.junit.Test;
import utilities.JSONUtils;

import java.io.File;
import java.util.List;

import static org.junit.Assert.*;

public class JSONTest {

    private Connect4ForwardModel fm = new Connect4ForwardModel();

    /**
     * Plays the given columns in order, alternating players, returning the resulting state.
     */
    private Connect4GameState playGame(Connect4GameParameters params, int... columns) {
        Connect4GameState state = (Connect4GameState) GameType.Connect4.createGameState(params, 2);
        fm.setup(state);
        for (int col : columns) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            AbstractAction chosen = actions.get(0);
            for (AbstractAction a : actions) {
                if (a instanceof SetGridValueAction sg && sg.getX() == col) {
                    chosen = a;
                    break;
                }
            }
            fm.next(state, chosen);
        }
        return state;
    }

    @Test
    public void testJSON() {
        Connect4GameParameters params = new Connect4GameParameters();
        Connect4GameState state = playGame(params, 0, 1, 2, 3, 0, 1);

        state.setFirstPlayer(1);
        state.setTurnOwner(0);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        JSONObject json = state.toJSON();

        // The shared AbstractGameState fields are nested under "abstractGameState"
        assertTrue(json.containsKey("abstractGameState"));
        JSONObject abstractGS = (JSONObject) json.get("abstractGameState");
        assertEquals(1L, ((Number) abstractGS.get("firstPlayer")).longValue());
        assertEquals(0L, ((Number) abstractGS.get("turnOwner")).longValue());

        // Connect4-specific board fields
        assertEquals(params.width, ((Number) json.get("width")).intValue());
        assertEquals(params.height, ((Number) json.get("height")).intValue());

        // Load it back into a freshly set up state, as the GUI's load path does
        Connect4GameState newState = (Connect4GameState) GameType.Connect4.createGameState(params, 2);
        fm.setup(newState);
        Connect4StateJSON.loadFromJSON(newState, json);

        assertEquals(state.getFirstPlayer(), newState.getFirstPlayer());
        assertEquals(state.getTurnOwner(), newState.getTurnOwner());
        assertEquals(state.getGameStatus(), newState.getGameStatus());
        assertEquals(state.getGameTick(), newState.getGameTick());
        assertEquals(state.getGridBoard(), newState.getGridBoard());
    }

    @Test
    public void testJSONLoadFromFile() throws Exception {
        Connect4GameParameters params = new Connect4GameParameters();
        Connect4GameState state = playGame(params, 3, 3, 4, 2, 5, 1);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);

        JSONObject json = state.toJSON();
        File file = File.createTempFile("connect4State", ".json");
        JSONUtils.writeJSON(json, file.getPath());

        // Reload from the parsed-from-file JSON (exercises Long/Integer parsing quirks), via the
        // reflective dispatcher the Frontend uses
        JSONObject fromFile = JSONUtils.loadJSONFile(file.getPath());
        Connect4GameState loadedState = JSONUtils.loadClassFromJSON(fromFile);

        assertEquals(state, loadedState);
        // identity of the board (componentID) and every cell must survive the round-trip
        assertEquals(state.getGridBoard().getComponentID(), loadedState.getGridBoard().getComponentID());

        file.delete();
    }

    @Test
    public void parametersAreReadFromFile() throws Exception {
        // Use non-default parameters to confirm they are serialized and restored rather than the defaults
        Connect4GameParameters params = new Connect4GameParameters();
        params.setParameterValue("width", 8);
        params.setParameterValue("height", 7);
        params.setParameterValue("winCount", 5);

        Connect4GameState state = playGame(params, 0, 1, 0, 1);

        JSONObject json = state.toJSON();
        File file = File.createTempFile("connect4Params", ".json");
        JSONUtils.writeJSON(json, file.getPath());

        Connect4GameState loadedState = JSONUtils.loadClassFromJSON(JSONUtils.loadJSONFile(file.getPath()));
        Connect4GameParameters loadedParams = (Connect4GameParameters) loadedState.getGameParameters();

        assertEquals(8, loadedParams.width);
        assertEquals(7, loadedParams.height);
        assertEquals(5, loadedParams.winCount);
        assertEquals(state, loadedState);

        file.delete();
    }
}

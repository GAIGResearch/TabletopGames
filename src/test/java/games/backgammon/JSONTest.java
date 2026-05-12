package games.backgammon;

import core.AbstractGameState;
import core.CoreConstants;
import org.json.simple.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class JSONTest {

    @Test
    public void testJSON() throws Exception {
        BGParameters params = new BGParameters();
        BGGameState state = new BGGameState(params, 2);
        BGForwardModel fm = new BGForwardModel();
        fm.setup(state);
        state.rollDice();
        
        // Use reflection to call setGameID()
        java.lang.reflect.Method setGameIDMethod = AbstractGameState.class.getDeclaredMethod("setGameID", int.class);
        setGameIDMethod.setAccessible(true);
        setGameIDMethod.invoke(state, 1234);
        
        state.setFirstPlayer(1);
        state.setTurnOwner(0);
        state.setGameStatus(CoreConstants.GameResult.GAME_ONGOING);
        
        JSONObject json = state.toJSON();
        
        // Check if abstractGameState key exists
        assertTrue(json.containsKey("abstractGameState"));
        JSONObject abstractGS = (JSONObject) json.get("abstractGameState");
        assertEquals(1234L, ((Number) abstractGS.get("gameID")).longValue());
        assertEquals(1L, ((Number) abstractGS.get("firstPlayer")).longValue());
        assertEquals(0L, ((Number) abstractGS.get("turnOwner")).longValue());
        
        // Load it back
        BGGameState newState = new BGGameState(params, 2);
        fm.setup(newState); // Initialize it
        BGStateJSON.loadFromJSON(newState, json);
        
        assertEquals(state.getGameID(), newState.getGameID());
        assertEquals(state.getFirstPlayer(), newState.getFirstPlayer());
        assertEquals(state.getTurnOwner(), newState.getTurnOwner());
        assertEquals(state.getGameStatus(), newState.getGameStatus());
        assertEquals(state.getGameTick(), newState.getGameTick());
    }
}

package games.wonders7;

import games.wonders7.cards.Wonder7Card;
import org.junit.Before;
import org.junit.Test;

import static games.wonders7.Wonders7Constants.Resource.*;
import static games.wonders7.Wonders7Constants.createCardHash;
import static games.wonders7.cards.Wonder7Card.Type.RawMaterials;
import static org.junit.Assert.*;

public class scoring {

    Wonders7ForwardModel fm = new Wonders7ForwardModel();
    Wonders7GameParameters params;
    Wonders7GameState state;


    @Before
    public void setup() {
        params = new Wonders7GameParameters();
        params.setRandomSeed(4909);
        state = new Wonders7GameState(params, 4);
        fm.setup(state);
    }

    @Test
    public void checkGameScoreHasNoSideEffects() {

        for (int i = 0; i < 4; i++) {
            // 3 coins in starting resources
            assertEquals(1.0, state.getGameScore(i), 0.001);
        }
        state.getPlayerResources(0).put(Shield, 2);
        state.getPlayerResources(1).put(Shield, 3);
        state.getPlayerResources(2).put(Shield, 0);
        state.getPlayerResources(3).put(Shield, 2);

        // this should have no effect (unfortunately in the initial implementation it did)
        for (int i = 0; i < 4; i++) {
            assertEquals(1.0, state.getGameScore(i), 0.001);
        }
    }

    @Test
    public void militaryScoresAtEndOfAge() {

        state.getPlayerResources(0).put(Shield, 2);
        state.getPlayerResources(1).put(Shield, 3);
        state.getPlayerResources(2).put(Shield, 0);
        state.getPlayerResources(3).put(Shield, 2);

        fm.checkAgeEnd(state);
        // should do nothing yet
        for (int i = 0; i < 4; i++) {
            assertEquals(1.0, state.getGameScore(i), 0.001);
        }

        for (int i = 0; i < 4; i++) {
            state.getPlayerHand(i).clear();
            state.getPlayerHand(i).add(new Wonder7Card("Lumber Yard", RawMaterials, createCardHash(Wood)));
        }

        fm.checkAgeEnd(state);
        assertEquals(0, state.getGameScore(0), 0.001);
        assertEquals(3, state.getGameScore(1), 0.001);
        assertEquals(-1, state.getGameScore(2), 0.001);
        assertEquals(2, state.getGameScore(3), 0.001);

    }

    @Test
    public void scienceScoringBasic() {
        fail("Not yet implemented");
    }

    @Test
    public void scienceScoringWithWilds() {
        fail("Not yet implemented");
    }
}

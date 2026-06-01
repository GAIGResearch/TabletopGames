package games.hearts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.hearts.actions.Pass;
import games.hearts.actions.Play;
import games.hearts.metrics.HeartsActionFeatures;
import games.hearts.metrics.HeartsStateFeatures;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class TestHeartsFeatures {

    HeartsGameState state;
    HeartsForwardModel fm;

    @Before
    public void setup() {
        fm = new HeartsForwardModel();
        state = new HeartsGameState(new HeartsParameters(), 4);
        fm.setup(state);
    }

    @Test
    public void testHeartsActionFeatures() {
        HeartsActionFeatures actionFeatures = new HeartsActionFeatures();
        assertEquals(10, actionFeatures.names().length);
        assertEquals(10, actionFeatures.types().length);

        // Test Pass action
        FrenchCard card = state.getPlayerDecks().get(0).get(0);
        Pass passAction = new Pass(0, card);
        double[] passFeatures = actionFeatures.doubleVector(passAction, state, 0);
        
        assertEquals(1.0, passFeatures[0], 0.001); // isPass
        // Check suit
        for (int i=1; i<=4; i++) {
            if (i == card.suite.ordinal() + 1) assertEquals(1.0, passFeatures[i], 0.001);
            else assertEquals(0.0, passFeatures[i], 0.001);
        }
        assertEquals((double) card.number, passFeatures[5], 0.001); // rank
        
        // Test Play action
        // Move to PLAYING phase
        state.setGamePhase(HeartsGameState.Phase.PLAYING);
        Play playAction = new Play(0, card);
        double[] playFeatures = actionFeatures.doubleVector(playAction, state, 0);
        
        assertEquals(0.0, playFeatures[0], 0.001); // isPass
        for (int i=1; i<=4; i++) {
            if (i == card.suite.ordinal() + 1) assertEquals(1.0, playFeatures[i], 0.001);
            else assertEquals(0.0, playFeatures[i], 0.001);
        }
        assertEquals((double) card.number, playFeatures[5], 0.001); // rank
    }

    @Test
    public void testHeartsStateFeatures() {
        HeartsStateFeatures stateFeatures = new HeartsStateFeatures();
        double[] features = stateFeatures.doubleVector(state, 0);
        assertTrue(features.length > 0);
        
        // Check if normalized scores are between 0 and 1 (initially 0)
        assertEquals(0.0, features[1], 0.001); // currentScore
        assertEquals(0.0, features[2], 0.001); // opponentBestScore
    }
}

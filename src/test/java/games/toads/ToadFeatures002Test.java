package games.toads;

import games.toads.metrics.ToadFeatures002;
import org.junit.Before;
import org.junit.Test;

import static games.toads.ToadTestUtils.*;
import static org.junit.Assert.*;

/**
 * ToadFeatures002 with the Rulebook 3 cards. The Generals are found by type, and the per-card slots for
 * cards of value < 7 are indexed by the card's value (hand 13 + value, discards 22 + value); Generals in hand at
 * 20 / 21, discarded at 29 / 30.
 */
public class ToadFeatures002Test {

    ToadForwardModel fm = new ToadForwardModel();
    ToadGameState state;
    ToadFeatures002 features = new ToadFeatures002();

    @Before
    public void setUp() {
        ToadParameters params = new ToadParameters();
        params.setRandomSeed(933);
        params.setParameterValue("openingReturn", false);
        state = newState(params, fm);
        state.getDiscards(0).clear();
    }

    @Test
    public void handSlotsAreSetByValueAndGeneralsByType() {
        // the Scout first, so a position-indexed slot (13 + 0) differs from the value-indexed one (13 + 2)
        setHand(state, 0, scout(), generalHostages(), generalFlags());
        double[] v = features.doubleVector(state, 0);
        for (int i = 13; i <= 21; i++) {
            double expected = (i == 15 || i == 20 || i == 21) ? 1.0 : 0.0; // Scout 13+2, GENERAL_ONE 20, GENERAL_TWO 21
            assertEquals("slot " + i + " (" + features.names()[i] + ")", expected, v[i], 1e-9);
        }
    }

    @Test
    public void discardSlotsAreSetByValueAndGeneralsByType() {
        setHand(state, 0);
        state.getDiscards(0).add(bodyguard());
        state.getDiscards(0).add(generalFlags());
        double[] v = features.doubleVector(state, 0);
        for (int i = 22; i <= 30; i++) {
            double expected = (i == 28 || i == 30) ? 1.0 : 0.0; // Bodyguard 22+6, GENERAL_TWO 30
            assertEquals("slot " + i + " (" + features.names()[i] + ")", expected, v[i], 1e-9);
        }
    }

    @Test
    public void featureNamesUseTheRulebook3Cards() {
        String[] names = features.names();
        assertEquals(31, names.length);
        assertEquals("SIEGE_CANNON_IN_HAND", names[13]);
        assertEquals("BODYGUARD_IN_HAND", names[19]);
        assertEquals("SIEGE_CANNON_USED", names[22]);
        assertEquals("BODYGUARD_USED", names[28]);
    }
}

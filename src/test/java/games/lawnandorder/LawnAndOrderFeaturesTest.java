package games.lawnandorder;

import core.actions.AbstractAction;
import core.interfaces.IStateFeatureVector;
import games.lawnandorder.components.LawnCard;
import games.lawnandorder.components.RuleCard;
import core.interfaces.IActionFeatureVector;
import games.lawnandorder.actions.Continue;
import games.lawnandorder.actions.Pass;
import games.lawnandorder.actions.PlayObject;
import games.lawnandorder.features.LawnAndOrderActionFeatures;
import games.lawnandorder.features.LawnAndOrderFeatures;
import games.lawnandorder.features.LawnAndOrderFeaturesReduced;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static games.lawnandorder.LawnAndOrderTestUtils.*;
import static games.lawnandorder.components.LawnCard.Attribute.*;
import static org.junit.Assert.*;

/**
 * The two state feature vectors and the action feature vector: values on an arranged state, the values for a player
 * who is not active, and over random games that every value is finite and that a player's features are the same in
 * their own copy of the state (so they use nothing hidden from them).
 */
public class LawnAndOrderFeaturesTest {

    private static final double EPS = 1e-9;
    private final IStateFeatureVector full = new LawnAndOrderFeatures();
    private final IStateFeatureVector reduced = new LawnAndOrderFeaturesReduced();
    private final IActionFeatureVector actionFeatures = new LawnAndOrderActionFeatures();

    private LawnAndOrderGameState state;
    // hand cards for player 0: A is cited by No Pink when played, B is safe
    private final LawnCard cardA = card(FURNITURE, PINK, OVERSIZED);
    private final LawnCard cardB = card(ORNAMENT, RED, PLASTIC);

    /**
     * Two players, so both see both Insider Tips. No Pink is revealed; the tips are No Red and an Administrative
     * Error; the 13 other Rule cards are in the agenda and unseen. Lawn 0 has Furniture x2, Structure, Blue, Yellow,
     * Red and Plastic x3, with 3 Citations, and the hand is cardA and cardB.
     */
    @Before
    public void arrange() {
        state = newState(2, 42);
        List<RuleCard> tips = List.of(rule(RED), special(RuleCard.Special.ADMINISTRATIVE_ERROR));
        List<RuleCard> revealed = List.of(rule(PINK));
        List<RuleCard> agenda = new ArrayList<>(allRuleCards());
        agenda.removeAll(List.of(rule(RED), rule(PINK)));
        agenda.remove(special(RuleCard.Special.ADMINISTRATIVE_ERROR));
        setRules(state, agenda, tips, revealed);
        setLawn(state, 0, card(FURNITURE, BLUE, PLASTIC), card(FURNITURE, YELLOW, PLASTIC), card(STRUCTURE, RED, PLASTIC));
        putInHand(state, 0, cardA, cardB);
        keepOnlyInHand(state, 0, cardA, cardB);
        state.citations[0] = 3;
        assertAllCardsPresent(state);
    }

    private static double get(IStateFeatureVector fv, double[] values, String name) {
        int i = Arrays.asList(fv.names()).indexOf(name);
        assertTrue("no feature " + name, i >= 0);
        return values[i];
    }

    @Test
    public void namesMatchVectorLength() {
        assertEquals(10, reduced.names().length);
        assertEquals(48, full.names().length);
        assertEquals(reduced.names().length, reduced.doubleVector(state, 0).length);
        assertEquals(full.names().length, full.doubleVector(state, 0).length);
    }

    @Test
    public void arrangedStateFullFeatures() {
        double[] v = full.doubleVector(state, 0);
        assertEquals(3, get(full, v, "LAWN_SIZE"), EPS);
        assertEquals(1, get(full, v, "LAWN_PTS_TYPE"), EPS);    // Furniture x2
        assertEquals(0, get(full, v, "LAWN_PTS_COLOUR"), EPS);
        assertEquals(2, get(full, v, "LAWN_PTS_FEATURE"), EPS); // Plastic x3
        assertEquals(3, get(full, v, "LAWN_POINTS_USEFUL"), EPS);
        assertEquals(3, get(full, v, "LARGEST_GROUP"), EPS);
        assertEquals(6, get(full, v, "GROWABLE_GROUPS"), EPS);

        assertEquals(3, get(full, v, "CITATIONS"), EPS);
        assertEquals(3, get(full, v, "CITATION_LIMIT"), EPS);
        assertEquals(0, get(full, v, "CITATION_HEADROOM"), EPS);
        assertEquals(1, get(full, v, "ZERO_TOLERANCE_UNSEEN"), EPS);
        // unseen Standard Rules cite Furniture 2, Structure 1, Blue 1, Yellow 1, Plastic 3 (Red is on a tip)
        assertEquals(8.0 / 13, get(full, v, "EXPECTED_CITATIONS_NEXT"), EPS);
        // after the next play the limit is 4: No Furniture or No Plastic takes 3 Citations over it
        assertEquals(2.0 / 13, get(full, v, "P_BUST_NEXT"), EPS);
        assertEquals(3, get(full, v, "MAX_EXPOSURE"), EPS);
        assertEquals(1, get(full, v, "CONDEMNED_REVEALED"), EPS);
        assertEquals(1, get(full, v, "SAFE_ATTRIBUTES_KNOWN"), EPS);

        assertEquals(13, get(full, v, "AGENDA_REMAINING"), EPS);
        assertEquals(1, get(full, v, "RULES_REVEALED"), EPS);
        assertEquals(13, get(full, v, "UNSEEN_POOL_SIZE"), EPS);
        assertEquals(10, get(full, v, "UNSEEN_STANDARD_RULES"), EPS);
        assertEquals(1, get(full, v, "EMERGENCY_UNSEEN"), EPS);

        // cardA gains 1 (Furniture x3) and is cited by No Pink; cardB gains 3 (Red x2, Plastic x4) and is safe
        assertEquals(2, get(full, v, "HAND_SIZE"), EPS);
        assertEquals(3, get(full, v, "HAND_BEST_GAIN"), EPS);
        assertEquals(2, get(full, v, "HAND_MEAN_GAIN"), EPS);
        assertEquals(1, get(full, v, "HAND_SAFE_CARDS"), EPS);
        assertEquals(3, get(full, v, "HAND_BEST_SAFE_GAIN"), EPS);
        assertEquals(0, get(full, v, "HAND_MIN_IMMEDIATE_CITATIONS"), EPS);
        // each card adds two unseen Standard Rules' worth of exposure
        assertEquals(10.0 / 13, get(full, v, "HAND_MEAN_EXPOSURE"), EPS);

        assertEquals(0, get(full, v, "PASSED"), EPS);
        assertEquals(1, get(full, v, "PHASE_PLAY"), EPS);
        assertEquals(5, get(full, v, "OPP_HAND_SIZE_MEAN"), EPS);
        assertEquals(0, get(full, v, "OPP_PASSED"), EPS);
    }

    @Test
    public void zeroToleranceCanTakeThePlayerOver() {
        // limit after the next play is 4, or 3 if Zero Tolerance is revealed. With 4 Citations, Zero Tolerance and the
        // Standard Rules for Furniture, Structure, Blue, Yellow and Plastic each give a Cease & Desist
        state.citations[0] = 4;
        assertEquals(6.0 / 13, get(full, full.doubleVector(state, 0), "P_BUST_NEXT"), EPS);
    }

    @Test
    public void usefulPointsAreCappedAtTheShortfall() {
        state.trackScores[0][LawnCard.Category.FEATURE.ordinal()] = 9;
        double[] v = full.doubleVector(state, 0);
        assertEquals(2, get(full, v, "LAWN_POINTS_USEFUL"), EPS);
        // cardB's Plastic adds nothing now, so its gain is 1 (Red x2), the same as cardA's
        assertEquals(1, get(full, v, "HAND_BEST_GAIN"), EPS);
        assertEquals(0, get(full, v, "MIN_TRACK"), EPS); // the other tracks are still 0
    }

    @Test
    public void arrangedStateReducedFeatures() {
        double[] v = reduced.doubleVector(state, 0);
        assertArrayEquals(new double[]{0, 0, 0, 3, 3, 0, 8.0 / 13, 0, 13, 2}, v, EPS);
    }

    @Test
    public void passedPlayerHasNoRiskAndNoHand() {
        state.status[0] = LawnAndOrderGameState.PlayerStatus.PASSED;
        double[] v = full.doubleVector(state, 0);
        assertEquals(1, get(full, v, "PASSED"), EPS);
        assertEquals(5, get(full, v, "CITATION_HEADROOM"), EPS);
        for (String name : List.of("EXPECTED_CITATIONS_NEXT", "P_BUST_NEXT", "MAX_EXPOSURE", "HAND_SIZE",
                "HAND_BEST_GAIN", "HAND_SAFE_CARDS", "HAND_MEAN_EXPOSURE"))
            assertEquals(name, 0, get(full, v, name), EPS);
        // the lawn still counts
        assertEquals(3, get(full, v, "LAWN_POINTS_USEFUL"), EPS);

        double[] r = reduced.doubleVector(state, 0);
        assertArrayEquals(new double[]{0, 0, 0, 3, 3, 5, 0, 0, 13, 0}, r, EPS);
        // and player 1 sees an inactive opponent
        assertEquals(1, get(reduced, reduced.doubleVector(state, 1), "OPP_INACTIVE"), EPS);
        assertEquals(1, get(full, full.doubleVector(state, 1), "OPP_PASSED"), EPS);
    }

    @Test
    public void actionFeaturesForPlayingACard() {
        assertEquals(8, actionFeatures.names().length);
        // cardA: Furniture matches the lawn; cited by No Pink when it lands, which also raises the limit to 4. Then
        // any unseen Standard Rule on the lawn (Furniture, Structure, Blue, Yellow, Plastic, Oversized), or Zero
        // Tolerance, gives a Cease & Desist
        assertArrayEquals(new double[]{0, 0, 0, 1, 1, 1, 2.0 / 13, 7.0 / 13},
                actionFeatures.doubleVector(new PlayObject(0, cardA), state, 0), EPS);
        // cardB: Red and Plastic match; safe when it lands; then No Furniture or No Plastic gives a Cease & Desist
        assertArrayEquals(new double[]{0, 0, 0, 3, 0, 2, 2.0 / 13, 2.0 / 13},
                actionFeatures.doubleVector(new PlayObject(0, cardB), state, 0), EPS);
    }

    @Test
    public void actionFeaturesForContinueAndPass() {
        assertArrayEquals(new double[]{1, 2.0 / 13, 3, 0, 0, 0, 0, 0},
                actionFeatures.doubleVector(new Continue(0), state, 0), EPS);
        assertArrayEquals(new double[8], actionFeatures.doubleVector(new Pass(0), state, 0), EPS);
    }

    @Test
    public void featuresAreFiniteAndUseOnlyVisibleInformation() {
        for (int nPlayers = 2; nPlayers <= 5; nPlayers++) {
            for (long seed = 0; seed < 3; seed++) {
                LawnAndOrderGameState s = newState(nPlayers, seed);
                LawnAndOrderForwardModel fm = new LawnAndOrderForwardModel();
                Random rnd = new Random(seed);
                int steps = 0;
                while (s.isNotTerminal() && steps < 2000) {
                    for (int p = 0; p < nPlayers; p++)
                        for (IStateFeatureVector fv : List.of(full, reduced)) {
                            double[] v = fv.doubleVector(s, p);
                            for (int i = 0; i < v.length; i++)
                                assertTrue(fv.names()[i] + " = " + v[i], Double.isFinite(v[i]));
                            // a copy for p redeterminises everything p cannot see
                            assertArrayEquals(fv.getClass().getSimpleName() + " for player " + p + " at step " + steps,
                                    v, fv.doubleVector(s.copy(p), p), EPS);
                        }
                    List<AbstractAction> actions = fm.computeAvailableActions(s);
                    int current = s.getCurrentPlayer();
                    LawnAndOrderGameState copy = (LawnAndOrderGameState) s.copy(current);
                    for (AbstractAction a : actions) {
                        double[] v = actionFeatures.doubleVector(a, s, current);
                        for (int i = 0; i < v.length; i++)
                            assertTrue(actionFeatures.names()[i] + " = " + v[i], Double.isFinite(v[i]));
                        assertArrayEquals(a + " at step " + steps, v, actionFeatures.doubleVector(a, copy, current), EPS);
                    }
                    fm.next(s, actions.get(rnd.nextInt(actions.size())));
                    steps++;
                }
            }
        }
    }
}

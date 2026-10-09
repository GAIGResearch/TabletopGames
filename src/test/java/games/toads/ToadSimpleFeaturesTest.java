package games.toads;

import core.actions.AbstractAction;
import games.toads.actions.*;
import games.toads.metrics.ToadActionFeaturesSimple;
import games.toads.metrics.ToadFeaturesSimple;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static games.toads.ToadTestUtils.*;
import static org.junit.Assert.*;

/**
 * ToadFeaturesSimple and ToadActionFeaturesSimple: values on arranged states, and over random games that every value
 * is finite and that a player's features are the same in their own copy of the state (so they use nothing hidden
 * from them).
 */
public class ToadSimpleFeaturesTest {

    private static final double EPS = 1e-9;
    private final ToadForwardModel fm = new ToadForwardModel();
    private final ToadFeaturesSimple stateFeatures = new ToadFeaturesSimple();
    private final ToadActionFeaturesSimple actionFeatures = new ToadActionFeaturesSimple();
    private ToadGameState state;
    private int attacker, defender;

    @Before
    public void setUp() {
        // 4-card hands and the PLAY phase at once
        state = newState(tacticsParams(51), fm);
        attacker = state.getCurrentPlayer();
        defender = 1 - attacker;
        setHand(state, attacker, generalHostages(), siegeCannon(), scout(), bodyguard());
    }

    private double get(double[] values, String name) {
        int i = Arrays.asList(stateFeatures.names()).indexOf(name);
        assertTrue("no feature " + name, i >= 0);
        return values[i];
    }

    /** The action features before the one-hot block. */
    private static double[] base(double[] v) {
        return Arrays.copyOf(v, 7);
    }

    private static double named(String[] names, double[] values, String name) {
        int i = Arrays.asList(names).indexOf(name);
        assertTrue("no feature " + name, i >= 0);
        return values[i];
    }

    @Test
    public void namesMatchVectorLength() {
        assertEquals(10 + 4 * 9, stateFeatures.names().length);
        assertEquals(7 + 9, actionFeatures.names().length);
        assertEquals(stateFeatures.names().length, stateFeatures.doubleVector(state, 0).length);
        assertEquals(actionFeatures.names().length,
                actionFeatures.doubleVector(new PlayFieldCard(attacker, scout()), state, attacker).length);
    }

    @Test
    public void attackerAtTheStartOfTheGame() {
        double[] v = stateFeatures.doubleVector(state, attacker);
        assertEquals(0, get(v, "WAR_TWO"), EPS);
        assertEquals(0, get(v, "HOSTAGE_LEAD"), EPS);
        assertEquals(4, get(v, "BATTLES_LEFT"), EPS);   // 4 in hand and 5 in the deck
        assertEquals(1, get(v, "ATTACKER"), EPS);
        assertEquals(15.0 / 4, get(v, "HAND_MEAN"), EPS);
        assertEquals(1, get(v, "SIEGE_CANNON_IN_HAND"), EPS);
        assertEquals(0, get(v, "OPP_FIELD"), EPS);
        assertEquals(0, get(stateFeatures.doubleVector(state, defender), "ATTACKER"), EPS);
    }

    @Test
    public void defenderSeesTheAttackersFaceUpCard() {
        fm.next(state, new PlayFieldCard(attacker, state.getPlayerHand(attacker).get(0)));  // the General
        double[] v = stateFeatures.doubleVector(state, defender);
        assertEquals(7, get(v, "OPP_FIELD"), EPS);
        assertEquals(1, get(v, "OPP_GENERAL_ONE_FIELD"), EPS);
        assertEquals(1, oppFieldBlockSum(v), EPS);
    }

    /** The number of OPP_<TYPE>_FIELD features set. */
    private double oppFieldBlockSum(double[] v) {
        double sum = 0;
        for (int i = 0; i < v.length; i++)
            if (stateFeatures.names()[i].endsWith("_FIELD") && stateFeatures.names()[i].startsWith("OPP_")
                    && !stateFeatures.names()[i].equals("OPP_FIELD"))
                sum += v[i];
        return sum;
    }

    @Test
    public void opponentsEarlierCardsArePlayed() {
        state.getDiscards(attacker).add(scout());
        state.getDiscards(attacker).add(siegeCannon());
        double[] v = stateFeatures.doubleVector(state, defender);
        assertEquals(1, get(v, "OPP_SCOUT_PLAYED"), EPS);
        assertEquals(1, get(v, "OPP_SIEGE_CANNON_PLAYED"), EPS);
        assertEquals(0, get(v, "OPP_ASSASSIN_PLAYED"), EPS);
        assertEquals(0, get(v, "SCOUT_PLAYED"), EPS);
    }

    @Test
    public void attackerDoesNotSeeTheDefendersFaceUpCard() {
        fm.next(state, new PlayFieldCard(attacker, state.getPlayerHand(attacker).get(0)));
        state.fieldCards[defender] = bodyguard();
        double[] v = stateFeatures.doubleVector(state, attacker);
        assertEquals(0, get(v, "OPP_FIELD"), EPS);
        assertEquals(0, oppFieldBlockSum(v), EPS);
    }

    @Test
    public void hostageRaceAndCasualty() {
        state.battlesWon[0][attacker] = 3;
        state.battlesWon[0][defender] = 1;
        double[] v = stateFeatures.doubleVector(state, defender);
        assertEquals(-2, get(v, "HOSTAGE_LEAD"), EPS);
        assertEquals(1, get(v, "ANGRY"), EPS);
        assertEquals(0, get(v, "WAR_ONE_RESULT"), EPS);
        fm.endRound(state, defender);
        state.tieBreakers[defender] = scout();
        v = stateFeatures.doubleVector(state, defender);
        assertEquals(1, get(v, "WAR_TWO"), EPS);
        assertEquals(0, get(v, "HOSTAGE_LEAD"), EPS);
        assertEquals(-1, get(v, "WAR_ONE_RESULT"), EPS);
        assertEquals(2, get(v, "CASUALTY"), EPS);
    }

    @Test
    public void attackerActions() {
        assertArrayEquals(new double[]{0, 0, 0, 0, 1, 0, 0},
                base(actionFeatures.doubleVector(new PlayFieldCard(attacker, siegeCannon()), state, attacker)), EPS);
        assertArrayEquals(new double[]{0, 6, 0, 0, 0, 0, 0},
                base(actionFeatures.doubleVector(new PlayFlankCard(attacker, bodyguard()), state, attacker)), EPS);
    }

    @Test
    public void defenderActionsAgainstAGeneral() {
        state.fieldCards[attacker] = generalHostages();
        // an Assassin beats a General
        assertArrayEquals(new double[]{1, 6, -6, 1, 0, 0, 0},
                base(actionFeatures.doubleVector(new PlayDefenderCards(defender, assassinII(), bodyguard()), state, defender)), EPS);
        // a Siege Cannon in Defence always loses
        assertArrayEquals(new double[]{0, 6, 0, -1, 0, 1, 0},
                base(actionFeatures.doubleVector(new PlayDefenderCards(defender, siegeCannon(), bodyguard()), state, defender)), EPS);
        assertArrayEquals(new double[]{7, 2, 0, 0, 0, 0, 0},
                base(actionFeatures.doubleVector(new PlayDefenderCards(defender, generalFlags(), scout()), state, defender)), EPS);
    }

    @Test
    public void defenderActionsAgainstASiegeCannon() {
        state.fieldCards[attacker] = siegeCannon();
        // only a Saboteur beats a Siege Cannon in Attack
        assertEquals(1, actionFeatures.doubleVector(new PlayDefenderCards(defender, saboteurIII(), scout()), state, defender)[3], EPS);
        assertEquals(-1, actionFeatures.doubleVector(new PlayDefenderCards(defender, generalFlags(), scout()), state, defender)[3], EPS);
    }

    @Test
    public void cardsInHandAndPlayed() {
        state.getDiscards(attacker).add(tricksterII());
        double[] v = stateFeatures.doubleVector(state, attacker);
        for (String type : List.of("GENERAL_ONE", "SIEGE_CANNON", "SCOUT", "BODYGUARD"))
            assertEquals(type, 1, get(v, type + "_IN_HAND"), EPS);
        assertEquals(0, get(v, "ASSASSIN_IN_HAND"), EPS);
        assertEquals(1, get(v, "TRICKSTER_PLAYED"), EPS);
        assertEquals(0, get(v, "GENERAL_ONE_PLAYED"), EPS);

        // the face-up General has left the hand; the hidden Scout is still in it until revealed, but is played
        fm.next(state, new PlayFieldCard(attacker, state.getPlayerHand(attacker).get(0)));
        fm.next(state, new PlayFlankCard(attacker, inHand(state, attacker, ToadConstants.ToadCardType.SCOUT)));
        v = stateFeatures.doubleVector(state, attacker);
        assertEquals(0, get(v, "GENERAL_ONE_IN_HAND"), EPS);
        assertEquals(1, get(v, "GENERAL_ONE_PLAYED"), EPS);
        assertEquals(0, get(v, "SCOUT_IN_HAND"), EPS);
        assertEquals(1, get(v, "SCOUT_PLAYED"), EPS);
        assertEquals(1, get(v, "BODYGUARD_IN_HAND"), EPS);
    }

    @Test
    public void actionOneHotForTheCardsPlayed() {
        String[] names = actionFeatures.names();
        double[] v = actionFeatures.doubleVector(new PlayFlankCard(attacker, bodyguard()), state, attacker);
        assertEquals(1, named(names, v, "BODYGUARD_PLAY"), EPS);
        assertEquals(1, Arrays.stream(v, 7, v.length).sum(), EPS);
        state.fieldCards[attacker] = generalHostages();
        v = actionFeatures.doubleVector(new PlayDefenderCards(defender, assassinII(), siegeCannon()), state, defender);
        assertEquals(1, named(names, v, "ASSASSIN_PLAY"), EPS);
        assertEquals(1, named(names, v, "SIEGE_CANNON_PLAY"), EPS);
        assertEquals(2, Arrays.stream(v, 7, v.length).sum(), EPS);
        // returning a card is not playing it
        v = actionFeatures.doubleVector(new ReturnCardToDeck(berserkerII()), state, attacker);
        assertEquals(0, Arrays.stream(v, 7, v.length).sum(), EPS);
    }

    @Test
    public void returningACard() {
        assertArrayEquals(new double[]{0, 0, 0, 0, 0, 0, 5},
                base(actionFeatures.doubleVector(new ReturnCardToDeck(berserkerII()), state, attacker)), EPS);
    }

    @Test
    public void featuresAreFiniteAndUseOnlyVisibleInformation() {
        for (long seed = 0; seed < 20; seed++) {
            ToadParameters params = new ToadParameters();  // the defaults, with the opening return
            params.setRandomSeed(seed);
            ToadGameState s = newState(params, fm);
            Random rnd = new Random(seed);
            int steps = 0;
            while (s.isNotTerminal()) {
                for (int p = 0; p < 2; p++) {
                    double[] v = stateFeatures.doubleVector(s, p);
                    for (int i = 0; i < v.length; i++)
                        assertTrue(stateFeatures.names()[i] + " = " + v[i], Double.isFinite(v[i]));
                    assertArrayEquals("state features for player " + p + " at step " + steps + ", seed " + seed,
                            v, stateFeatures.doubleVector(s.copy(p), p), EPS);
                }
                int current = s.getCurrentPlayer();
                List<AbstractAction> actions = fm.computeAvailableActions(s);
                ToadGameState copy = (ToadGameState) s.copy(current);
                for (AbstractAction a : actions)
                    assertArrayEquals(a + " at step " + steps + ", seed " + seed,
                            actionFeatures.doubleVector(a, s, current), actionFeatures.doubleVector(a, copy, current), EPS);
                fm.next(s, actions.get(rnd.nextInt(actions.size())));
                steps++;
            }
        }
    }
}

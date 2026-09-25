package games.rummy;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.rummy.actions.Discard;
import games.rummy.actions.DrawCard;
import games.rummy.actions.LayOff;
import games.rummy.actions.Meld;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.rummy.RummyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Seeded multi-deal games (RummyParameters.targetScore > 0) played with random legal actions through fm.next,
 * checking the scores at each deal end, each new deal, and the final results.
 */
public class RummyMultiDealFlowTest {

    private static final int STEP_CAP = 20000;

    private static RummyParameters params(int targetScore, int maxTurnsPerDeal) {
        RummyParameters params = new RummyParameters();
        params.setParameterValue("targetScore", targetScore);
        params.setParameterValue("maxTurnsPerDeal", maxTurnsPerDeal);
        return params;
    }

    /** The turns played in each deal, and whether the game ended with a total at the target. */
    private record Match(List<Integer> turnsPerDeal, boolean reachedTarget) {
    }

    /** The action a seeded random policy picks. */
    private static AbstractAction choose(List<AbstractAction> all, Random rnd, boolean allActions) {
        if (allActions) {
            // a kind of action uniformly among those available, then an action of that kind
            List<Class<?>> kinds = all.stream().<Class<?>>map(Object::getClass).distinct().toList();
            Class<?> kind = kinds.get(rnd.nextInt(kinds.size()));
            List<AbstractAction> ofKind = all.stream().filter(kind::isInstance).toList();
            return ofKind.get(rnd.nextInt(ofKind.size()));
        }
        // draws and discards only
        List<AbstractAction> basic = all.stream().filter(a -> a instanceof DrawCard || a instanceof Discard).toList();
        return basic.get(rnd.nextInt(basic.size()));
    }

    /** The cards an action takes out of the hand. */
    private static List<FrenchCard> cardsPutDown(AbstractAction a) {
        if (a instanceof Discard d) return List.of(d.card);
        if (a instanceof Meld m) return m.cards;
        if (a instanceof LayOff l) return List.of(l.card);
        return List.of();
    }

    /** Plays a seeded multi-deal game to the end, checking every deal end and every new deal. */
    private Match playMatch(Game g, long seed, boolean allActions) {
        RummyGameState state = (RummyGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        RummyParameters params = (RummyParameters) state.getGameParameters();
        int n = state.getNPlayers();
        int target = params.targetScore;
        assertTrue("guard: a multi-deal game", target > 0);
        int handSize = params.handSize(n);
        Random rnd = new Random(seed);
        int[] totals = new int[n];
        int deal = 0, turnsInDeal = 0, steps = 0;
        List<Integer> turnsPerDeal = new ArrayList<>();

        while (state.isNotTerminal() && steps++ < STEP_CAP) {
            assertAllCardsPresent(state);
            int player = state.getCurrentPlayer();
            if (state.getGamePhase() == RummyGameState.Phase.DRAW) {
                assertEquals("deals completed", deal, state.getRoundCounter());
                assertEquals("turn counter within the deal", turnsInDeal, state.getTurnCounter());
                // deal d starts with player d % n, and play goes left
                assertEquals("player to act", (deal % n + turnsInDeal) % n, player);
            }
            AbstractAction chosen = choose(fm.computeAvailableActions(state), rnd, allActions);
            // the points and size of each hand once the action is taken (a deal end re-deals the hands)
            int[] points = new int[n];
            for (int p = 0; p < n; p++) points[p] = pointsByRule(state.getPlayerHand(p));
            List<FrenchCard> putDown = cardsPutDown(chosen);
            for (FrenchCard c : putDown) points[player] -= pointsByRule(c);
            boolean handEmptied = state.getPlayerHand(player).getSize() == putDown.size() && !putDown.isEmpty();

            fm.next(state, chosen);
            if (chosen instanceof Discard || handEmptied) turnsInDeal++;

            boolean dealEnded = !state.isNotTerminal() || state.getRoundCounter() != deal;
            if (!dealEnded) continue;
            turnsPerDeal.add(turnsInDeal);

            // the winner is the single fewest points in hand (an empty hand, 0, is always the single fewest)
            int fewest = Integer.MAX_VALUE, atFewest = 0, winner = -1, sum = 0;
            for (int p = 0; p < n; p++) {
                sum += points[p];
                if (points[p] < fewest) {
                    fewest = points[p];
                    atFewest = 1;
                    winner = p;
                } else if (points[p] == fewest) atFewest++;
            }
            if (handEmptied) assertEquals("the player who went out wins the deal", player, winner);
            int[] expected = totals.clone();
            if (atFewest == 1) expected[winner] += sum - points[winner];
            for (int p = 0; p < n; p++) {
                assertTrue("a total decreased", state.getPlayerScore(p) >= totals[p]);
                assertEquals("total of " + p + " after deal " + deal, expected[p], state.getPlayerScore(p));
                assertEquals(expected[p], state.getGameScore(p), 0.0);
            }
            totals = expected;
            int best = 0;
            for (int t : totals) best = Math.max(best, t);

            if (state.isNotTerminal()) {
                assertTrue("the game goes on with a total at the target", best < target);
                deal++;
                turnsInDeal = 0;
                assertEquals(deal, state.getRoundCounter());
                assertEquals("first player of deal " + deal, deal % n, state.getFirstPlayer());
                assertEquals(deal % n, state.getCurrentPlayer());
                assertEquals(0, state.getTurnCounter());
                for (int p = 0; p < n; p++)
                    assertEquals(handSize, state.getPlayerHand(p).getSize());
                assertEquals(1, state.getDiscardPile().getSize());
                assertEquals(52 - n * handSize - 1, state.getDrawDeck().getSize());
                assertTrue(state.getMelds().isEmpty());
                assertNull(state.getTakenCard());
                assertFalse(state.hasMeldedThisTurn());
                assertEquals(RummyGameState.Phase.DRAW, state.getGamePhase());
                assertAllCardsPresent(state);
            } else if (best < target) {
                // only the deal limit ends a game below the target
                assertEquals(100, state.getRoundCounter());
            }
        }
        assertFalse("the game did not end within " + STEP_CAP + " actions", state.isNotTerminal());

        // results by the totals: the best total wins, shared if tied
        int best = 0, atBest = 0;
        for (int t : totals) best = Math.max(best, t);
        for (int t : totals) if (t == best) atBest++;
        for (int p = 0; p < n; p++) {
            GameResult expectedResult = totals[p] != best ? LOSE_GAME : atBest > 1 ? DRAW_GAME : WIN_GAME;
            assertEquals("result of player " + p, expectedResult, state.getPlayerResults()[p]);
        }
        return new Match(turnsPerDeal, best >= target);
    }

    @Test
    public void twoPlayerGamesToOneHundredArePlayedOverSeveralDeals() {
        int deals = 0;
        for (long seed = 1; seed <= 3; seed++) {
            Match match = playMatch(newGame(2, seed, params(100, 200)), seed, true);
            assertTrue("seed " + seed + " ended below the target", match.reachedTarget());
            deals += match.turnsPerDeal().size();
        }
        assertTrue("guard: every game was a single deal", deals > 3);
    }

    @Test
    public void threePlayerGamesToOneHundredArePlayedOverSeveralDeals() {
        int deals = 0;
        for (long seed = 21; seed <= 23; seed++) {
            Match match = playMatch(newGame(3, seed, params(100, 200)), seed, true);
            assertTrue("seed " + seed + " ended below the target", match.reachedTarget());
            deals += match.turnsPerDeal().size();
        }
        assertTrue("guard: every game was a single deal", deals > 3);
    }

    @Test
    public void aFourPlayerGameToFiftyIsPlayedToTheEnd() {
        Match match = playMatch(newGame(4, 12, params(50, 200)), 12, true);
        assertTrue(match.reachedTarget());
    }

    @Test
    public void theTurnCapAppliesToEachDeal() {
        // with draws and discards only no hand empties, and the draw deck of 31 cards outlasts 10 turns, so every deal
        // runs to the cap
        int deals = 0;
        for (long seed = 14; seed <= 16; seed++) {
            Match match = playMatch(newGame(2, seed, params(100, 10)), seed, false);
            for (int turns : match.turnsPerDeal())
                assertEquals("seed " + seed + " deal turns", 10, turns);
            deals += match.turnsPerDeal().size();
        }
        assertTrue("guard: every game was a single deal", deals > 3);
    }

    // --- a pair differing only by targetScore: the same seeded play to the end of the first deal ---

    /** Plays draw/discard-only actions until the first deal ends; returns the number of actions taken. */
    private int playFirstDeal(Game g, long seed) {
        RummyGameState state = (RummyGameState) g.getGameState();
        AbstractForwardModel fm = g.getForwardModel();
        Random rnd = new Random(seed);
        int steps = 0;
        while (state.isNotTerminal() && state.getRoundCounter() == 0 && steps < 1000) {
            fm.next(state, choose(fm.computeAvailableActions(state), rnd, false));
            steps++;
        }
        return steps;
    }

    @Test
    public void withTargetScoreZeroTheGameEndsWithTheFirstDeal() {
        Game g = newGame(2, 17, params(0, 10));
        int steps = playFirstDeal(g, 17);
        RummyGameState state = (RummyGameState) g.getGameState();
        // 10 turns of a draw and a discard
        assertEquals(20, steps);
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getRoundCounter());
        for (int p = 0; p < 2; p++) {
            assertEquals(-pointsByRule(state.getPlayerHand(p)), state.getGameScore(p), 0.0);
            assertEquals(0, state.getPlayerScore(p));
        }
    }

    @Test
    public void withTargetScoreOneHundredTheSamePlayGoesOnToASecondDeal() {
        Game g = newGame(2, 17, params(100, 10));
        int steps = playFirstDeal(g, 17);
        RummyGameState state = (RummyGameState) g.getGameState();
        assertEquals(20, steps);
        // the winner of the first deal scored the other hand, here below 100, so a second deal starts
        assertTrue(state.getPlayerScore(0) + state.getPlayerScore(1) < 100);
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getTurnCounter());
        assertAllCardsPresent(state);
    }
}

package games.leducpoker;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.leducpoker.actions.Call;
import games.leducpoker.actions.Fold;
import games.leducpoker.actions.Raise;
import org.junit.Test;

import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.*;
import static games.leducpoker.LeducPokerTestUtils.*;
import static org.junit.Assert.*;

/**
 * Whole hands in real games from the factory, driven only through fm.next.
 */
public class LeducPokerGameFlowTest {

    static final Call CALL = new Call();
    static final Raise RAISE = new Raise();
    static final Fold FOLD = new Fold();

    @Test
    public void scriptedHandFromDealToShowdown() {
        Game game = newGame(3);
        LeducPokerGameState state = (LeducPokerGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        arrange(state, card("KH"), card("JS"), card("QS"));

        // round 0: P0 raises (1 + 2 = 3), P1 calls (3)
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, RAISE);
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(List.of(FOLD, CALL, RAISE), fm.computeAvailableActions(state));
        fm.next(state, CALL);
        assertEquals(List.of(card("QS")), state.getBoard().getComponents());
        assertEquals(3, state.getContribution(0));
        assertEquals(3, state.getContribution(1));

        // round 1: P0 checks, P1 bets (3 + 4 = 7), P0 re-raises (3 + 4 + 4 = 11), P1 calls (11)
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(CALL, RAISE), fm.computeAvailableActions(state));
        fm.next(state, CALL);
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, RAISE);
        assertEquals(7, state.getContribution(1));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, RAISE);
        assertEquals(11, state.getContribution(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals("two raises made this round", List.of(FOLD, CALL), fm.computeAvailableActions(state));
        fm.next(state, CALL);
        assertEquals(11, state.getContribution(1));

        // showdown: KH v JS with board QS, no pair, King high wins player 1's 11
        assertFalse(state.isNotTerminal());
        assertEquals(11, state.getNetChips(0));
        assertEquals(-11, state.getNetChips(1));
        assertEquals(WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(LOSE_GAME, state.getPlayerResults()[1]);
        assertAllCardsPresent(state);
    }

    @Test
    public void randomHandsEndWithTheSettlementTheRulesGive() {
        int folds = 0, showdowns = 0;
        for (long seed = 0; seed < 200; seed++) {
            Game game = newGame(seed);
            LeducPokerGameState state = (LeducPokerGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            Random rnd = new Random(seed);
            int steps = 0;
            int lastActor = -1;
            AbstractAction last = null;
            int[] before = new int[2];
            // at most 4 actions a round (check, raise, raise, call), so 8 a hand
            while (state.isNotTerminal() && steps++ < 20) {
                assertAllCardsPresent(state);
                for (int p = 0; p < 2; p++) {
                    double h = state.getHeuristicScore(p);
                    assertTrue("heuristic " + h, h >= 0 && h <= 1);
                }
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertFalse("no actions offered at step " + steps + " (seed " + seed + ")", actions.isEmpty());
                last = actions.get(rnd.nextInt(actions.size()));
                lastActor = state.getCurrentPlayer();
                before[0] = state.getContribution(0);
                before[1] = state.getContribution(1);
                fm.next(state, last);
            }
            assertFalse("hand did not end within 20 actions (seed " + seed + ")", state.isNotTerminal());
            assertAllCardsPresent(state);

            int net0 = state.getNetChips(0), net1 = state.getNetChips(1);
            assertEquals("zero-sum", 0, net0 + net1);
            assertTrue("|net| <= 13", Math.abs(net0) <= 13);

            int expected0;
            if (last instanceof Fold) {
                folds++;
                // the folder loses what they had put in; the other gains it
                int folder = lastActor;
                expected0 = folder == 0 ? -before[0] : before[1];
            } else {
                showdowns++;
                assertTrue("a hand not ended by a fold ends with a call", last instanceof Call);
                assertEquals(1, state.getBoard().getSize());
                // the closing call matches the larger contribution
                int stake = Math.max(before[0], before[1]);
                assertEquals(stake, state.getContribution(0));
                assertEquals(stake, state.getContribution(1));
                int winner = oracleWinner(state.getHand(0).get(0), state.getHand(1).get(0), state.getBoard().get(0));
                expected0 = winner == 0 ? stake : winner == 1 ? -stake : 0;
            }
            assertEquals("net chips of player 0 (seed " + seed + ")", expected0, net0);
            CoreConstants.GameResult r0 = expected0 > 0 ? WIN_GAME : expected0 < 0 ? LOSE_GAME : DRAW_GAME;
            CoreConstants.GameResult r1 = expected0 < 0 ? WIN_GAME : expected0 > 0 ? LOSE_GAME : DRAW_GAME;
            assertEquals(r0, state.getPlayerResults()[0]);
            assertEquals(r1, state.getPlayerResults()[1]);
        }
        assertTrue("no hand ended in a fold", folds > 0);
        assertTrue("no hand reached a showdown", showdowns > 0);
    }

    @Test
    public void randomMatchesLastExactlyNHandsAndSettleEachHandByTheRules() {
        int nHands = 5;
        int folds = 0, showdowns = 0, ties = 0;
        for (boolean highCardUsesBoard : new boolean[]{false, true}) {
            for (long seed = 0; seed < 40; seed++) {
                LeducPokerParameters params = new LeducPokerParameters();
                params.setParameterValue("nHands", nHands);
                params.setParameterValue("highCardUsesBoard", highCardUsesBoard);
                Game game = newGame(seed, params);
                LeducPokerGameState state = (LeducPokerGameState) game.getGameState();
                AbstractForwardModel fm = game.getForwardModel();
                Random rnd = new Random(seed);
                String where = " (seed " + seed + ", highCardUsesBoard " + highCardUsesBoard + ")";
                int handsEnded = 0;
                int steps = 0;
                // at most 8 actions a hand
                while (state.isNotTerminal() && steps++ < 20 * nHands) {
                    assertAllCardsPresent(state);
                    assertEquals("round counter" + where, handsEnded, state.getRoundCounter());
                    assertEquals("first player alternates" + where, handsEnded % 2, state.getFirstPlayer());
                    for (int p = 0; p < 2; p++) {
                        double h = state.getHeuristicScore(p);
                        assertTrue("heuristic " + h, h >= 0 && h <= 1);
                    }
                    FrenchCard c0 = state.getHand(0).get(0), c1 = state.getHand(1).get(0);
                    FrenchCard board = state.getBoard().getSize() == 1 ? state.getBoard().get(0) : null;
                    int[] contrib = {state.getContribution(0), state.getContribution(1)};
                    int net0Before = state.getNetChips(0);
                    int actor = state.getCurrentPlayer();
                    List<AbstractAction> actions = fm.computeAvailableActions(state);
                    assertFalse("no actions offered" + where, actions.isEmpty());
                    AbstractAction action = actions.get(rnd.nextInt(actions.size()));
                    fm.next(state, action);

                    boolean handOver = !state.isNotTerminal() || state.getRoundCounter() != handsEnded;
                    if (!handOver)
                        continue;
                    handsEnded++;
                    int delta0;
                    if (action instanceof Fold) {
                        folds++;
                        // the folder loses what they had put in; the other gains it
                        delta0 = actor == 0 ? -contrib[0] : contrib[1];
                    } else {
                        showdowns++;
                        assertTrue("a hand not ended by a fold ends with a call" + where, action instanceof Call);
                        assertNotNull("showdown before the board card" + where, board);
                        // the closing call matches the larger contribution
                        int stake = Math.max(contrib[0], contrib[1]);
                        int winner = oracleWinner(c0, c1, board, highCardUsesBoard);
                        if (winner == -1) ties++;
                        delta0 = winner == 0 ? stake : winner == 1 ? -stake : 0;
                    }
                    assertEquals("net chips of player 0 after hand " + handsEnded + where,
                            net0Before + delta0, state.getNetChips(0));
                    if (state.isNotTerminal()) {
                        // the next hand is dealt
                        assertEquals(0, state.getBoard().getSize());
                        assertEquals(4, state.getDrawDeck().getSize());
                        assertEquals(1, state.getContribution(0));
                        assertEquals(1, state.getContribution(1));
                        assertEquals(0, state.getRaisesThisRound());
                        assertEquals(0, state.getActionsThisRound());
                        assertEquals("next hand's first player acts" + where, handsEnded % 2, state.getCurrentPlayer());
                    }
                }
                assertFalse("match did not end" + where, state.isNotTerminal());
                assertEquals("hands played" + where, nHands, handsEnded);
                assertEquals(nHands - 1, state.getRoundCounter());
                assertAllCardsPresent(state);

                int net0 = state.getNetChips(0), net1 = state.getNetChips(1);
                assertEquals("zero-sum", 0, net0 + net1);
                // 5 * 13 = 65
                assertTrue("|net| <= nHands * maxContribution", Math.abs(net0) <= 65);
                CoreConstants.GameResult r0 = net0 > 0 ? WIN_GAME : net0 < 0 ? LOSE_GAME : DRAW_GAME;
                CoreConstants.GameResult r1 = net1 > 0 ? WIN_GAME : net1 < 0 ? LOSE_GAME : DRAW_GAME;
                assertEquals(r0, state.getPlayerResults()[0]);
                assertEquals(r1, state.getPlayerResults()[1]);
            }
        }
        assertTrue("no hand ended in a fold", folds > 0);
        assertTrue("no hand reached a showdown", showdowns > 0);
        assertTrue("no showdown tied", ties > 0);
    }
}

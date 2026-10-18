package games.cribbage;

import core.AbstractForwardModel;
import core.CoreConstants.GameResult;
import core.Game;
import core.actions.AbstractAction;
import games.cribbage.actions.DiscardToCrib;
import games.cribbage.actions.PlayCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.*;
import static games.cribbage.CribbageGameState.CribbageGamePhase.Discard;
import static games.cribbage.CribbageGameState.CribbageGamePhase.Play;
import static games.cribbage.CribbageTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration tests: real games driven only by fm.next.
 */
public class CribbageGameFlowTest {

    @Test
    public void scriptedRoundFromTheDealToTheNextDeal() {
        Game game = newGame(3);
        CribbageGameState state = (CribbageGameState) game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();
        // player 0 deals; player 1 is the non-dealer
        giveHand(state, 1, cards("10H", "4C", "8S", "7D", "QC", "5D"));
        giveHand(state, 0, cards("9H", "5C", "3S", "KD", "5S", "2C"));
        putOnTopOfDrawDeck(state, card("JD"));

        assertEquals(1, state.getCurrentPlayer());
        assertEquals(15, fm.computeAvailableActions(state).size());
        fm.next(state, new DiscardToCrib(card("QC"), card("5D")));
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new DiscardToCrib(card("5S"), card("2C")));
        assertAllCardsPresent(state);

        // the Jack of Diamonds is turned up: his heels, 2 to the dealer
        assertEquals(card("JD"), state.getStarter());
        assertEquals(Play, state.getGamePhase());
        assertEquals(2, state.getScore(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCard(card("10H")), new PlayCard(card("4C")), new PlayCard(card("8S")), new PlayCard(card("7D"))),
                new HashSet<>(fm.computeAvailableActions(state)));

        fm.next(state, new PlayCard(card("10H")));   // 10
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("5C")));    // 15: playFifteenPoints 1 to player 0
        assertEquals(3, state.getScore(0));
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("4C")));    // 19
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("KD")));    // 29
        // player 1 cannot play (8 -> 37, 7 -> 36), nor can player 0 (9 -> 38, 3 -> 32):
        // the count ends, last card to player 0, and player 1 leads
        assertEquals(4, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertTrue(state.getPlaySequence().isEmpty());
        assertEquals(1, state.getCurrentPlayer());
        assertAllCardsPresent(state);

        fm.next(state, new PlayCard(card("8S")));    // 8
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("3S")));    // 11
        assertEquals(1, state.getCurrentPlayer());
        fm.next(state, new PlayCard(card("7D")));    // 18
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(4, state.getScore(0));
        assertEquals(0, state.getScore(1));
        assertEquals(List.of(new PlayCard(card("9H"))), fm.computeAvailableActions(state));
        fm.next(state, new PlayCard(card("9H")));    // 27, the last card of the play

        // 8 3 7 9: no 15, pair or run; neither can play: last card 1 to player 0 (4 -> 5). Then the show, JD starter:
        // non-dealer (1) 10H 4C 8S 7D + JD: fifteen 8+7 = 2; no pair, run (7 8 10), flush or Jack in hand = 2
        // dealer (0) 5C KD 3S 9H + JD: fifteens 5+K, 5+J = 4; nothing else = 4
        // crib (to 0) QC 5D 5S 2C + JD: fifteens Q+5 x2, J+5 x2 = 8; pair 5-5 = 2; no run/flush/nobs = 10
        assertEquals(5 + 4 + 10, state.getScore(0));
        assertEquals(2, state.getScore(1));

        // the round is over: round 2, player 1 deals
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getDealer());
        assertEquals(Discard, state.getGamePhase());
        assertEquals(0, state.getCurrentPlayer());
        assertNull(state.getStarter());
        assertEquals(6, state.getPlayerHand(0).getSize());
        assertEquals(6, state.getPlayerHand(1).getSize());
        assertEquals(40, state.getDrawDeck().getSize());
        assertEquals(0, state.getCrib().getSize());
        assertTrue(state.isNotTerminal());
        assertAllCardsPresent(state);
    }

    @Test
    public void seededGamesWithRandomActionsRunToTheEnd() {
        for (long seed = 1; seed <= 10; seed++) {
            Game game = newGame(seed);
            CribbageGameState state = (CribbageGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            Random rnd = new Random(seed);
            int steps = 0, discards = 0, plays = 0;
            while (state.isNotTerminal() && steps++ < 500) {
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertFalse("no actions at step " + steps + " of seed " + seed, actions.isEmpty());
                if (state.getGamePhase() == Discard) {
                    assertEquals(15, actions.size());
                    discards++;
                } else {
                    plays++;
                }
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
                assertTrue(state.getRunningTotal() <= 31);
                assertAllCardsPresent(state);
            }
            assertFalse("seed " + seed + " did not end within 500 actions", state.isNotTerminal());
            // 2 rounds, each with 2 discards and 8 cards played
            assertEquals(4, discards);
            assertEquals(16, plays);

            GameResult[] results = state.getPlayerResults();
            int s0 = state.getScore(0), s1 = state.getScore(1);
            if (s0 == s1) {
                assertEquals(DRAW_GAME, results[0]);
                assertEquals(DRAW_GAME, results[1]);
            } else {
                int winner = s0 > s1 ? 0 : 1;
                assertEquals(WIN_GAME, results[winner]);
                assertEquals(LOSE_GAME, results[1 - winner]);
            }
        }
    }
}

package games.president;

import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.president.actions.Pass;
import games.president.actions.PlayCards;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * Real games driven with fm.next: a scripted deal played to its end, and seeded random games with invariants and a
 * results oracle.
 */
public class PresidentGameFlowTest {

    @Test
    public void aScriptedDealIsPlayedToTheEnd() {
        Game game = newGame(4, 61);
        PresidentGameState state = (PresidentGameState) game.getGameState();
        PresidentForwardModel fm = (PresidentForwardModel) game.getForwardModel();
        // small hands; the rest of the pack goes to the discard pile
        giveHand(state, 0, "3H", "3D", "9C", "KS");
        giveHand(state, 1, "4H", "4C", "10D", "AS");
        giveHand(state, 2, "5S", "5D", "JH", "2C");
        giveHand(state, 3, "6H", "7H", "QD", "QC");
        assertEquals(0, state.getCurrentPlayer());

        // trick 1: pairs, 3 < 4 < 5 < Q
        assertEquals(Set.of(new PlayCards(3, 1), new PlayCards(3, 2), new PlayCards(9, 1), new PlayCards(13, 1)),
                actionSet(state, fm));
        play(state, fm, 0, 3, 2);
        assertEquals(Set.of(new Pass(), new PlayCards(4, 2)), actionSet(state, fm));
        play(state, fm, 1, 4, 2);
        assertEquals(Set.of(new Pass(), new PlayCards(5, 2)), actionSet(state, fm));
        play(state, fm, 2, 5, 2);
        assertEquals(Set.of(new Pass(), new PlayCards(12, 2)), actionSet(state, fm));
        play(state, fm, 3, 12, 2);
        // no one else holds a pair
        assertEquals(Set.of(new Pass()), actionSet(state, fm));
        pass(state, fm, 0);
        pass(state, fm, 1);
        assertEquals(List.of(12, 12, 5, 5, 4, 4, 3, 3), numbers(state.getPlayPile()));
        pass(state, fm, 2);
        // cleared: player 3 played the Queens and leads
        assertEquals(0, state.getPlayPile().getSize());
        assertEquals(3, state.getCurrentPlayer());

        // trick 2: singles 6 < K < A < 2
        assertEquals(Set.of(new PlayCards(6, 1), new PlayCards(7, 1)), actionSet(state, fm));
        play(state, fm, 3, 6, 1);
        play(state, fm, 0, 13, 1);
        play(state, fm, 1, 14, 1);
        assertEquals(Set.of(new Pass(), new PlayCards(2, 1)), actionSet(state, fm));
        play(state, fm, 2, 2, 1);
        pass(state, fm, 3);
        pass(state, fm, 0);
        pass(state, fm, 1);
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(0, state.getPlayPile().getSize());

        // trick 3: player 2 goes out with the Jack; nobody can beat it
        play(state, fm, 2, 11, 1);
        assertEquals(List.of(2), state.getFinishingOrder());
        assertEquals(2, state.getPlayerScore(2));
        assertEquals(Set.of(new Pass()), actionSet(state, fm));
        pass(state, fm, 3);
        pass(state, fm, 0);
        pass(state, fm, 1);
        // cleared; player 2 is out, so player 3 leads
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Set.of(new PlayCards(7, 1)), actionSet(state, fm));

        // trick 4: player 3 goes out with the 7, player 0 with the 9; player 1 is left holding the 10
        play(state, fm, 3, 7, 1);
        assertEquals(List.of(2, 3), state.getFinishingOrder());
        assertEquals(1, state.getPlayerScore(3));
        assertTrue(state.isNotTerminal());
        assertEquals(Set.of(new Pass(), new PlayCards(9, 1)), actionSet(state, fm));
        play(state, fm, 0, 9, 1);

        assertFalse(state.isNotTerminal());
        assertEquals(List.of(2, 3, 0, 1), state.getFinishingOrder());
        assertArrayEquals(new int[]{0, 0, 2, 1}, state.playerScores);
        assertEquals(1, state.getOrdinalPosition(2));
        assertEquals(2, state.getOrdinalPosition(3));
        assertEquals(3, state.getOrdinalPosition(0));
        assertEquals(4, state.getOrdinalPosition(1));
        assertArrayEquals(new CoreConstants.GameResult[]{LOSE_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME},
                state.getPlayerResults());
        assertAllCardsPresent(state);
    }

    /**
     * Plays a seeded game with random legal actions to its end, checking invariants at every step and the results at
     * the end.
     */
    private void playRandomGame(int nPlayers, long seed) {
        Game game = newGame(nPlayers, seed);
        PresidentGameState state = (PresidentGameState) game.getGameState();
        PresidentForwardModel fm = (PresidentForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        List<Integer> wentOut = new ArrayList<>();
        int steps = 0;
        while (state.isNotTerminal() && steps++ < 3000) {
            int p = state.getCurrentPlayer();
            assertTrue("player " + p + " to act holds no cards", state.getPlayerHand(p).getSize() > 0);
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no legal actions for player " + p, actions.isEmpty());
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            int handBefore = state.getPlayerHand(p).getSize();
            long ofNumberBefore = action instanceof PlayCards pc ? countOf(state, p, pc.number) : 0;

            fm.next(state, action);

            assertAllCardsPresent(state);
            if (action instanceof PlayCards pc) {
                assertEquals(handBefore - pc.count, state.getPlayerHand(p).getSize());
                assertEquals(ofNumberBefore - pc.count, countOf(state, p, pc.number));
                if (state.getPlayerHand(p).getSize() == 0) {
                    wentOut.add(p);
                    assertEquals(p, (int) state.getFinishingOrder().get(wentOut.size() - 1));
                }
            } else {
                assertEquals(handBefore, state.getPlayerHand(p).getSize());
            }
        }
        assertFalse("game did not end within 3000 actions", state.isNotTerminal());

        // everyone but the Scum went out; the Scum is last in the finishing order and still holds cards
        List<Integer> order = state.getFinishingOrder();
        assertEquals(nPlayers, order.size());
        assertEquals(nPlayers - 1, wentOut.size());
        assertEquals(wentOut, order.subList(0, nPlayers - 1));
        int scum = order.get(nPlayers - 1);
        assertFalse(wentOut.contains(scum));
        assertTrue(state.getPlayerHand(scum).getSize() > 0);
        for (int i = 0; i < nPlayers; i++) {
            int p = order.get(i);
            // first out 2 (presidentPoints), second 1 (vicePresidentPoints), the rest 0
            int expectedScore = i == 0 ? 2 : i == 1 ? 1 : 0;
            assertEquals("score of player " + p, expectedScore, state.getPlayerScore(p));
            assertEquals("position of player " + p, i + 1, state.getOrdinalPosition(p));
            assertEquals("result of player " + p, i == 0 ? WIN_GAME : LOSE_GAME, state.getPlayerResults()[p]);
        }
    }

    private static long countOf(PresidentGameState state, int player, int number) {
        return state.getPlayerHand(player).getComponents().stream().filter((FrenchCard c) -> c.number == number).count();
    }

    @Test
    public void seededRandomGamesWithFourPlayersEndWithTheRightResults() {
        for (long seed = 1; seed <= 5; seed++)
            playRandomGame(4, seed);
    }

    @Test
    public void seededRandomGamesWithFivePlayersEndWithTheRightResults() {
        for (long seed = 11; seed <= 15; seed++)
            playRandomGame(5, seed);
    }

    @Test
    public void seededRandomGamesWithSevenPlayersEndWithTheRightResults() {
        for (long seed = 21; seed <= 25; seed++)
            playRandomGame(7, seed);
    }
}

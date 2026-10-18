package games.president;

import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import games.president.actions.GiveCard;
import games.president.actions.Pass;
import games.president.actions.PlayCards;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.GameResult.LOSE_GAME;
import static core.CoreConstants.GameResult.WIN_GAME;
import static games.president.PresidentGameState.Phase.EXCHANGE;
import static games.president.PresidentGameState.Phase.PLAY;
import static games.president.PresidentTestUtils.*;
import static org.junit.Assert.*;

/**
 * Games to a target score, driven with fm.next: a scripted deal end followed by the exchange and the next deal, and
 * seeded random games to targetScore 11.
 */
public class PresidentMultiDealFlowTest {

    @Test
    public void aDealEndLeadsThroughTwoGiveCardsToThePresidentsLead() {
        Game game = newGame(4, 62, multiDealParams(11, 2));
        PresidentGameState state = (PresidentGameState) game.getGameState();
        PresidentForwardModel fm = (PresidentForwardModel) game.getForwardModel();
        // player 2 is out (President); 0 goes out second, 3 third, and 1 is the Scum. Scores before: 0, 0, 2, 0
        arrangeOut(state, 2);
        state.playerScores[2] = 2;
        giveHand(state, 0, "5H");
        giveHand(state, 1, "3C", "4D");
        giveHand(state, 3, "KS");
        state.setTurnOwner(0);
        play(state, fm, 0, 5, 1);
        pass(state, fm, 1);
        play(state, fm, 3, 13, 1);

        // scores 1, 0, 2, 0 (below 11): a new deal, 13 each; the Scum's two highest cards go to the President
        assertTrue(state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertArrayEquals(new int[]{1, 0, 2, 0}, state.playerScores);
        assertEquals(List.of(13, 11, 15, 13), handSizes(state));
        assertEquals(EXCHANGE, state.getGamePhase());
        assertEquals(1, state.getScum());
        assertEquals(2, state.getCardsToGive());
        assertEquals(2, state.getCurrentPlayer());
        // the President now holds two cards above every card the Scum kept
        int scumBest = state.getPlayerHand(1).getComponents().stream().mapToInt(PresidentTestUtils::exchangeOrder).max().orElseThrow();
        assertTrue(state.getPlayerHand(2).getComponents().stream().filter(c -> exchangeOrder(c) > scumBest).count() >= 2);

        // the President gives back their two lowest cards, one decision each
        List<FrenchCard> lowest = new ArrayList<>(state.getPlayerHand(2).getComponents());
        lowest.sort(Comparator.comparingInt(PresidentTestUtils::exchangeOrder));
        assertEquals(giveCardsFor(state.getPlayerHand(2)), actionSet(state, fm));
        fm.next(state, new GiveCard(lowest.get(0)));
        assertEquals(EXCHANGE, state.getGamePhase());
        assertEquals(1, state.getCardsToGive());
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(giveCardsFor(state.getPlayerHand(2)), actionSet(state, fm));
        fm.next(state, new GiveCard(lowest.get(1)));

        assertEquals(List.of(13, 13, 13, 13), handSizes(state));
        assertTrue(state.getPlayerHand(1).contains(lowest.get(0)));
        assertTrue(state.getPlayerHand(1).contains(lowest.get(1)));
        assertEquals(PLAY, state.getGamePhase());
        assertEquals(-1, state.getScum());
        assertEquals(2, state.getCurrentPlayer());
        List<AbstractAction> lead = fm.computeAvailableActions(state);
        assertFalse(lead.contains(new Pass()));
        assertTrue(lead.stream().allMatch(a -> a instanceof PlayCards));

        // play continues clockwise from the President
        fm.next(state, lead.get(0));
        assertEquals(3, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    private static List<Integer> handSizes(PresidentGameState state) {
        return state.getPlayerHands().stream().map(h -> h.getSize()).toList();
    }

    /**
     * Plays a seeded game to targetScore 11 (exchangeCards 1) with random legal actions, checking invariants at every
     * step and the ranking at the end.
     */
    private void playMultiDealGame(int n, long seed) {
        Game game = newGame(n, seed, multiDealParams(11, 1));
        PresidentGameState state = (PresidentGameState) game.getGameState();
        PresidentForwardModel fm = (PresidentForwardModel) game.getForwardModel();
        Random rnd = new Random(seed);
        int deals = 0, steps = 0, president = -1, scum = -1;
        while (state.isNotTerminal() && steps++ < 50000) {
            int p = state.getCurrentPlayer();
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertFalse("no legal actions for player " + p, actions.isEmpty());
            if (state.getGamePhase() == EXCHANGE) {
                assertEquals(president, p);
                assertEquals(scum, state.getScum());
                assertEquals(1, state.getCardsToGive());
                assertEquals(giveCardsFor(state.getPlayerHand(p)), new HashSet<>(actions));
            } else {
                // PLAY: no exchange in progress, and the player to act holds cards
                assertEquals(-1, state.getScum());
                assertTrue(state.getPlayerHand(p).getSize() > 0);
                assertTrue(actions.stream().noneMatch(a -> a instanceof GiveCard));
            }
            AbstractAction action = actions.get(rnd.nextInt(actions.size()));
            int roundBefore = state.getRoundCounter();
            List<Integer> orderBefore = new ArrayList<>(state.getFinishingOrder());
            int[] scoresBefore = state.playerScores.clone();
            boolean wasExchange = state.getGamePhase() == EXCHANGE;

            fm.next(state, action);

            assertAllCardsPresent(state);
            if (state.getRoundCounter() > roundBefore || !state.isNotTerminal()) {
                deals++;
                // the action ended a deal: the actor went out, leaving one player holding cards, the Scum
                assertTrue(action instanceof PlayCards);
                assertFalse(orderBefore.contains(p));
                List<Integer> order = new ArrayList<>(orderBefore);
                order.add(p);
                assertEquals(n - 1, order.size());
                president = order.get(0);
                scum = -1;
                for (int q = 0; q < n; q++)
                    if (!order.contains(q)) scum = q;
                // each deal gives 2 + 1 points; with n >= 4 the last one out scores 0, so this action added nothing
                assertArrayEquals(scoresBefore, state.playerScores);
                assertEquals(3 * deals, Arrays.stream(state.playerScores).sum());
            }
            if (state.isNotTerminal() && state.getRoundCounter() > roundBefore) {
                // a new deal, with the exchange of one card
                assertEquals(0, state.getPlayPile().getSize());
                assertEquals(0, state.getDiscardPile().getSize());
                assertEquals(List.of(), state.getFinishingOrder());
                assertEquals(-1, state.getLastPlayer());
                assertEquals(0, state.getSetSize());
                assertEquals(EXCHANGE, state.getGamePhase());
                assertEquals(president, state.getCurrentPlayer());
                for (int q = 0; q < n; q++) {
                    int expected = dealtTo(q, president, n) + (q == president ? 1 : q == scum ? -1 : 0);
                    assertEquals("hand of " + q + " after the deal", expected, state.getPlayerHand(q).getSize());
                }
            }
            if (wasExchange && state.getGamePhase() != EXCHANGE) {
                assertEquals(PLAY, state.getGamePhase());
                // the exchange is over: the President and the Scum are back to their dealt sizes, and the President leads
                assertEquals(dealtTo(president, president, n), state.getPlayerHand(president).getSize());
                assertEquals(dealtTo(scum, president, n), state.getPlayerHand(scum).getSize());
                assertEquals(president, state.getCurrentPlayer());
                assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof PlayCards));
            }
        }
        assertFalse("game did not end within 50000 actions", state.isNotTerminal());
        assertTrue("more than one deal", deals > 1);
        assertTrue(state.getRoundCounter() < 100);

        int[] scores = state.playerScores;
        assertTrue(Arrays.stream(scores).max().orElseThrow() >= 11);
        // the ranking: by score, ties by the last deal's finishing order
        List<Integer> order = state.getFinishingOrder();
        assertEquals(n, order.size());
        List<Integer> ranking = new ArrayList<>(order);
        ranking.sort(Comparator.comparingInt((Integer q) -> -scores[q]).thenComparingInt(order::indexOf));
        for (int i = 0; i < n; i++) {
            int q = ranking.get(i);
            assertEquals("position of player " + q, i + 1, state.getOrdinalPosition(q));
            assertEquals("result of player " + q, i == 0 ? WIN_GAME : LOSE_GAME, state.getPlayerResults()[q]);
        }
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
    }

    @Test
    public void seededRandomGamesToElevenWithFourPlayers() {
        for (long seed = 31; seed <= 33; seed++)
            playMultiDealGame(4, seed);
    }

    @Test
    public void seededRandomGamesToElevenWithFivePlayers() {
        for (long seed = 41; seed <= 43; seed++)
            playMultiDealGame(5, seed);
    }
}

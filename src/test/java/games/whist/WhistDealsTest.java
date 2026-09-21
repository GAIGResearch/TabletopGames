package games.whist;

import core.CoreConstants.GameResult;
import core.actions.AbstractAction;
import games.tricktaking.PlayCard;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static core.CoreConstants.GameResult.*;
import static core.components.FrenchCard.Suite.Hearts;
import static core.components.FrenchCard.Suite.Spades;
import static games.whist.WhistParameters.TrumpMode.TURN_UP;
import static games.whist.WhistTestUtils.*;
import static org.junit.Assert.*;

/**
 * Games of several deals (WhistParameters.nDeals), turned-up trumps. Each deal is ended by an arranged last trick
 * played through fm.next (WhistTestUtils.playLastTrick). Team 0 is players 0 and 2, team 1 players 1 and 3.
 * The dealer of deal d (d = round counter, from 0) is (d + 3) % 4, and the player on their left leads.
 */
public class WhistDealsTest {

    WhistForwardModel fm = new WhistForwardModel();

    private WhistGameState newGame(int nDeals, long seed) {
        return newState(seed, params(nDeals, TURN_UP, false));
    }

    @Test
    public void afterTheFirstDealANewDealIsDealtByPlayerZeroAndLedByPlayerOne() {
        WhistGameState state = newGame(3, 5);
        state.getKnownVoids().get(2).add(Hearts);          // voids from deal 1, to be forgotten
        state.getKnownVoids().get(1).add(Spades);
        // deal 1: team 0 4 + 4 + 1 (player 0 wins the last trick) = 9 tricks -> 9 - 6 = 3 points
        playLastTrick(state, fm, new int[]{4, 2, 4, 2}, 0);

        assertTrue("game over after one of three deals", state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        assertEquals(0, state.getDealer());                // (1 + 3) % 4
        assertEquals(1, state.getCurrentPlayer());         // on the new dealer's left
        assertEquals(0, state.getCurrentTrick().getSize());
        assertEquals(1, state.getCurrentTrick().getLeader());
        assertEquals(0, state.getDiscardPile().getSize());
        for (int p = 0; p < 4; p++) {
            assertEquals("hand of player " + p, 13, state.getPlayerHand(p).getSize());
            assertEquals("tricks of player " + p, 0, state.getTricksTaken(p));
            assertTrue("voids of player " + p, state.getKnownVoids().get(p).isEmpty());
        }
        assertAllCardsPresent(state);
        // a new card turned up, in the new dealer's hand
        assertNotNull(state.getTrumpCard());
        assertTrue(state.getPlayerHand(0).contains(state.getTrumpCard()));
        assertEquals(state.getTrumpCard().suite, state.getTrumpSuit());
        // points carry over
        assertEquals(3, state.getTeamPoints(0));
        assertEquals(0, state.getTeamPoints(1));

        // player 1 leads: any of their 13 cards
        Set<AbstractAction> expected = state.getPlayerHand(1).getComponents().stream()
                .map(c -> (AbstractAction) new PlayCard(c)).collect(Collectors.toSet());
        assertEquals(expected, new HashSet<>(fm.computeAvailableActions(state)));
    }

    @Test
    public void theThirdDealIsDealtByPlayerOneAndLedByPlayerTwo() {
        WhistGameState state = newGame(3, 6);
        playLastTrick(state, fm, new int[]{4, 2, 4, 2}, 0);
        playLastTrick(state, fm, new int[]{2, 4, 2, 4}, 1);

        assertTrue(state.isNotTerminal());
        assertEquals(2, state.getRoundCounter());
        assertEquals(1, state.getDealer());                // (2 + 3) % 4
        assertEquals(2, state.getCurrentPlayer());
        assertEquals(2, state.getCurrentTrick().getLeader());
        assertTrue(state.getPlayerHand(1).contains(state.getTrumpCard()));
        for (int p = 0; p < 4; p++)
            assertEquals(13, state.getPlayerHand(p).getSize());
    }

    @Test
    public void pointsAccumulateAcrossDealsAndTheGameEndsAfterTheLast() {
        WhistGameState state = newGame(3, 7);

        // deal 1: team 0 takes 4 + 4 + 1 = 9 -> +3 (9 - 6)
        playLastTrick(state, fm, new int[]{4, 2, 4, 2}, 2);
        assertEquals(3, state.getTeamPoints(0));
        assertEquals(0, state.getTeamPoints(1));

        // deal 2: team 1 takes 3 + 4 + 1 = 8 -> +2 (8 - 6); team 0 keeps its 3
        playLastTrick(state, fm, new int[]{2, 3, 3, 4}, 1);
        assertTrue(state.isNotTerminal());
        assertEquals(3, state.getTeamPoints(0));
        assertEquals(2, state.getTeamPoints(1));

        // deal 3: team 0 takes 3 + 3 + 1 = 7 -> +1: totals team 0 3 + 1 = 4, team 1 2
        playLastTrick(state, fm, new int[]{3, 3, 3, 3}, 0);
        assertFalse(state.isNotTerminal());
        // the game ends in the last deal's round: deals 0, 1, 2
        assertEquals(2, state.getRoundCounter());
        assertEquals(4, state.getTeamPoints(0));
        assertEquals(2, state.getTeamPoints(1));
        assertEquals(4.0, state.getGameScore(2), 0.0);
        assertEquals(2.0, state.getGameScore(3), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME, WIN_GAME, LOSE_GAME}, state.getPlayerResults());
    }

    @Test
    public void theGameEndsExactlyAfterTheLastDealWithoutDealingAgain() {
        WhistGameState state = newGame(2, 8);

        // deal 1: team 1 takes 4 + 4 + 1 = 9 -> +3
        playLastTrick(state, fm, new int[]{2, 4, 2, 4}, 3);
        assertTrue("one of two deals played", state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());

        // deal 2: team 0 takes 3 + 3 + 1 = 7 -> +1. Team 0 wins the last deal, but team 1 leads overall, 3 to 1
        playLastTrick(state, fm, new int[]{3, 3, 3, 3}, 2);
        assertFalse(state.isNotTerminal());
        // the game ends in the last deal's round, with no endRound: deals 0 and 1
        assertEquals(1, state.getRoundCounter());
        // no third deal: the last deal's cards are all on the discard pile
        assertEquals(52, state.getDiscardPile().getSize());
        for (int p = 0; p < 4; p++)
            assertEquals(0, state.getPlayerHand(p).getSize());
        assertArrayEquals(new int[]{3, 3, 4, 3}, state.tricksTaken);   // the last deal's tricks
        assertEquals(1, state.getTeamPoints(0));
        assertEquals(3, state.getTeamPoints(1));
        assertArrayEquals(new GameResult[]{LOSE_GAME, WIN_GAME, LOSE_GAME, WIN_GAME}, state.getPlayerResults());
    }

    @Test
    public void equalPointsAfterTheLastDealIsADrawForAll() {
        WhistGameState state = newGame(2, 9);
        // deal 1: team 0 takes 3 + 4 + 1 = 8 -> +2
        playLastTrick(state, fm, new int[]{3, 2, 4, 3}, 0);
        // deal 2: team 1 takes 4 + 3 + 1 = 8 -> +2: 2 each
        playLastTrick(state, fm, new int[]{2, 4, 3, 3}, 3);

        assertFalse(state.isNotTerminal());
        assertEquals(2, state.getTeamPoints(0));
        assertEquals(2, state.getTeamPoints(1));
        assertArrayEquals(new GameResult[]{DRAW_GAME, DRAW_GAME, DRAW_GAME, DRAW_GAME}, state.getPlayerResults());
    }
}

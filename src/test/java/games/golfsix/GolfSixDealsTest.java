package games.golfsix;

import core.CoreConstants.GameResult;
import core.components.FrenchCard;
import games.golfsix.actions.DrawCard;
import games.golfsix.actions.ReplaceCard;
import games.golfsix.actions.TurnUp;
import org.junit.Test;

import java.util.List;

import static core.CoreConstants.GameResult.*;
import static games.golfsix.GolfSixTestUtils.*;
import static org.junit.Assert.*;

/**
 * * nDeals: after a deal is scored the next is dealt by the next player, scores add up over the deals, and the game
 * ends after the last deal with the lowest total winning. nDeals 1 is GolfSixDealEndTest.
 */
public class GolfSixDealsTest {

    final GolfSixForwardModel fm = new GolfSixForwardModel();

    /**
     * 2 players, nDeals 2, past the reveal. Player 0 (to play) has only position 4 (the 9H) face-down and the 3S on
     * top of the draw deck: replacing it ends the first deal with player 0 on 3 and player 1 on 25.
     */
    private GolfSixGameState firstDealOneCardFromTheEnd(long seed) {
        GolfSixParameters params = variant(false, 2);
        params.setRandomSeed(seed);
        GolfSixGameState state = newState(params, 2);
        // 5/5 pair + K/3 + 2/2 pair = 0 + (0 + 3) + 0 = 3 once the 3S replaces the 9H
        setGrid(state, 0, "5H", "KS", "2C", "5D", "9H", "2D");
        // A/4 + J/Q + 10/10 pair = (1 + 4) + (10 + 10) + 0 = 25
        setGrid(state, 1, "AH", "JC", "10S", "4D", "QC", "10H");
        skipReveal(state);
        faceUp(state, 0, 2, 3, 5);
        stackDrawDeck(state, "3S");
        return state;
    }

    private void endFirstDeal(GolfSixGameState state) {
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));
    }

    @Test
    public void afterTheFirstDealTheNextIsDealtAndRevealedStartingLeftOfTheNewDealer() {
        GolfSixGameState state = firstDealOneCardFromTheEnd(61);
        endFirstDeal(state);

        assertTrue("one deal still to play", state.isNotTerminal());
        assertEquals(1, state.getRoundCounter());
        // dealer = (roundCounter + n - 1) % n = (1 + 2 - 1) % 2 = 0; player 1 is on the dealer's left
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals("the first deal's points are kept", 3, state.getScore(0));
        assertEquals(25, state.getScore(1));
        assertEquals("DDDDDD", faceUpPattern(state, 0));
        assertEquals("DDDDDD", faceUpPattern(state, 1));
        assertEquals(1, state.getDiscardPile().getSize());
        assertNull(state.getDrawnCard());
        assertFalse(state.isDrawnFromDiscard());
        assertEquals(-1, state.getFinisher());
        assertEquals(52 - 2 * 6 - 1, state.getDrawDeck().getSize());
        assertEquals("the safeguard's turn count restarts", 0, state.getTurnCounter());
        assertEquals(allTurnUps(), actions(fm, state));
        assertAllCardsPresent(state);
    }

    @Test
    public void theCardsAreGatheredAndReshuffledForTheNextDeal() {
        GolfSixGameState state = firstDealOneCardFromTheEnd(62);
        // a faithful copy holds every card in the same place, but has its own random number generator
        GolfSixGameState other = (GolfSixGameState) state.copy();
        assertEquals(state, other);
        endFirstDeal(state);
        endFirstDeal(other);

        // the grids at the end of the first deal were these; a fresh shuffle does not deal them again
        assertNotEquals(cards("5H", "KS", "2C", "5D", "3S", "2D"), state.getGrid(0).getComponents());
        assertNotEquals(cards("AH", "JC", "10S", "4D", "QC", "10H"), state.getGrid(1).getComponents());
        assertNotEquals(cards("5H", "KS", "2C", "5D", "3S", "2D"), state.getGrid(1).getComponents());
        assertNotEquals(cards("AH", "JC", "10S", "4D", "QC", "10H"), state.getGrid(0).getComponents());
        // the same cards gathered in the same order are dealt differently, so they were shuffled
        assertNotEquals(state.getGrid(0).getComponents(), other.getGrid(0).getComponents());
        assertAllCardsPresent(state);
    }

    @Test
    public void afterTheLastDealTheLowestTotalWins() {
        GolfSixGameState state = firstDealOneCardFromTheEnd(63);
        endFirstDeal(state);
        assertEquals(1, state.getCurrentPlayer());

        // second deal, past the reveal: player 1 (to play) has only position 4 face-down and draws the 3C
        // player 1: 5/5 pair + K/3 + 2/2 pair = 0 + (0 + 3) + 0 = 3
        setGrid(state, 1, "5C", "KD", "2H", "5S", "9C", "2S");
        // player 0: 7/8 + K/4 + 6/6 pair = (7 + 8) + (0 + 4) + 0 = 19
        setGrid(state, 0, "7C", "KH", "6C", "8S", "4C", "6S");
        skipReveal(state);
        faceUp(state, 1, 2, 3, 5);
        stackDrawDeck(state, "3C");
        fm.next(state, new DrawCard(false));
        fm.next(state, new ReplaceCard(4));

        assertEquals(GAME_END, state.getGameStatus());
        // totals: player 0 3 + 19 = 22, player 1 25 + 3 = 28; player 1 won the second deal but player 0 the game
        assertEquals(22, state.getScore(0));
        assertEquals(28, state.getScore(1));
        assertEquals(-22, state.getGameScore(0), 0.0);
        assertArrayEquals(new GameResult[]{WIN_GAME, LOSE_GAME}, state.getPlayerResults());
        assertEquals("UUUUUU", faceUpPattern(state, 0));
        assertAllCardsPresent(state);
    }

    @Test
    public void theDealPassesRoundTheTableAndTheSafeguardCountRestartsEachDeal() {
        // 3 players, 3 deals, maxTurnsPerPlayer 1: each deal is scored after 3 turns, i.e. after the reveal
        GolfSixParameters params = variant(false, 3);
        params.setParameterValue("maxTurnsPerPlayer", 1);
        params.setRandomSeed(64);
        GolfSixGameState state = newState(params, 3);
        int[] totals = new int[3];

        // deal 1 dealt by player 2, revealed by 0, 1, 2; deal 2 by 0, revealed by 1, 2, 0; deal 3 by 1, from 2
        int[][] revealOrder = {{0, 1, 2}, {1, 2, 0}, {2, 0, 1}};
        for (int deal = 0; deal < 3; deal++) {
            assertEquals(deal, state.getRoundCounter());
            assertEquals((deal + 2) % 3, state.getDealer());
            for (int i = 0; i < 3; i++) {
                assertTrue("deal " + deal + " still going", state.isNotTerminal());
                assertEquals(revealOrder[deal][i], state.getCurrentPlayer());
                fm.next(state, new TurnUp(0));
                List<List<FrenchCard>> grids = gridSnapshot(state);
                fm.next(state, new TurnUp(1));
                if (i == 2)  // the last TurnUp scores the deal; it moves no card
                    for (int p = 0; p < 3; p++) totals[p] += oracleGridScore(grids.get(p));
            }
            for (int p = 0; p < 3; p++)
                assertEquals("total of player " + p + " after deal " + deal, totals[p], state.getScore(p));
        }
        assertEquals(GAME_END, state.getGameStatus());
    }
}

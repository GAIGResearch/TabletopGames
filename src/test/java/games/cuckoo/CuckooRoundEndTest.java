package games.cuckoo;

import core.components.FrenchCard;
import games.cuckoo.actions.KeepCard;
import games.cuckoo.actions.SwapCard;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static games.cuckoo.CuckooTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of a round after the dealer's decision: who loses a life, going out, and the next deal. Each test arranges
 * the cards, gives the turn to the dealer (player 3) and lets them decide.
 */
public class CuckooRoundEndTest {

    CuckooGameState state;
    CuckooForwardModel fm;

    @Before
    public void setup() {
        // 4 players, 3 lives each
        state = newState(4, 3, 42);
        fm = new CuckooForwardModel();
    }

    /**
     * Deal these cards (null for a player who is out), then player 3, the dealer, keeps: the round ends.
     */
    private void dealerKeepsWith(FrenchCard... cards) {
        dealCards(state, cards);
        setDealerAndTurn(state, 3, 3);
        fm.next(state, new KeepCard());
    }

    @Test
    public void theLowestCardLosesALife() {
        dealerKeepsWith(card("5H"), card("2S"), card("KD"), card("9C"));
        // 2 < 5 < 9 < K: player 1 loses a life
        assertArrayEquals(new int[]{3, 2, 3, 3}, state.lives);
        assertArrayEquals("nobody is out", new int[]{-1, -1, -1, -1}, state.roundEliminated);
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void everyPlayerTiedForLowestLosesALifeWhateverTheSuits() {
        dealerKeepsWith(card("2H"), card("2S"), card("KD"), card("9C"));
        // 2H and 2S tie for lowest: suits do not matter, both lose one
        assertArrayEquals(new int[]{2, 2, 3, 3}, state.lives);
    }

    @Test
    public void anAceIsTheLowestCard() {
        // FrenchCard numbers an Ace 14, but in Cuckoo it ranks 1, below the 2
        dealerKeepsWith(card("AS"), card("2S"), card("KD"), card("QC"));
        assertArrayEquals(new int[]{2, 3, 3, 3}, state.lives);
    }

    @Test
    public void pictureCardsRankTenJackQueenKing() {
        // J (11) is lowest of K, Q, J, K
        dealerKeepsWith(card("KS"), card("QH"), card("JD"), card("KH"));
        assertArrayEquals(new int[]{3, 3, 2, 3}, state.lives);
        // 10 is lowest of 10, J, Q, K
        dealerKeepsWith(card("10C"), card("JD"), card("QH"), card("KS"));
        assertArrayEquals(new int[]{2, 3, 2, 3}, state.lives);
    }

    @Test
    public void theDealersCutCardCountsAtTheEndOfTheRound() {
        dealCards(state, card("5H"), card("6D"), card("7C"), card("2S"));
        putOnTopOfDrawDeck(state, card("9H"));
        setDealerAndTurn(state, 3, 3);
        fm.next(state, new SwapCard());
        // the dealer now holds 9H: 5H is the lowest of 5, 6, 7, 9, so player 0 loses a life
        assertArrayEquals(new int[]{2, 3, 3, 3}, state.lives);
    }

    @Test
    public void aCutKingLeavesTheDealerWithTheirOwnCard() {
        dealCards(state, card("5H"), card("6D"), card("7C"), card("2S"));
        putOnTopOfDrawDeck(state, card("KH"));
        setDealerAndTurn(state, 3, 3);
        fm.next(state, new SwapCard());
        // the dealer keeps the 2S, the lowest, and loses a life
        assertArrayEquals(new int[]{3, 3, 3, 2}, state.lives);
    }

    @Test
    public void playersOutOfTheGameLoseNoMoreLives() {
        advanceRoundCounter(state, fm, 1);
        knockOut(state, 2, 0);
        dealerKeepsWith(card("5H"), card("7D"), null, card("9C"));
        // player 2 holds nothing and is not the lowest; 5H is lowest of those in the game
        assertArrayEquals(new int[]{2, 3, 0, 3}, state.lives);
        assertEquals("round player 2 went out is unchanged", 0, state.roundEliminated[2]);
    }

    @Test
    public void aPlayerLosingTheirLastLifeIsOutOfTheGame() {
        state.lives[1] = 1;
        dealerKeepsWith(card("5H"), card("2S"), card("KD"), card("9C"));
        assertArrayEquals(new int[]{3, 0, 3, 3}, state.lives);
        // it happened in round 0, the round just played
        assertArrayEquals(new int[]{-1, 0, -1, -1}, state.roundEliminated);
        assertEquals("the round counter moves on", 1, state.getRoundCounter());
        // the new deal gives player 1 nothing, and three cards to the others
        assertNull(state.getPlayerCard(1));
        assertOneCardPerPlayerInGame(state);
        assertAllCardsPresent(state);
        assertTrue("three players are left", state.isNotTerminal());
    }

    @Test
    public void theDealPassesLeftAndThePlayerAfterTheNewDealerDecidesFirst() {
        dealerKeepsWith(card("5H"), card("2S"), card("KD"), card("9C"));
        // old dealer 3: the new dealer is player 0 and player 1 decides first
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getRoundCounter());
        assertOneCardPerPlayerInGame(state);
    }

    @Test
    public void theDealPassesOverPlayersOutOfTheGame() {
        knockOut(state, 0, 0);
        dealerKeepsWith(null, card("5H"), card("2S"), card("9C"));
        // old dealer 3; player 0 is out, so player 1 deals and player 2 decides first
        assertEquals(1, state.getDealer());
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void theFirstPlayerSkipsPlayersOutOfTheGame() {
        knockOut(state, 1, 0);
        dealerKeepsWith(card("5H"), null, card("2S"), card("9C"));
        // old dealer 3: player 0 deals; player 1 is out, so player 2 decides first
        assertEquals(0, state.getDealer());
        assertEquals(2, state.getCurrentPlayer());
    }

    @Test
    public void theDealPassesToTheOldDealersLeftWhenTheDealerGoesOut() {
        state.lives[3] = 1;
        dealerKeepsWith(card("5H"), card("7D"), card("9C"), card("2S"));
        // the dealer, player 3, goes out with the 2S; the deal still passes to their left: player 0, then player 1 decides
        assertArrayEquals(new int[]{3, 3, 3, 0}, state.lives);
        assertEquals(0, state.getDealer());
        assertEquals(1, state.getCurrentPlayer());
        assertNull(state.getPlayerCard(3));
    }

    @Test
    public void theNextDealGathersAndReshufflesAllTheCards() {
        dealCards(state, card("5H"), card("7D"), card("9C"), card("JS"));
        setDealerAndTurn(state, 3, 3);
        List<FrenchCard> drawBefore = new ArrayList<>(state.drawDeck.getComponents());
        fm.next(state, new KeepCard());
        assertAllCardsPresent(state);
        assertOneCardPerPlayerInGame(state);
        // gathering the four cards without shuffling would deal them straight back and leave the draw deck as it was
        assertNotEquals(drawBefore, state.drawDeck.getComponents());
    }
}

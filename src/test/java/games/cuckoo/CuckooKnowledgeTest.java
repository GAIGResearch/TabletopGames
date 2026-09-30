package games.cuckoo;

import games.cuckoo.actions.KeepCard;
import games.cuckoo.actions.SwapCard;
import org.junit.Before;
import org.junit.Test;

import static games.cuckoo.CuckooTestUtils.*;
import static org.junit.Assert.*;

/**
 * What each player knows about the other players' cards, driven through the real actions: a swap, a refused swap, a
 * chain of swaps, keeping, the dealer's cut and refused cut, and the reset at a new deal.
 * Knowledge is written as one string per observer, one character per holder ('1' = the observer knows that card).
 */
public class CuckooKnowledgeTest {

    CuckooGameState state;
    CuckooForwardModel fm;

    @Before
    public void setup() {
        // 4 players, 3 lives each; player 3 deals, player 0 decides first
        state = newState(4, 3, 42);
        fm = new CuckooForwardModel();
    }

    @Test
    public void atTheDealEachPlayerKnowsOnlyTheirOwnCard() {
        assertKnowledge(state, "1000", "0100", "0010", "0001");
    }

    @Test
    public void afterASwapTheTwoPlayersKnowBothCards() {
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());
        assertEquals(card("9C"), state.getPlayerCard(0));
        assertEquals(card("5H"), state.getPlayerCard(1));
        // P0 knows P1 has the 5H it gave away; P1 knows P0 has the 9C it gave away. P2 and P3 saw nothing.
        assertKnowledge(state, "1100", "1100", "0010", "0001");
    }

    @Test
    public void aRefusedSwapShowsTheNeighboursKingToEveryone() {
        dealCards(state, card("5H"), card("KS"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());
        assertEquals("no card moves", card("5H"), state.getPlayerCard(0));
        assertEquals(card("KS"), state.getPlayerCard(1));
        // column 1 becomes all known; P1 learns nothing of P0's card, and P0 nothing else
        assertKnowledge(state, "1100", "0100", "0110", "0101");
    }

    @Test
    public void afterAChainOfSwapsAPlayerKnowsWhereTheirOldCardWent() {
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());
        // P0 9C, P1 5H; knowledge 1100 1100 0010 0001 (as in afterASwapTheTwoPlayersKnowBothCards)
        fm.next(state, new SwapCard());
        // P1 gives the 5H to P2 for the 3S: P0 9C, P1 3S, P2 5H
        assertEquals(card("9C"), state.getPlayerCard(0));
        assertEquals(card("3S"), state.getPlayerCard(1));
        assertEquals(card("5H"), state.getPlayerCard(2));
        // Columns 1 and 2 swap for every observer (the cards moved):
        //   P0 1100 -> 1010: P0 still knows its old 5H, now with P2, and no longer knows P1's card (3S)
        //   P1 1100 -> 1010, then P1 knows its own and P2's card -> 1110 (it still knows P0 holds its old 9C)
        //   P2 0010 -> 0100, then P2 knows its own and P1's card -> 0110
        //   P3 0001 unchanged
        assertKnowledge(state, "1010", "1110", "0110", "0001");
    }

    @Test
    public void aKingShownToEveryoneIsStillKnownAfterItIsSwappedOn() {
        dealCards(state, card("6H"), card("KS"), card("3D"), card("9C"));
        fm.next(state, new SwapCard());
        // P0's swap refused: everyone knows P1's KS -> 1100 0100 0110 0101
        fm.next(state, new SwapCard());
        // P1 gives the KS to P2 for the 3D. Columns 1 and 2 swap: 1010 0010 0110 0011;
        // then P1 knows P2's card (0110) and P2 knows P1's (0110)
        assertEquals(card("3D"), state.getPlayerCard(1));
        assertEquals(card("KS"), state.getPlayerCard(2));
        assertKnowledge(state, "1010", "0110", "0110", "0011");
    }

    @Test
    public void keepingChangesNoKnowledge() {
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());
        fm.next(state, new KeepCard());   // P1 keeps the 5H
        assertEquals(2, state.getCurrentPlayer());
        assertKnowledge(state, "1100", "1100", "0010", "0001");
    }

    @Test
    public void aSwapPastAPlayerOutOfTheGameChangesTheKnowledgeOfTheNeighbourInTheGame() {
        state = newState(5, 3, 42);
        knockOut(state, 1, 0);
        dealCards(state, card("5H"), null, card("9C"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());
        // P0's neighbour is P2 (P1 is out): P0 and P2 know each other's cards
        assertEquals(card("5H"), state.getPlayerCard(2));
        assertKnowledge(state, "10100", "01000", "10100", "00010", "00001");
    }

    @Test
    public void theDealersCutLeavesOnlyTheDealerKnowingTheirCard() {
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        putOnTopOfDrawDeck(state, card("2C"));
        setDealerAndTurn(state, 3, 2);
        fm.next(state, new SwapCard());
        // P2 and dealer P3 exchange: P2 8D, P3 3S, and both know both -> 1000 0100 0011 0011
        assertEquals(3, state.getCurrentPlayer());
        assertKnowledge(state, "1000", "0100", "0011", "0011");
        // the dealer's cut (via execute: fm.next would end the round and deal again)
        new SwapCard().execute(state);
        assertEquals(card("2C"), state.getPlayerCard(3));
        // P2 knew the dealer's 3S, which is now in the draw deck: column 3 is known only by the dealer.
        // The dealer still knows P2's card (the 8D it gave P2).
        assertKnowledge(state, "1000", "0100", "0010", "0011");
    }

    @Test
    public void aRefusedCutChangesNoKnowledge() {
        dealCards(state, card("5H"), card("9C"), card("3S"), card("8D"));
        putOnTopOfDrawDeck(state, card("KC"));
        setDealerAndTurn(state, 3, 2);
        fm.next(state, new SwapCard());
        new SwapCard().execute(state);
        // the cut King stays in the draw deck; the dealer keeps the 3S that P2 gave them, and P2 still knows it
        assertEquals(card("3S"), state.getPlayerCard(3));
        assertKnowledge(state, "1000", "0100", "0011", "0011");
    }

    @Test
    public void aNewDealResetsKnowledgeToOwnCardsOnly() {
        dealCards(state, card("5H"), card("KS"), card("3S"), card("8D"));
        fm.next(state, new SwapCard());   // refused: everyone knows P1's King
        fm.next(state, new KeepCard());
        fm.next(state, new SwapCard());   // P2 and P3 exchange
        assertKnowledge(state, "1100", "0100", "0111", "0111");
        fm.next(state, new KeepCard());   // the dealer keeps: the round ends and a new round is dealt
        assertEquals(1, state.getRoundCounter());
        assertKnowledge(state, "1000", "0100", "0010", "0001");
    }
}

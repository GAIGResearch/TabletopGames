package games.goofspiel;

import core.components.FrenchCard;
import games.goofspiel.actions.Bid;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

import static games.goofspiel.GoofspielTestUtils.*;
import static org.junit.Assert.*;

/**
 * Copies: a full copy is faithful and independent; a copy for a player hides the prize deck order and the
 * other players' face-down bids (returned to their hands, so they are still to bid), and keeps the player's own.
 */
public class GoofspielCopyTest {

    GoofspielForwardModel fm;

    @Before
    public void setup() {
        fm = new GoofspielForwardModel();
    }

    @Test
    public void fullCopyIsEqualAndIndependent() {
        GoofspielGameState state = newState(2, 9);
        playRound(fm, state, "9C", "4S");
        fm.next(state, new Bid(0, card("KC")));

        GoofspielGameState copy = (GoofspielGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());

        // player 1 bids in the copy: the round resolves there only
        fm.next(copy, new Bid(1, card("QS")));
        assertNotEquals(state, copy);
        assertEquals(11, state.getHand(0).getSize());
        assertEquals(List.of(card("KC")), state.getBid(0).getComponents());
        assertEquals(12, state.getHand(1).getSize());
        assertEquals(0, state.getBid(1).getSize());
        assertEquals(1, state.getPlayedBids(0).getSize());
        assertEquals(1, state.getRoundCounter());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(2, copy.getPlayedBids(0).getSize());
        assertAllCardsPresent(state);
    }

    @Test
    public void copyForTheWaitingPlayerReturnsTheOtherBidToItsHand() {
        GoofspielGameState state = newState(2, 9);
        fm.next(state, new Bid(0, card("9C")));

        GoofspielGameState seenBy1 = (GoofspielGameState) state.copy(1);
        // player 0's face-down bid is back in their hand: they are to bid again
        assertEquals(13, seenBy1.getHand(0).getSize());
        assertTrue(seenBy1.getHand(0).contains(card("9C")));
        assertEquals(0, seenBy1.getBid(0).getSize());
        assertEquals(List.of(0, 1), seenBy1.getCurrentSimultaneousPlayers());
        // player 1, still to bid, holds the turn
        assertEquals(1, seenBy1.getCurrentPlayer());
        assertEquals(13, fm.computeAvailableActions(seenBy1).size());
        // public information unchanged
        assertEquals(state.getPrizesOnOffer().getComponents(), seenBy1.getPrizesOnOffer().getComponents());
        assertEquals(new HashSet<>(state.getPrizeDeck().getComponents()), new HashSet<>(seenBy1.getPrizeDeck().getComponents()));
        assertAllCardsPresent(seenBy1);

        // player 0 sees their own bid
        GoofspielGameState seenBy0 = (GoofspielGameState) state.copy(0);
        assertEquals(List.of(card("9C")), seenBy0.getBid(0).getComponents());
        assertEquals(12, seenBy0.getHand(0).getSize());
        assertEquals(1, seenBy0.getCurrentPlayer());

        // the master state is untouched
        assertEquals(List.of(card("9C")), state.getBid(0).getComponents());
        assertEquals(12, state.getHand(0).getSize());
        assertEquals(1, state.getCurrentPlayer());
    }

    @Test
    public void copyForAPlayerWhoHasBidKeepsTheirBidAndHandsTheTurnToTheFirstStillToBid() {
        GoofspielGameState state = newState(3, 9);
        fm.next(state, new Bid(0, card("9C")));
        fm.next(state, new Bid(1, card("5S")));
        assertEquals(2, state.getCurrentPlayer());

        GoofspielGameState seenBy1 = (GoofspielGameState) state.copy(1);
        assertEquals(List.of(card("5S")), seenBy1.getBid(1).getComponents());
        assertEquals(12, seenBy1.getHand(1).getSize());
        assertEquals(0, seenBy1.getBid(0).getSize());
        assertTrue(seenBy1.getHand(0).contains(card("9C")));
        assertEquals(List.of(0, 2), seenBy1.getCurrentSimultaneousPlayers());
        // player 1 has bid, so the turn goes to the first player still to bid
        assertEquals(0, seenBy1.getCurrentPlayer());
        assertAllCardsPresent(seenBy1);

        // player 2, still to bid, sees neither bid and holds the turn
        GoofspielGameState seenBy2 = (GoofspielGameState) state.copy(2);
        assertEquals(List.of(0, 1, 2), seenBy2.getCurrentSimultaneousPlayers());
        assertEquals(2, seenBy2.getCurrentPlayer());
        assertEquals(13, seenBy2.getHand(1).getSize());
    }

    @Test
    public void aReturnedBidGoesBackToItsPlaceInTheHand() {
        // on top of the hand it would show the observer which card was bid
        GoofspielGameState state = newState(2, 9);
        List<FrenchCard> handBefore = List.copyOf(state.getHand(1).getComponents());
        fm.next(state, new Bid(1, card("7S")));
        GoofspielGameState seenBy0 = (GoofspielGameState) state.copy(0);
        assertEquals(handBefore, seenBy0.getHand(1).getComponents());
    }

    @Test
    public void inACopyTheTurnSkipsTheObserverWhoHasAlreadyBid() {
        // players 0 and 1 have bid; in player 1's copy player 0 bids again, and the turn must skip player 1
        GoofspielGameState state = newState(3, 9);
        fm.next(state, new Bid(0, card("9C")));
        fm.next(state, new Bid(1, card("5S")));
        GoofspielGameState seenBy1 = (GoofspielGameState) state.copy(1);
        assertEquals(0, seenBy1.getCurrentPlayer());

        fm.next(seenBy1, new Bid(0, card("2C")));
        assertEquals(2, seenBy1.getCurrentPlayer());
        assertEquals(List.of(2), seenBy1.getCurrentSimultaneousPlayers());
    }

    @Test
    public void copyForAPlayerStillToBidTakesTheTurnFromAnotherWaitingPlayer() {
        GoofspielGameState state = newState(3, 9);
        fm.next(state, new Bid(0, card("9C")));
        assertEquals(1, state.getCurrentPlayer());
        // player 2 is still to bid but does not hold the turn in the master state
        GoofspielGameState seenBy2 = (GoofspielGameState) state.copy(2);
        assertEquals(2, seenBy2.getCurrentPlayer());
        assertEquals(List.of(0, 1, 2), seenBy2.getCurrentSimultaneousPlayers());
    }

    @Test
    public void copyForAPlayerShufflesThePrizeDeck() {
        GoofspielGameState state = newState(2, 9);
        fm.next(state, new Bid(0, card("9C")));
        // two copies differ only in the random numbers drawn for them: the 12 prize cards must differ in order
        List<FrenchCard> a = ((GoofspielGameState) state.copy(1)).getPrizeDeck().getComponents();
        List<FrenchCard> b = ((GoofspielGameState) state.copy(1)).getPrizeDeck().getComponents();
        assertEquals(new HashSet<>(a), new HashSet<>(b));
        assertNotEquals(a, b);
    }

    @Test
    public void withoutPartialObservabilityNothingIsHidden() {
        GoofspielGameState state = newState(2, 9);
        state.getCoreGameParameters().partialObservable = false;
        fm.next(state, new Bid(0, card("9C")));
        GoofspielGameState seenBy1 = (GoofspielGameState) state.copy(1);
        assertEquals(List.of(card("9C")), seenBy1.getBid(0).getComponents());
        assertEquals(state.getPrizeDeck().getComponents(), seenBy1.getPrizeDeck().getComponents());
        assertEquals(state, seenBy1);
    }
}

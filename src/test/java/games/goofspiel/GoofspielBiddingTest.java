package games.goofspiel;

import core.actions.AbstractAction;
import games.goofspiel.actions.Bid;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static games.goofspiel.GoofspielTestUtils.*;
import static org.junit.Assert.*;

/**
 * Bids: which bids a player may make, what a single bid does, and how the turn passes among the players
 * still to bid before the round is resolved.
 */
public class GoofspielBiddingTest {

    GoofspielGameState state;
    GoofspielForwardModel fm;

    @Before
    public void setup() {
        state = newState(2, 11);
        fm = new GoofspielForwardModel();
    }

    private static Set<AbstractAction> bidsFor(int player, char suitLetter, List<String> ranks) {
        return ranks.stream().map(r -> (AbstractAction) new Bid(player, card(r + suitLetter))).collect(Collectors.toSet());
    }

    @Test
    public void everyPlayerMayBidAnyCardOfTheirHandAtTheStart() {
        // one Bid per card in hand (13), carrying the bidding player
        assertEquals(bidsFor(0, 'C', RANKS), new HashSet<>(fm.computeAvailableActions(state)));
        assertEquals(13, fm.computeAvailableActions(state).size());
        assertEquals(bidsFor(1, 'S', RANKS), new HashSet<>(fm.computeAvailableActions(state, null, 1)));
        assertEquals(List.of(0, 1), state.getCurrentSimultaneousPlayers());
    }

    @Test
    public void aBidPutsTheCardFaceDownAndPassesTheTurn() {
        fm.next(state, new Bid(0, card("9C")));

        // the card moved from hand to bid; nothing is revealed or resolved yet
        assertEquals(12, state.getHand(0).getSize());
        assertFalse(state.getHand(0).contains(card("9C")));
        assertEquals(List.of(card("9C")), state.getBid(0).getComponents());
        assertEquals(0, state.getPlayedBids(0).getSize());
        assertEquals(1, state.getPrizesOnOffer().getSize());
        assertEquals(12, state.getPrizeDeck().getSize());
        assertEquals(0, state.getRoundCounter());

        // player 0 has no more decisions this round; player 1 still has all 13
        assertTrue(fm.computeAvailableActions(state, null, 0).isEmpty());
        assertEquals(List.of(1), state.getCurrentSimultaneousPlayers());
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(bidsFor(1, 'S', RANKS), new HashSet<>(fm.computeAvailableActions(state)));
        assertAllCardsPresent(state);
    }

    @Test
    public void cardsAlreadyBidAreNoLongerOffered() {
        arrangePrizes(state, "7D");
        playRound(fm, state, "9C", "4S");
        // 12 cards left each: the 9 of Clubs and the 4 of Spades are gone
        assertEquals(bidsFor(0, 'C', List.of("A", "2", "3", "4", "5", "6", "7", "8", "10", "J", "Q", "K")),
                new HashSet<>(fm.computeAvailableActions(state, null, 0)));
        assertEquals(bidsFor(1, 'S', List.of("A", "2", "3", "5", "6", "7", "8", "9", "10", "J", "Q", "K")),
                new HashSet<>(fm.computeAvailableActions(state, null, 1)));
    }

    @Test
    public void turnCyclesFromTheCurrentPlayerToTheNextStillToBid() {
        GoofspielGameState three = newState(3, 11);
        // player 2 holds the turn (as after a redeterminisation); the turn then cycles 2 -> 0 -> 1
        three.setTurnOwner(2);
        fm.next(three, new Bid(2, card("5H")));
        assertEquals(0, three.getCurrentPlayer());
        assertEquals(List.of(0, 1), three.getCurrentSimultaneousPlayers());
        fm.next(three, new Bid(0, card("5C")));
        // player 2 has bid already, so the turn skips to player 1
        assertEquals(1, three.getCurrentPlayer());
        assertEquals(List.of(1), three.getCurrentSimultaneousPlayers());
        fm.next(three, new Bid(1, card("3S")));
        // round resolved: everyone bids again, starting with player 0
        assertEquals(1, three.getRoundCounter());
        assertEquals(0, three.getCurrentPlayer());
        assertEquals(List.of(0, 1, 2), three.getCurrentSimultaneousPlayers());
    }

    @Test
    public void turnPassesInSeatOrderWithThreePlayers() {
        GoofspielGameState three = newState(3, 11);
        fm.next(three, new Bid(0, card("5C")));
        assertEquals(1, three.getCurrentPlayer());
        assertEquals(List.of(1, 2), three.getCurrentSimultaneousPlayers());
        fm.next(three, new Bid(1, card("5S")));
        assertEquals(2, three.getCurrentPlayer());
        assertEquals(List.of(2), three.getCurrentSimultaneousPlayers());
        assertTrue(fm.computeAvailableActions(three, null, 1).isEmpty());
        assertEquals(bidsFor(2, 'H', RANKS), new HashSet<>(fm.computeAvailableActions(three)));
    }
}

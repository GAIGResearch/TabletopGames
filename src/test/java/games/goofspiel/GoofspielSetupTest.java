package games.goofspiel;

import core.components.FrenchCard;
import org.junit.Test;

import java.util.List;

import static core.components.FrenchCard.Suite.*;
import static games.goofspiel.GoofspielTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal: one suit per player, the Diamonds shuffled into the prize deck with one turned up.
 */
public class GoofspielSetupTest {

    @Test
    public void eachPlayerHoldsTheirWholeSuitAndOneDiamondIsOnOffer() {
        GoofspielGameState state = newState(2, 3);
        assertEquals(multiset(fullSuit(Clubs)), multiset(state.getHand(0)));
        assertEquals(multiset(fullSuit(Spades)), multiset(state.getHand(1)));
        // values 1..13 in each hand
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13), values(state.getHand(0).getComponents()));
        // 13 Diamonds: 12 face down, 1 on offer
        assertEquals(12, state.getPrizeDeck().getSize());
        assertEquals(1, state.getPrizesOnOffer().getSize());
        assertEquals(Diamonds, state.getPrizesOnOffer().get(0).suite);
        for (int p = 0; p < 2; p++) {
            assertEquals(0, state.getBid(p).getSize());
            assertEquals(0, state.getPlayedBids(p).getSize());
            assertEquals(0, state.getWonPrizes(p).getSize());
        }
        assertEquals(0, state.getDiscardedPrizes().getSize());
        assertAllCardsPresent(state);
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(List.of(0, 1), state.getCurrentSimultaneousPlayers());
    }

    @Test
    public void suitsFollowClubsSpadesHeartsAndRepeatForAFourthPlayer() {
        GoofspielGameState three = newState(3, 3);
        assertEquals(multiset(fullSuit(Clubs)), multiset(three.getHand(0)));
        assertEquals(multiset(fullSuit(Spades)), multiset(three.getHand(1)));
        assertEquals(multiset(fullSuit(Hearts)), multiset(three.getHand(2)));
        assertEquals(List.of(0, 1, 2), three.getCurrentSimultaneousPlayers());

        GoofspielGameState four = newState(4, 3);
        assertEquals(multiset(fullSuit(Clubs)), multiset(four.getHand(3)));
        assertAllCardsPresent(four);
    }

    @Test
    public void prizeOrderDependsOnTheSeed() {
        // same everything but the seed: the prize deck must come out in a different order
        List<FrenchCard> a = newState(2, 1).getPrizeDeck().getComponents();
        List<FrenchCard> b = newState(2, 2).getPrizeDeck().getComponents();
        assertNotEquals(a, b);
    }
}

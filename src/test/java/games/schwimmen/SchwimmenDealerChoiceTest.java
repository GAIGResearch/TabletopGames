package games.schwimmen;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.components.FrenchCard;
import core.components.PartialObservableDeck;
import games.schwimmen.actions.ChooseHand;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.schwimmen.SchwimmenTestUtils.*;
import static org.junit.Assert.*;

/**
 * The deal as set up, and the dealer's choice between their own hand and the extra hand.
 */
public class SchwimmenDealerChoiceTest {

    SchwimmenForwardModel fm;
    SchwimmenGameState state;

    @Before
    public void setup() {
        fm = new SchwimmenForwardModel();
        state = newState(4, ordinarySeed(4, 0), fm);
    }

    @Test
    public void theDealGivesThreeCardsEachAndAHiddenExtraHandWithTheDealerToChoose() {
        for (int n : new int[]{2, 5, 8}) {
            SchwimmenGameState s = newState(n, ordinarySeed(n, 0), fm);
            for (int p = 0; p < n; p++) {
                PartialObservableDeck<FrenchCard> hand = s.getPlayerHand(p);
                assertEquals(3, hand.getSize());
                for (int i = 0; i < 3; i++)
                    assertTrue("dealt cards are seen only by their owner", visibleOnlyToOwner(hand, i, n));
            }
            assertEquals(3, s.getExtraHand().getSize());
            assertEquals(CoreConstants.VisibilityMode.HIDDEN_TO_ALL, s.getExtraHand().getVisibilityMode());
            assertEquals(0, s.getTable().getSize());
            assertEquals(0, s.getDiscardPile().getSize());
            // 32 cards less 3 per player and 3 for the extra hand
            assertEquals(32 - 3 * n - 3, s.getDrawDeck().getSize());
            assertEquals(n - 1, s.getDealer());
            assertEquals(n - 1, s.getCurrentPlayer());
            assertTrue(s.isDealerChoicePending());
            assertAllCardsPresent(s);
        }
    }

    @Test
    public void onlyTheTwoChoicesAreOfferedToTheDealer() {
        assertEquals(Set.of(new ChooseHand(false), new ChooseHand(true)),
                new HashSet<>(fm.computeAvailableActions(state)));
        assertEquals(2, fm.computeAvailableActions(state).size());
    }

    @Test
    public void keepingTheHandPutsTheExtraHandFaceUpOnTheTable() {
        List<FrenchCard> dealerHand = new ArrayList<>(state.getPlayerHand(3).getComponents());
        Set<FrenchCard> extra = new HashSet<>(state.getExtraHand().getComponents());
        int drawSize = state.getDrawDeck().getSize();

        fm.next(state, new ChooseHand(false));

        assertEquals(dealerHand, state.getPlayerHand(3).getComponents());
        for (int i = 0; i < 3; i++)
            assertTrue("the kept hand is still seen only by the dealer", visibleOnlyToOwner(state.getPlayerHand(3), i, 4));
        assertEquals(extra, new HashSet<>(state.getTable().getComponents()));
        assertEquals(3, state.getTable().getSize());
        assertEquals(CoreConstants.VisibilityMode.VISIBLE_TO_ALL, state.getTable().getVisibilityMode());
        assertEquals(0, state.getExtraHand().getSize());
        assertFalse(state.isDealerChoicePending());
        assertEquals(drawSize, state.getDrawDeck().getSize());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void takingTheExtraHandPutsTheDealersOldHandFaceUpOnTheTable() {
        Set<FrenchCard> oldHand = new HashSet<>(state.getPlayerHand(3).getComponents());
        Set<FrenchCard> extra = new HashSet<>(state.getExtraHand().getComponents());
        List<FrenchCard> otherHand = new ArrayList<>(state.getPlayerHand(1).getComponents());

        fm.next(state, new ChooseHand(true));

        assertEquals(extra, new HashSet<>(state.getPlayerHand(3).getComponents()));
        assertEquals(3, state.getPlayerHand(3).getSize());
        for (int i = 0; i < 3; i++)
            assertTrue("the extra hand is seen only by the dealer", visibleOnlyToOwner(state.getPlayerHand(3), i, 4));
        assertEquals(oldHand, new HashSet<>(state.getTable().getComponents()));
        assertEquals(3, state.getTable().getSize());
        assertEquals(0, state.getExtraHand().getSize());
        assertEquals(otherHand, state.getPlayerHand(1).getComponents());
        assertEquals(0, state.getCurrentPlayer());
        assertAllCardsPresent(state);
    }

    @Test
    public void theChoiceIsNotOfferedAgainOncePlayStarts() {
        fm.next(state, new ChooseHand(true));
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertFalse(actions.contains(new ChooseHand(false)));
        assertFalse(actions.contains(new ChooseHand(true)));
        assertEquals(11, actions.size());
    }
}

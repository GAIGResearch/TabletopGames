package core;

import core.components.PartialObservableDeck;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Test;

import static org.junit.Assert.*;

public class PartialObervableDecks {

    @Test
    public void addingElementToDeckPicksUpDefaultVisibility() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 1, new boolean[]{true, false, false});
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        for (int i = 0; i < 2; i++) {
            assertTrue(deck.isComponentVisible(i, 0));
            assertFalse(deck.isComponentVisible(i, 1));
            assertFalse(deck.isComponentVisible(i, 2));
        }
        deck.setVisibilityOfComponent(1, new boolean[] {true, true, true});
        deck.add(DominionCard.create(CardType.SMITHY), 1); // Add to the middle of the deck
        // this moves the specifically modified card to position 2.
        for (int i = 0; i < 3; i++) {
            if (i != 2) {
                assertTrue(deck.isComponentVisible(i, 0));
                assertFalse(deck.isComponentVisible(i, 1));
                assertFalse(deck.isComponentVisible(i, 2));
            } else {
                assertTrue(deck.isComponentVisible(i, 0));
                assertTrue(deck.isComponentVisible(i, 1));
                assertTrue(deck.isComponentVisible(i, 2));
            }
        }
    }

    @Test
    public void initialisingWithVisibleToAllDoes() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 1, 4, CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        for (int i = 0; i < 2; i++) {
            assertTrue(deck.isComponentVisible(i, 0));
            assertTrue(deck.isComponentVisible(i, 1));
            assertTrue(deck.isComponentVisible(i, 2));
        }
    }

    @Test
    public void initialisingWithHiddenToAllDoes() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 1, 4, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        for (int i = 0; i < 3; i++) {
            assertFalse(deck.isComponentVisible(i, 0));
            assertFalse(deck.isComponentVisible(i, 1));
            assertFalse(deck.isComponentVisible(i, 2));
            assertFalse(deck.isComponentVisible(i, 3));
        }
        deck.setVisibilityOfComponent(0, new boolean[] {false, true, false, true});
        for (int i = 0; i < 3; i++) {
            if (i == 0) {
                assertFalse(deck.isComponentVisible(i, 0));
                assertTrue(deck.isComponentVisible(i, 1));
                assertFalse(deck.isComponentVisible(i, 2));
                assertTrue(deck.isComponentVisible(i, 3));
            } else {
                assertFalse(deck.isComponentVisible(i, 0));
                assertFalse(deck.isComponentVisible(i, 1));
                assertFalse(deck.isComponentVisible(i, 2));
                assertFalse(deck.isComponentVisible(i, 3));
            }
        }
    }

    @Test
    public void initialisingWithVisibleToOwnerDoes() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        for (int i = 0; i < 2; i++) {
            assertFalse(deck.isComponentVisible(i, 0));
            assertFalse(deck.isComponentVisible(i, 1));
            assertTrue(deck.isComponentVisible(i, 2));
            assertFalse(deck.isComponentVisible(i, 3));
        }
    }

    @Test
    public void initialisingWithFirstVisibleToAllDoes() {
        // A deck works on a First In Last Out basis - so we deal the last card to be drawn first (it goes to the bottom of the deck
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.FIRST_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));  // this is the top of the deck and the 'FIRST' card
        for (int i = 0; i < 3; i++) {
            for (int p = 0; p < 4; p++) {
                System.out.println("Checking " + i + " for player " + p);
                if (i == 2) {
                    assertTrue(deck.isComponentVisible(i, p));
                } else {
                    assertFalse(deck.isComponentVisible(i, p));
                }
            }
        }
    }

    @Test
    public void initialisingWithLastVisibleToAllDoes() {
        fail("Not implemented");
    }
}

package core;

import core.components.PartialObservableDeck;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Test;

import java.util.Random;

import static core.CoreConstants.VisibilityMode.HIDDEN_TO_ALL;
import static core.CoreConstants.VisibilityMode.TOP_VISIBLE_TO_ALL;
import static org.junit.Assert.*;

public class PartialObervableDecks {


    Random rnd = new Random(393);
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
        deck.setVisibilityOfComponent(1, new boolean[]{true, true, true});
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
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 1, 4, HIDDEN_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        for (int i = 0; i < 3; i++) {
            assertFalse(deck.isComponentVisible(i, 0));
            assertFalse(deck.isComponentVisible(i, 1));
            assertFalse(deck.isComponentVisible(i, 2));
            assertFalse(deck.isComponentVisible(i, 3));
        }
        deck.setVisibilityOfComponent(0, new boolean[]{false, true, false, true});
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
    public void allVisibleIfDealtOneAtATimeAndTopMostIsVisible() {
        // A deck works on a First In Last Out basis - so we deal the last card to be drawn first (it goes to the bottom of the deck)
        // As we put the cards on one at a time, they are all visible
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.TOP_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));  // this is the top of the deck and the 'FIRST' card
        for (int i = 0; i < 3; i++) {
            for (int p = 0; p < 4; p++) {
                System.out.println(i + " " + p + " " + deck.isComponentVisible(i, p));
                assertTrue(deck.isComponentVisible(i, p));
            }
        }
    }

    @Test
    public void shufflingResetsVisibility() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.TOP_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));  // this is the top of the deck and the 'FIRST' card
        deck.shuffle(rnd);
        for (int i = 0; i < 3; i++) {
            for (int p = 0; p < 4; p++) {
                System.out.println(i + " " + p + " " + deck.isComponentVisible(i, p));
                if (i == 0) {
                    assertTrue(deck.isComponentVisible(i, p));
                } else {
                    assertFalse(deck.isComponentVisible(i, p));
                }
            }
        }
    }

    @Test
    public void initialisingWithBottomVisibleToAllDoes() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.BOTTOM_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));  // this is the top of the deck and the 'FIRST' card
        for (int i = 0; i < 3; i++) {
            for (int p = 0; p < 4; p++) {
                System.out.println(i + " " + p + " " + deck.isComponentVisible(i, p));
                if (i == deck.getSize() - 1) {
                    assertTrue(deck.isComponentVisible(i, p));
                } else {
                    assertFalse(deck.isComponentVisible(i, p));
                }
            }
        }
    }

    @Test
    public void testAddToBottom() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.BOTTOM_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        assertEquals(CardType.COPPER, deck.get(2).cardType());
        deck.addToBottom(DominionCard.create(CardType.MINE));
        assertEquals(CardType.MINE, deck.get(3).cardType());
        assertEquals(CardType.COPPER, deck.get(2).cardType());
        for (int i = 0; i < 4; i++) {
            for (int p = 0; p < 4; p++) {
                System.out.println(i + " " + p + " " + deck.isComponentVisible(i, p));
                if (i > 1) {  // the last two cards are visible
                    assertTrue(deck.isComponentVisible(i, p));
                } else {
                    assertFalse(deck.isComponentVisible(i, p));
                }
            }
        }
    }

    @Test
    public void addingWholeDeckDoesNotIncreaseTopVisibility() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, HIDDEN_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        PartialObservableDeck<DominionCard> deck2 = new PartialObservableDeck<>("Test", 2, 4, TOP_VISIBLE_TO_ALL);
        deck2.add(DominionCard.create(CardType.COPPER));
        deck2.add(deck);
        for (int p = 0; p < 4; p++) {
            assertTrue(deck2.isComponentVisible(0, p));
            assertFalse(deck2.isComponentVisible(1, p));
            assertFalse(deck2.isComponentVisible(2, p));
            assertTrue(deck2.isComponentVisible(3, p));
        }

        deck2.shuffle(rnd);
        for (int p = 0; p < 4; p++) {
            assertTrue(deck2.isComponentVisible(0, p));
            assertFalse(deck2.isComponentVisible(1, p));
            assertFalse(deck2.isComponentVisible(2, p));
            assertFalse(deck2.isComponentVisible(3, p));
        }
    }


    @Test
    public void setVisibilityModeToTopAfterCreatingDeck() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, HIDDEN_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        deck.add(DominionCard.create(CardType.COPPER));
        deck.setVisibility(CoreConstants.VisibilityMode.TOP_VISIBLE_TO_ALL);
        for (int p = 0; p < 4; p++) {
            assertTrue(deck.isComponentVisible(0, p));
            assertFalse(deck.isComponentVisible(1, p));
            assertFalse(deck.isComponentVisible(2, p));
            assertFalse(deck.isComponentVisible(3, p));
        }
    }


    @Test
    public void reshuffleTopDeckWithBottomDeck() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.BOTTOM_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));

        PartialObservableDeck<DominionCard> deck2 = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        deck2.add(DominionCard.create(CardType.MERCHANT));
        deck2.add(DominionCard.create(CardType.GOLD));
        deck2.add(DominionCard.create(CardType.SILVER));
        deck2.setVisibility(CoreConstants.VisibilityMode.TOP_VISIBLE_TO_ALL);  // SILVER

        deck.add(deck2);  // should put deck2 on top of deck
        assertEquals(CardType.SILVER, deck.get(0).cardType());  // top
        assertEquals(CardType.COPPER, deck.get(5).cardType());  // bottom
        for (int p = 0; p < 4; p++) {
            assertTrue(deck.isComponentVisible(0, p));
            assertFalse(deck.isComponentVisible(1, p));
            assertFalse(deck.isComponentVisible(2, p));
            assertFalse(deck.isComponentVisible(3, p));
            assertFalse(deck.isComponentVisible(4, p));
            assertTrue(deck.isComponentVisible(5, p));
        }

        deck.redeterminiseUnknown(rnd, 1);
        assertEquals(CardType.SILVER, deck.get(0).cardType());
        assertFalse(deck.get(1).cardType() == CardType.GOLD &&
                deck.get(2).cardType() == CardType.MERCHANT &&
                deck.get(3).cardType() == CardType.SMITHY &&
                deck.get(4).cardType() == CardType.MILITIA);
        assertEquals(CardType.COPPER, deck.get(5).cardType());
        for (int p = 0; p < 4; p++) {
            assertTrue(deck.isComponentVisible(0, p));
            assertFalse(deck.isComponentVisible(1, p));
            assertFalse(deck.isComponentVisible(2, p));
            assertFalse(deck.isComponentVisible(3, p));
            assertFalse(deck.isComponentVisible(4, p));
            assertTrue(deck.isComponentVisible(5, p));
        }
    }


    @Test
    public void reshuffleSingleDeckWithPerspective() {
        fail("Not yet implemented");
    }

    @Test
    public void reshuffleTwoDecksWithPerspective() {
        fail("Not yet implemented");
    }
}

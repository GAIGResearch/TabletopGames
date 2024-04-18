package core;

import core.components.Deck;
import core.components.PartialObservableDeck;
import games.dominion.cards.CardType;
import games.dominion.cards.DominionCard;
import org.junit.Test;
import utilities.DeterminisationUtilities;

import java.util.*;

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
        PartialObservableDeck<DominionCard> deck2 = new PartialObservableDeck<>("Test", 2, 4, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        deck2.add(DominionCard.create(CardType.MERCHANT));
        deck2.setVisibilityOfComponent(0, new boolean[]{true, false, true, false});
        deck2.add(DominionCard.create(CardType.GOLD));
        deck2.setVisibilityOfComponent(0, new boolean[]{true, false, false, false});
        deck2.add(DominionCard.create(CardType.SILVER));
        deck2.add(DominionCard.create(CardType.COPPER));
        deck2.setVisibilityOfComponent(0, new boolean[]{false, true, false, false});
        deck2.add(DominionCard.create(CardType.MILITIA));
        deck2.add(DominionCard.create(CardType.SMITHY));

        deck2.redeterminiseUnknown(rnd, 0);
        assertEquals(CardType.MERCHANT, deck2.get(5).cardType());
        assertArrayEquals(new boolean[]{true, false, true, false}, deck2.getVisibilityOfComponent(5));
        assertEquals(CardType.GOLD, deck2.get(4).cardType());
        assertArrayEquals(new boolean[]{true, false, false, false}, deck2.getVisibilityOfComponent(4));
        assertArrayEquals(new boolean[]{false, false, false, false}, deck2.getVisibilityOfComponent(3));
        assertArrayEquals(new boolean[]{false, true, false, false}, deck2.getVisibilityOfComponent(2));

        deck2.redeterminiseUnknown(rnd, 2);
        assertEquals(CardType.MERCHANT, deck2.get(5).cardType());
        assertArrayEquals(new boolean[]{true, false, true, false}, deck2.getVisibilityOfComponent(5));
        assertArrayEquals(new boolean[]{true, false, false, false}, deck2.getVisibilityOfComponent(4));
        assertNotEquals(CardType.GOLD, deck2.get(4).cardType());
        assertNotEquals(CardType.SILVER, deck2.get(3).cardType());
        CardType card2 = deck2.get(2).cardType(); // the one that player 1 knows
        CardType card3 = deck2.get(3).cardType(); // the one that player 1 does not know

        deck2.redeterminiseUnknown(rnd, 1);
        assertEquals(card2, deck2.get(2).cardType());
        assertNotEquals(card3, deck2.get(3).cardType());
        // visibility is tracked, even if the actual cards aren't
        assertArrayEquals(new boolean[]{false, true, false, false}, deck2.getVisibilityOfComponent(2));
        assertArrayEquals(new boolean[]{true, false, true, false}, deck2.getVisibilityOfComponent(5));

    }

    @Test
    public void reshuffleTwoDecks() {
        Deck<DominionCard> deck = new Deck<>("Test", 2, TOP_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        deck.add(DominionCard.create(CardType.COPPER));
        deck.shuffle(rnd);  // so that only the top card is visible
        CardType top1 = deck.get(0).cardType();
        CardType next1 = deck.get(1).cardType();

        Deck<DominionCard> deck2 = new Deck<>("Test", 2, TOP_VISIBLE_TO_ALL);
        deck2.add(DominionCard.create(CardType.GOLD));
        deck2.add(DominionCard.create(CardType.MERCHANT));
        deck2.add(DominionCard.create(CardType.SILVER));
        deck2.add(DominionCard.create(CardType.WORKSHOP));
        deck2.shuffle(rnd);  // so that only the top card is visible
        CardType top2 = deck2.get(0).cardType();
        CardType next2 = deck2.get(1).cardType();

        // reshuffle everything (all players are the same here)
        DeterminisationUtilities.reshuffle(2, List.of(deck, deck2), c -> true, rnd);
        assertEquals(top1, deck.get(0).cardType());
        assertEquals(top2, deck2.get(0).cardType());
        assertNotEquals(next1, deck.get(1).cardType());
        assertNotEquals(next2, deck2.get(1).cardType());
    }

    @Test
    public void shuffleDeckAndPartialObservableDeck() {
        PartialObservableDeck<DominionCard> deck = new PartialObservableDeck<>("Test", 2, 4, TOP_VISIBLE_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        deck.add(DominionCard.create(CardType.COPPER));
        deck.shuffle(rnd);  // so that only the top card is visible
        deck.setVisibilityOfComponent(1, 2, true);
        CardType card0 = deck.get(0).cardType();
        CardType card1 = deck.get(1).cardType();
        CardType card2 = deck.get(2).cardType();

        Deck<DominionCard> deck2 = new Deck<>("Test", 2, TOP_VISIBLE_TO_ALL);
        deck2.add(DominionCard.create(CardType.GOLD));
        deck2.add(DominionCard.create(CardType.MERCHANT));
        deck2.add(DominionCard.create(CardType.SILVER));
        deck2.add(DominionCard.create(CardType.WORKSHOP));
        deck2.shuffle(rnd);  // so that only the top card is visible
        CardType card0_2 = deck2.get(0).cardType();
        CardType card1_2 = deck2.get(1).cardType();

        assertEquals(card0, deck.get(0).cardType());
        assertEquals(card0_2, deck2.get(0).cardType());
        List<CardType> deckCard2 = new ArrayList<>();
        List<CardType> deck2Card1 = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            DeterminisationUtilities.reshuffle(2, List.of(deck, deck2), c -> true, rnd);
            assertEquals(card0, deck.get(0).cardType());
            assertEquals(card0_2, deck2.get(0).cardType());
            assertEquals(card1, deck.get(1).cardType());
            assertArrayEquals(new boolean[]{false, false, true, false}, deck.getVisibilityOfComponent(1));
            assertArrayEquals(new boolean[]{true, true, true, true}, deck.getVisibilityOfComponent(0));
            assertArrayEquals(new boolean[]{false, false, false, false}, deck.getVisibilityOfComponent(2));
            deckCard2.add(deck.get(2).cardType());
            deck2Card1.add(deck2.get(1).cardType());
        }
        // some shuffles will have the same card in the same position; but should be 3 or less in each case of the 10 shuffles
        assertEquals(1, deckCard2.stream().filter(c -> c == card2).count(), 2);
        assertEquals(1, deck2Card1.stream().filter(c -> c == card1_2).count(), 2);
    }

    @Test
    public void shuffleTwoDecksWithPredicate() {
        Deck<DominionCard> deck = new Deck<>("Test", 2, HIDDEN_TO_ALL);
        deck.add(DominionCard.create(CardType.COPPER));
        deck.add(DominionCard.create(CardType.MILITIA));
        deck.add(DominionCard.create(CardType.SMITHY));
        deck.add(DominionCard.create(CardType.COPPER));
        deck.shuffle(rnd);  // so that only the top card is visible
        Deck<DominionCard> deckCopy = deck.copy();

        Deck<DominionCard> deck2 = new Deck<>("Test", 2, HIDDEN_TO_ALL);
        deck2.add(DominionCard.create(CardType.GOLD));
        deck2.add(DominionCard.create(CardType.MERCHANT));
        deck2.add(DominionCard.create(CardType.SILVER));
        deck2.add(DominionCard.create(CardType.WORKSHOP));
        deck2.shuffle(rnd);  // so that only the top card is visible
        Deck<DominionCard> deck2Copy = deck2.copy();

        int[] nonShuffledCount = new int[2];

        // reshuffle everything (all players are the same here)
        for (int i = 0; i < 10; i++) {
            DeterminisationUtilities.reshuffle(2, List.of(deck, deck2), c -> c.cardType() != CardType.COPPER, rnd);
            // now go through both decks, and check COPPER in same position as in copy
            // and track the count of other cards in the same position
            for (int j = 0; j < 4; j++) {
                if (deck.get(j).cardType() == CardType.COPPER) {
                    assertEquals(deckCopy.get(j).cardType(), deck.get(j).cardType());
                } else {
                    nonShuffledCount[0] += (deckCopy.get(j).cardType() == deck.get(j).cardType()) ? 1 : 0;
                }
                // No COPPER in deck2
                nonShuffledCount[1] += (deck2Copy.get(j).cardType() == deck2.get(j).cardType()) ? 1 : 0;
            }
        }
        // 6 non-COPPER, so 1 in 6 shuffles will leave any given card unchanged
        assertEquals(3, nonShuffledCount[0], 3); // 2 cards, so expect 20/6 = 3
        assertEquals(7, nonShuffledCount[1], 3); // 4 cards, so expect 40/6 = 7
    }
}

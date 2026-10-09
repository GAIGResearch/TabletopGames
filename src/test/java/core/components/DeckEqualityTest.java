package core.components;

import org.junit.Test;

import static core.CoreConstants.VisibilityMode.*;
import static org.junit.Assert.*;

/**
 * Deck and PartialObservableDeck equality: who can see which cards is part of a deck's value, so two decks holding
 * the same cards with different visibility are not equal. Equal decks must also have equal hash codes.
 */
public class DeckEqualityTest {

    static final FrenchCard ACE = new FrenchCard(FrenchCard.FrenchCardType.Ace, FrenchCard.Suite.Spades);
    static final FrenchCard SEVEN = new FrenchCard(FrenchCard.FrenchCardType.Number, FrenchCard.Suite.Hearts, 7);
    static final FrenchCard KING = new FrenchCard(FrenchCard.FrenchCardType.King, FrenchCard.Suite.Clubs);

    static final boolean[] NOBODY = {false, false, false};
    static final boolean[] ALL = {true, true, true};
    static final boolean[] ONLY_1 = {false, true, false};

    private static PartialObservableDeck<FrenchCard> partialDeck() {
        PartialObservableDeck<FrenchCard> deck = new PartialObservableDeck<>("Grid", 1, NOBODY);
        deck.add(ACE, NOBODY);
        deck.add(SEVEN, ONLY_1);
        deck.add(KING, ALL);
        return deck;
    }

    private static void assertEqualWithSameHash(Object a, Object b) {
        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
    }

    // ---------------------------------------------------------------- Deck

    @Test
    public void aCopiedDeckIsEqualWithTheSameHash() {
        Deck<FrenchCard> deck = new Deck<>("Hand", 0, VISIBLE_TO_OWNER);
        deck.add(ACE);
        deck.add(SEVEN);
        assertEqualWithSameHash(deck, deck.copy());
    }

    @Test
    public void decksDifferingOnlyInVisibilityModeAreNotEqual() {
        Deck<FrenchCard> hidden = new Deck<>("Pile", 0, HIDDEN_TO_ALL);
        hidden.add(ACE);
        Deck<FrenchCard> visible = hidden.copy();
        visible.setVisibility(VISIBLE_TO_ALL);
        assertNotEquals(hidden, visible);
        assertNotEquals(visible, hidden);
    }

    @Test
    public void decksDifferingOnlyInOwnerAreNotEqual() {
        // hashCode has always included the owner, so equality must too or equal decks could hash differently
        Deck<FrenchCard> deck = new Deck<>("Hand", 0, VISIBLE_TO_OWNER);
        deck.add(ACE);
        Deck<FrenchCard> other = deck.copy();
        other.setOwnerId(1);
        assertNotEquals(deck, other);
    }

    // ---------------------------------------------------------------- PartialObservableDeck

    @Test
    public void aCopiedPartialObservableDeckIsEqualWithTheSameHash() {
        PartialObservableDeck<FrenchCard> deck = partialDeck();
        assertEqualWithSameHash(deck, deck.copy());
    }

    @Test
    public void aCopiedPartialObservableDeckKeepsItsVisibilityMode() {
        // the copy constructor sets MIXED_VISIBILITY, so copy() must restore the original's mode
        PartialObservableDeck<FrenchCard> deck = new PartialObservableDeck<>("Hand", 1, 3, VISIBLE_TO_OWNER);
        deck.add(ACE);
        deck.add(SEVEN);
        PartialObservableDeck<FrenchCard> copy = deck.copy();
        assertEquals(VISIBLE_TO_OWNER, copy.getVisibilityMode());
        assertEquals(VISIBLE_TO_OWNER, deck.copy(0).getVisibilityMode());
        assertEqualWithSameHash(deck, copy);
    }

    @Test
    public void turningOneCardFaceUpMakesThePartialObservableDeckUnequal() {
        PartialObservableDeck<FrenchCard> deck = partialDeck();
        PartialObservableDeck<FrenchCard> copy = deck.copy();
        // index 0 is the King (the last card added), visible to all; index 2 is the Ace, visible to nobody
        copy.setVisibilityOfComponent(2, ALL);
        assertEquals("same cards in the same order", deck.getComponents(), copy.getComponents());
        assertNotEquals(deck, copy);
        assertNotEquals(copy, deck);
    }

    @Test
    public void showingACardToOneMorePlayerMakesThePartialObservableDeckUnequal() {
        PartialObservableDeck<FrenchCard> deck = partialDeck();
        PartialObservableDeck<FrenchCard> copy = deck.copy();
        copy.setVisibilityOfComponent(1, 0, true);  // the Seven, seen by player 1, is now seen by player 0 too
        assertNotEquals(deck, copy);
    }

    @Test
    public void restoringTheVisibilityMakesThemEqualAgainWithTheSameHash() {
        PartialObservableDeck<FrenchCard> deck = partialDeck();
        PartialObservableDeck<FrenchCard> copy = deck.copy();
        copy.setVisibilityOfComponent(2, ALL);
        copy.setVisibilityOfComponent(2, NOBODY);
        assertEqualWithSameHash(deck, copy);
    }

    @Test
    public void partialObservableDecksDifferingOnlyInDefaultVisibilityAreNotEqual() {
        // the default visibility decides who sees the next card added, so it is part of the deck's state
        PartialObservableDeck<FrenchCard> deck = partialDeck();
        PartialObservableDeck<FrenchCard> other = new PartialObservableDeck<>("Grid", 1, ONLY_1, deck.getComponentID());
        other.add(ACE, NOBODY);
        other.add(SEVEN, ONLY_1);
        other.add(KING, ALL);
        assertEquals(deck.getComponents(), other.getComponents());
        assertNotEquals(deck, other);
    }

    @Test
    public void equalityComparesVisibilityByValueNotByArrayIdentity() {
        // each deck holds its own boolean arrays, so a reference comparison would never find two decks equal
        PartialObservableDeck<FrenchCard> deck = partialDeck();
        PartialObservableDeck<FrenchCard> other = new PartialObservableDeck<>("Grid", 1, NOBODY.clone(),
                deck.getComponentID());
        other.add(ACE, NOBODY.clone());
        other.add(SEVEN, ONLY_1.clone());
        other.add(KING, ALL.clone());
        assertEqualWithSameHash(deck, other);
    }

    @Test
    public void aPartialObservableDeckIsNotEqualToAPlainDeckWithTheSameCards() {
        PartialObservableDeck<FrenchCard> partial = partialDeck();
        Deck<FrenchCard> plain = new Deck<>("Grid", 1, partial.getComponentID(), partial.getVisibilityMode());
        plain.add(partial.getComponents().get(2));
        plain.add(partial.getComponents().get(1));
        plain.add(partial.getComponents().get(0));
        assertEquals(partial.getComponents(), plain.getComponents());
        assertNotEquals(partial, plain);
        assertNotEquals(plain, partial);
    }
}

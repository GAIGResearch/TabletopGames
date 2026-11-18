package core.components;

import core.CoreConstants;
import org.junit.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.RandomAccess;

import static org.junit.Assert.*;

/**
 * Deck access is mostly by index (get, peek, pick, iteration), so the components must be held in a list with
 * constant-time indexed access. With a LinkedList, iterating over a deck is O(n^2).
 */
public class DeckStorageTest {

    private static Deck<Card> deckOf(int n) {
        Deck<Card> deck = new Deck<>("deck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        for (int i = 0; i < n; i++)
            deck.add(new Card("card" + i));
        return deck;
    }

    @Test
    public void newDeckHasRandomAccessStorage() {
        assertTrue(deckOf(3).getComponents() instanceof RandomAccess);
    }

    @Test
    public void copiedDeckHasRandomAccessStorage() {
        Deck<Card> deck = deckOf(3);
        assertTrue(deck.copy().getComponents() instanceof RandomAccess);
        assertTrue(((Deck<?>) deck.copy(0)).getComponents() instanceof RandomAccess);
    }

    @Test
    public void deckGivenALinkedListStillHasRandomAccessStorage() {
        Deck<Card> deck = deckOf(0);
        deck.setComponents(new LinkedList<>(List.of(new Card("a"), new Card("b"))));
        assertTrue(deck.getComponents() instanceof RandomAccess);
        assertEquals(2, deck.getSize());
    }

    @Test
    public void partialObservableDeckHasRandomAccessStorage() {
        PartialObservableDeck<Card> deck = new PartialObservableDeck<>("pod", -1, 2, CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        deck.add(new Card("a"));
        deck.add(new Card("b"));
        assertTrue(deck.getComponents() instanceof RandomAccess);
        assertTrue(deck.copy().getComponents() instanceof RandomAccess);
        assertTrue(deck.copy(0).getComponents() instanceof RandomAccess);
    }

    @Test
    public void iterationVisitsComponentsInDeckOrder() {
        Deck<Card> deck = deckOf(50);
        List<Card> seen = new ArrayList<>();
        for (Card c : deck) seen.add(c);
        assertEquals(deck.getComponents(), seen);
    }

    @Test
    public void addingADeckOrCollectionSetsOwner() {
        Deck<Card> target = new Deck<>("target", 1, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
        target.add(deckOf(2));
        target.add(List.of(new Card("x")));
        assertEquals(3, target.getSize());
        assertTrue(target.getComponents().stream().allMatch(c -> c.getOwnerId() == 1));
    }
}

package core.actions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;

import static core.CoreConstants.VisibilityMode;


public class RearrangeDeckOfCards extends DrawCard {
    protected int[] newCardOrder;
    protected int rearrangeDeck;

    /**
     * Changes the order of the first N cards in a deck, by playing a card.
     * @param deckFrom - origin deck of card played for this action
     * @param deckTo - destination deck for card played for this action (after discarded)
     * @param fromIndex - index from origin deck of card played for this action
     * @param rearrangeDeck - deck containing cards to rearrange
     * @param newCardOrder - new order for the first N cards, where N = length of the order array
     */
    public RearrangeDeckOfCards(int deckFrom, int deckTo, int fromIndex, int rearrangeDeck, int[] newCardOrder) {
        super(deckFrom, deckTo, fromIndex);
        this.rearrangeDeck = rearrangeDeck;
        this.newCardOrder = newCardOrder;
    }

    /**
     * Rearranging the deck without playing a card.
     * @param rearrangeDeck - deck to rearrange.
     * @param newCardOrder - new order for first N cards in the deck, where N = length of the order array
     */
    public RearrangeDeckOfCards(int rearrangeDeck, int[] newCardOrder) {
        this(-1, -1, -1, rearrangeDeck, newCardOrder);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discard card played
        boolean result = true;
        if (deckFrom != -1) result = super.execute(gs);
        Deck<Card> rd = (Deck<Card>) gs.getComponentById(rearrangeDeck);

        Card[] cards = new Card[newCardOrder.length];
        for (int value : newCardOrder) {
            cards[value] = rd.draw();
        }
        Deck<Card> draws = new Deck<>("Temp Draws from: " + rd.getComponentName(), VisibilityMode.HIDDEN_TO_ALL);
        draws.setComponents(new ArrayList<>(Arrays.asList(cards)));
        //return result & rd.add(draws);
        return result & rd.add(draws, 0);
    }

    // Getters
    public int[] getNewCardOrder() {
        return newCardOrder;
    }
    public int getRearrangeDeck() {
        return rearrangeDeck;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RearrangeDeckOfCards)) return false;
        if (!super.equals(o)) return false;
        RearrangeDeckOfCards that = (RearrangeDeckOfCards) o;
        return rearrangeDeck == that.rearrangeDeck &&
                Arrays.equals(newCardOrder, that.newCardOrder);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(super.hashCode(), rearrangeDeck);
        result = 31 * result + Arrays.hashCode(newCardOrder);
        return result;
    }

    @Override
    public String toString() {
        return "RearrangeCardsWithCard{" +
                "newCardOrder=" + Arrays.toString(newCardOrder) +
                ", rearrangeDeck=" + rearrangeDeck +
                '}';
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "RearrangeCardsWithCard{" +
                "newCardOrder=" + Arrays.toString(newCardOrder) +
                ", rearrangeDeck=" + gameState.getComponentById(rearrangeDeck).getComponentName() +
                ", card=" + gameState.getComponentById(cardId).getComponentName() +
                '}';
    }

    @Override
    public AbstractAction copy() {
        return new RearrangeDeckOfCards(deckFrom, deckTo, fromIndex, rearrangeDeck, newCardOrder);
    }
}

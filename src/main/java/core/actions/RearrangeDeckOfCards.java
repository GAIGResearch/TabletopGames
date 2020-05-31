package core.actions;

import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;


public class RearrangeDeckOfCards extends DrawCard {
    protected int[] newCardOrder;
    protected int rearrangeDeck;

    public RearrangeDeckOfCards(int deckFrom, int deckTo, int fromIndex, int rearrangeDeck, int[] newCardOrder) {
        super(deckFrom, deckTo, fromIndex);
        this.rearrangeDeck = rearrangeDeck;
        this.newCardOrder = newCardOrder;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Discard card played
        boolean result = super.execute(gs);
        Deck<Card> rd = (Deck<Card>) gs.getComponentById(rearrangeDeck);

        Card[] cards = new Card[newCardOrder.length];
        for (int value : newCardOrder) {
            cards[value] = rd.draw();
        }
        Deck<Card> draws = new Deck<>("Temp Draws from: " + rd.getComponentName());
        draws.setComponents(new ArrayList<>(Arrays.asList(cards)));
        return result & rd.add(draws);
    }

    public int[] getNewCardOrder() {
        return newCardOrder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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
}

package core.actions;

import core.components.Card;
import core.components.Component;
import core.components.Deck;
import core.AbstractGameState;

import java.util.Objects;

public class DrawCard extends AbstractAction {

    protected int deckFrom;
    protected int deckTo;
    protected int fromIndex;
    protected int toIndex;

    protected int cardId;  // Component ID of the card moved, updated after the action is executed
    protected boolean executed;  // Indicates whether the action executed or not

    /**
     * This action moves one card (given by index in its origin deck) from a deck to another.
     * @param deckFrom - origin deck from which card will be moved.
     * @param deckTo - destination deck to which card will be moved.
     * @param fromIndex - index in the origin deck where the card can be found.
     * @param toIndex - index in the destination deck where the card should be placed.
     */
    public DrawCard (int deckFrom, int deckTo, int fromIndex, int toIndex) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.fromIndex = fromIndex;
        this.toIndex = toIndex;
    }

    public DrawCard (int deckFrom, int deckTo, int fromIndex) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.fromIndex = fromIndex;
        this.toIndex = 0;
    }

    public DrawCard (int deckFrom, int deckTo) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.fromIndex = 0;
        this.toIndex = 0;
    }

    public DrawCard(){}

    @Override
    public boolean execute(AbstractGameState gs) {
        Deck<Card> from = (Deck<Card>) gs.getComponentById(deckFrom);
        Deck<Card> to = (Deck<Card>) gs.getComponentById(deckTo);
        if (from != null && to != null) {
            Card card = from.pick(fromIndex);
            if (card != null) {
                cardId = card.getComponentID();
                if (to.add(card, toIndex)) {
                    executed = true;
                    return true;
                }
            }
        }
        return false;
    }

    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            if (fromIndex == -1) return null;
            Deck<Card> deck = (Deck<Card>) gs.getComponentById(deckFrom);
            if (deck != null) return deck.get(fromIndex);
            return (Card) gs.getComponentById(cardId);
        }
        return (Card) gs.getComponentById(cardId);
    }

    // Getters
    public int getCardId() {
        return cardId;
    }
    public int getFromIndex() {
        return fromIndex;
    }
    public int getToIndex() {
        return toIndex;
    }
    public int getDeckFrom() {
        return deckFrom;
    }
    public int getDeckTo() {
        return deckTo;
    }

    @Override
    public AbstractAction copy() {
        return new DrawCard(deckFrom, deckTo, fromIndex, toIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DrawCard)) return false;
        DrawCard drawCard = (DrawCard) o;
        return deckFrom == drawCard.deckFrom &&
                deckTo == drawCard.deckTo &&
                fromIndex == drawCard.fromIndex &&
                toIndex == drawCard.toIndex;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckFrom, deckTo, fromIndex, toIndex);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        Component deckF = gameState.getComponentById(deckFrom);
        Component deckT = gameState.getComponentById(deckTo);
        Component card = getCard(gameState);
        return "DrawCard{" +
                "deckFrom=" + (deckF != null? deckF.getComponentName() : "deck-from-not-found") +
                ", deckTo=" + (deckT != null? deckT.getComponentName() : "deck-to-not-found") +
                ", card=" + (card != null? card.getComponentName() : "card-not-found") +
                ", toIndex=" + toIndex +
                '}';
    }

    @Override
    public String toString() {
        return "DrawCard{" +
                "deckFrom=" + deckFrom +
                ", deckTo=" + deckTo +
                ", fromIndex=" + fromIndex +
                ", toIndex=" + toIndex +
                ", cardId=" + cardId +
                ", executed=" + executed +
                '}';
    }
}


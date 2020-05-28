package core.actions;

import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;

import java.util.Objects;

public class DrawCard extends AbstractAction {

    protected int deckFrom;
    protected int deckTo;
    protected int fromIndex;
    protected int toIndex;
    protected int cardId;

    protected boolean executed;

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

    @Override
    public boolean execute(AbstractGameState gs) {
        executed = true;
        Deck<Card> from = (Deck<Card>) gs.getComponentById(deckFrom);
        Deck<Card> to = (Deck<Card>) gs.getComponentById(deckTo);
        Card card = from.pick(fromIndex);
        if (card != null) {
            cardId = card.getComponentID();
        }
        return card != null && to.add(card, toIndex);
    }

    @Override
    public Card getCard(AbstractGameState gs) {
        if (!executed) {
            Deck<Card> deck = (Deck<Card>) gs.getComponentById(deckFrom);
            return deck.getComponents().get(fromIndex);
        }
        return (Card) gs.getComponentById(cardId);
    }

    @Override
    public AbstractAction copy() {
        DrawCard action = new DrawCard(deckFrom, deckTo, fromIndex, toIndex);
        action.cardId = cardId;
        action.executed = executed;
        return action;
    }

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DrawCard drawCard = (DrawCard) o;
        return deckFrom == drawCard.deckFrom &&
                deckTo == drawCard.deckTo &&
                fromIndex == drawCard.fromIndex &&
                toIndex == drawCard.toIndex &&
                cardId == drawCard.cardId &&
                executed == drawCard.executed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckFrom, deckTo, fromIndex, toIndex, cardId, executed);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
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


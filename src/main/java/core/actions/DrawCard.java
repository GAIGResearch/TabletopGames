package core.actions;

import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;

import java.util.Objects;

public class DrawCard implements IAction {

    private Deck<Card> deckFrom;
    private Deck<Card> deckTo;

    private int index;

    public DrawCard (Deck<Card> deckFrom, Deck<Card> deckTo, int index) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.index = index;
    }

    public DrawCard (Deck<Card> deckFrom, Deck<Card> deckTo) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        Card card;
        if (index != -1){
            card = deckFrom.pick(index);
        } else {
            card = deckFrom.draw();
        }
        if (card == null) {
            return false;
        }
        return deckTo.add(card);
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof DrawCard)
        {
            DrawCard otherAction = (DrawCard) other;
            return deckFrom.equals(otherAction.deckFrom) && deckTo.equals(otherAction.deckTo);

        }else return false;
    }

    @Override
    public String toString() {
        return "DrawCard{" +
                "deckFrom=" + deckFrom.getID() +
                ", deckTo=" + deckTo.getID() +
                ", index=" + index +
                '}';
    }

    public int getIndex() {
        return index;
    }

    public Deck<Card> getDeckFrom() {
        return deckFrom;
    }

    public Deck<Card> getDeckTo() {
        return deckTo;
    }

    public Card getDrawCard() {
        return deckFrom.getCards().get(index);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deckFrom, deckTo, index);
    }
}


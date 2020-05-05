package core.actions;

import core.components.Card;
import core.components.IDeck;
import core.AbstractGameState;

public class DrawCard implements IAction {

    private IDeck<Card> deckFrom;
    private IDeck<Card> deckTo;

    private int index;

    public DrawCard (IDeck<Card> deckFrom, IDeck<Card> deckTo, int index) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
        this.index = index;
    }

    public DrawCard (IDeck<Card> deckFrom, IDeck<Card> deckTo) {
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

    public IDeck<Card> getDeckFrom() {
        return deckFrom;
    }

    public IDeck<Card> getDeckTo() {
        return deckTo;
    }
}


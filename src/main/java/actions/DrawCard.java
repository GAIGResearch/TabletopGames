package actions;

import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;

public class DrawCard implements Action {

    //TODO: turn this into IDs and do not use two ways of creating this action.
    private String deckIdFrom;
    private String deckIdTo;

    private IDeck<Card> deckFrom;
    private IDeck<Card> deckTo;

    private int index;

    public DrawCard (String deckFrom, String deckTo) {
        this.deckIdFrom = deckFrom;
        this.deckIdTo = deckTo;
    }

    public DrawCard (String deckFrom, String deckTo, int index) {
        this.deckIdFrom = deckFrom;
        this.deckIdTo = deckTo;
        this.index = index;
    }

    public DrawCard (IDeck<Card> deckFrom, IDeck<Card> deckTo) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(GameState gs) {
        Card card;
        if (deckFrom == null) {
            deckFrom = (IDeck<Card>)gs.findDeck(deckIdFrom);
        }
        if (deckTo == null) {
            deckTo = (IDeck<Card>)gs.findDeck(deckIdTo);
        }

        if (index != -1){
            card = deckFrom.pick(index);
        } else {
            card = deckFrom.draw();
        }
        if (card == null) {
            return false;
        }
        return deckTo.add(card);

        // TODO: if you can't draw cards, game over
        // TODO: discard if too many / play event card
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof DrawCard)
        {
            DrawCard otherAction = (DrawCard) other;
            return deckIdFrom.equals(otherAction.deckIdFrom) && deckIdTo.equals(otherAction.deckIdTo) &&
                    deckFrom.equals(otherAction.deckFrom) && deckTo.equals(otherAction.deckTo);

        }else return false;
    }
}

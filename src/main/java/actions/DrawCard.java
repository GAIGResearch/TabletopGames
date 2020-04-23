package actions;

import actions.Action;
import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;

public class DrawCard implements Action {
    private String deckIdFrom;
    private String deckIdTo;

    private IDeck deckFrom;
    private IDeck deckTo;

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

    public DrawCard (IDeck deckFrom, IDeck deckTo) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(GameState gs) {
        Card card;
        if (deckFrom == null) {
            deckFrom = gs.findDeck(deckIdFrom);
        }
        if (deckTo == null) {
            deckTo = gs.findDeck(deckIdTo);
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

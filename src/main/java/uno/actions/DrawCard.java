package uno.actions;

import actions.Action;
import components.Card;
import components.Deck;
import core.GameState;

public class DrawCard implements Action {
    private String deckIdFrom;
    private String deckIdTo;

    private Deck deckFrom;
    private Deck deckTo;

    public DrawCard (String deckFrom, String deckTo) {
        this.deckIdFrom = deckFrom;
        this.deckIdTo = deckTo;
    }

    public DrawCard (Deck deckFrom, Deck deckTo) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(GameState gs) {
        if (deckFrom == null) {
            deckFrom = (Deck) gs.findDeck(deckIdFrom);
        }
        if (deckTo == null) {
            deckTo = (Deck) gs.findDeck(deckIdTo);
        }
        Card c = deckFrom.draw();
        if (c == null) {
            return false;
        }
        return deckTo.add(c);

        // TODO: no more card on deck
    }
}

package actions;

import components.Card;
import components.Deck;
import core.GameState;

public class DrawCard implements Action {
    private int deckFrom;
    private int deckTo;

    public DrawCard (int deckFrom, int deckTo) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(GameState gs) {
        Deck from = gs.findDeck(deckFrom);
        Deck to = gs.findDeck(deckTo);
        Card c = from.draw();
        if (c == null) return false;
        return to.add(c);
    }
}

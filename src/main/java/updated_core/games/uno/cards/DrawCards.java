package updated_core.games.uno.cards;

import actions.Action;
import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;

public class DrawCard<T> implements Action {

    private IDeck<T> deckFrom;
    private IDeck<T> deckTo;

    public DrawCard (IDeck<T> deckFrom, IDeck<T> deckTo) {
        this.deckFrom = deckFrom;
        this.deckTo = deckTo;
    }

    @Override
    public boolean execute(GameState gs) {
        T card = deckFrom.draw();
        return deckTo.add(card);
    }
}

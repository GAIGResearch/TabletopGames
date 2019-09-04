package actions;

import components.Card;
import components.Deck;
import core.GameState;

public class AddCardToDeck implements Action {
    private Card card;
    private int deck;

    AddCardToDeck(Card c, int deck) {
        this.card = c;
        this.deck = deck;
    }

    @Override
    public boolean execute(GameState gs) {
        Deck d = gs.findDeck(deck);
        return d.add(card);
    }
}

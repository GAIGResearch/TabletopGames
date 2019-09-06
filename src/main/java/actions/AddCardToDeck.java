package actions;

import components.Card;
import components.Deck;
import core.GameState;

public class AddCardToDeck implements Action {
    private Card card;
    private Deck deck;

    public AddCardToDeck(Card c, Deck deck) {
        this.card = c;
        this.deck = deck;
    }

    @Override
    public boolean execute(GameState gs) {
        return deck.add(card);
    }
}

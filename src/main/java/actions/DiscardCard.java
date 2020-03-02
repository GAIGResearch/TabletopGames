package actions;

import components.Card;
import components.Deck;
import core.GameState;

public class DiscardCard implements Action {
    private Deck deck;
    private int cardIndex;

    public DiscardCard (Deck deck, int cardIndex) {
        this.deck = deck;
        this.cardIndex = cardIndex;
    }


    @Override
    public boolean execute(GameState gs) {
        Card c = deck.pick(cardIndex);
        Deck discardDeck = gs.findDeck("Player Discard Deck");
        return discardDeck.add(c);

    }
}

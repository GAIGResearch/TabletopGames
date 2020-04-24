package actions;

import components.Card;
import components.Deck;
import core.GameState;

public class DiscardCard implements Action {
    private Deck<Card> deck;
    private int cardIndex;

    public DiscardCard (Deck<Card> deck, int cardIndex) {
        this.deck = deck;
        this.cardIndex = cardIndex;
    }


    @Override
    public boolean execute(GameState gs) {
        Card c = deck.pick(cardIndex);
        // todo discardDeck == null at some point
        Deck<Card> discardDeck = gs.findDeck("Player Deck Discard");
        return discardDeck.add(c);

    }
}

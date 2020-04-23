package actions;

import actions.Action;
import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;

public class DiscardCard implements Action {
    private IDeck deck;
    private int cardIndex;

    public DiscardCard (IDeck deck, int cardIndex) {
        this.deck = deck;
        this.cardIndex = cardIndex;
    }


    @Override
    public boolean execute(GameState gs) {
        Card c = deck.pick(cardIndex);
        // todo discardDeck == null at some point
        IDeck discardDeck = gs.findDeck("Player Deck Discard");
        return discardDeck.add(c);

    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof DiscardCard)
        {
            DiscardCard otherAction = (DiscardCard) other;
            return cardIndex == otherAction.cardIndex && deck.equals(otherAction.deck);

        }else return false;
    }
}

package actions;

import actions.Action;
import components.Card;
import components.Deck;
import components.IDeck;
import core.GameState;

public class AddCardToDeck implements Action {
    private Card card;
    private IDeck deck;
    private int index;

    public AddCardToDeck(Card c, IDeck deck) {
        this.card = c;
        this.deck = deck;
        this.index = 0;
    }

    public AddCardToDeck(Card c, IDeck deck, int index) {
        this.card = c;
        this.deck = deck;
        this.index = index;
    }

    @Override
    public boolean execute(GameState gs) {
        return deck.add(card, index);
    }

    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof AddCardToDeck)
        {
            AddCardToDeck otherAction = (AddCardToDeck) other;
            return index == otherAction.index && card.equals(otherAction.card) && deck.equals(otherAction.deck);

        }else return false;
    }
}

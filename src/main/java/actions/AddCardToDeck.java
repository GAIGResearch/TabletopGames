package actions;

import actions.Action;
import components.Card;
import components.Deck;
import core.GameState;

public class AddCardToDeck implements Action {
    private Card card;
    private Deck deck;
    private int index;

    public AddCardToDeck(Card c, Deck deck) {
        this.card = c;
        this.deck = deck;
        this.index = 0;
    }

    public AddCardToDeck(Card c, Deck deck, int index) {
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

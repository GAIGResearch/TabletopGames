package core.actions;

import core.components.Card;
import core.components.IDeck;
import core.AbstractGameState;

public class AddCardToDeck implements IAction {
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
    public boolean execute(AbstractGameState gs) {
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

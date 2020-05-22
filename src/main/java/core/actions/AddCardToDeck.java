package core.actions;

import core.components.Card;
import core.components.Deck;
import core.AbstractGameState;

public class AddCardToDeck implements IAction {
    private Card card;
    private Deck<Card> deck;
    private int index;

    public AddCardToDeck(Card c, Deck<Card> deck) {
        this.card = c;
        this.deck = deck;
        this.index = 0;
    }

    public AddCardToDeck(Card c, Deck<Card> deck, int index) {
        this.card = c;
        this.deck = deck;
        this.index = index;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return deck.add(card, index);
    }

    @Override
    public Card getCard() {
        return null;
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

    @Override
    public String toString() {
        return "AddCardToDeck{" +
                "card=" + card.toString() +
                ", deck=" + deck.getComponentName() +
                ", index=" + index +
                '}';
    }
}

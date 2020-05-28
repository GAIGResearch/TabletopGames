package core.actions;

import core.AbstractGameState;
import core.components.Card;
import core.components.Deck;

import java.util.Objects;

@SuppressWarnings("unchecked")
public class RemoveCardWithCard extends DrawCard {
    private int deck;
    private int removeCard;

    public RemoveCardWithCard(int deckFrom, int deckTo, int fromIndex, int deckRemove, int cardRemove) {
        super(deckFrom, deckTo, fromIndex);
        this.deck = deckRemove;
        this.removeCard = cardRemove;
    }


    @Override
    public boolean execute(AbstractGameState gs) {
        ((Deck<Card>)gs.getComponentById(deck)).remove(removeCard); // card removed from the game

        // Discard other card from player hand
        return super.execute(gs);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        RemoveCardWithCard that = (RemoveCardWithCard) o;
        return deck == that.deck &&
                removeCard == that.removeCard;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), deck, removeCard);
    }

    @Override
    public String toString() {
        return "RemoveCardWithCard{" +
                "deck=" + deck +
                ", removeCard=" + removeCard +
                '}';
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "RemoveCardWithCard{" +
                "deck=" + deck +
                ", removeCard=" + removeCard +
                ", cardPlayed=" + getCard(gameState).getComponentName() +
                '}';
    }

    public int getRemoveCard() {
        return removeCard;
    }

    public int getDeck() {
        return deck;
    }
}

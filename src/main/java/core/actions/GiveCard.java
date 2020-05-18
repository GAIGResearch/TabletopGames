package core.actions;

import core.components.Card;
import core.AbstractGameState;

import java.util.Objects;

public class GiveCard implements IAction {

    private Card card;
    private int otherPlayer;

    public GiveCard(Card card, int otherPlayer) {
        this.card = card;
        this.otherPlayer = otherPlayer;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        //TODO
        return false;
    }


    @Override
    public boolean equals(Object other)
    {
        if (this == other) return true;
        if(other instanceof GiveCard)
        {
            GiveCard otherAction = (GiveCard) other;
            return card.equals(otherAction.card) && otherPlayer == otherAction.otherPlayer;

        }else return false;
    }

    @Override
    public String toString() {
        return "GiveCard{" +
                "card=" + card.toString() +
                ", otherPlayer=" + otherPlayer +
                '}';
    }

    public Card getCard() {
        return card;
    }

    public int getOtherPlayer() {
        return otherPlayer;
    }

    @Override
    public int hashCode() {
        return Objects.hash(card, otherPlayer);
    }
}

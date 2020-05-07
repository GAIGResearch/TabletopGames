package core.actions;

import core.components.Card;
import core.AbstractGameState;

public class TakeCard implements IAction {
    //TODO: this should be agreed between players

    private Card card;
    private int otherPlayer;

    public TakeCard(Card card, int otherPlayer) {
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
        if(other instanceof TakeCard)
        {
            TakeCard otherAction = (TakeCard) other;
            return card.equals(otherAction.card) && otherPlayer == otherAction.otherPlayer;

        }else return false;
    }
}

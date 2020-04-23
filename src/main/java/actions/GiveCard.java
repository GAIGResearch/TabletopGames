package actions;

import actions.Action;
import components.Card;
import core.GameState;

public class GiveCard implements Action {
    //TODO: this should be agreed between players

    private Card card;
    private int otherPlayer;

    public GiveCard(Card card, int otherPlayer) {
        this.card = card;
        this.otherPlayer = otherPlayer;
    }

    @Override
    public boolean execute(GameState gs) {
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
}

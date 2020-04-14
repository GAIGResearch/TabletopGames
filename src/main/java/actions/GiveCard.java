package actions;

import actions.Action;
import components.Card;
import core.GameState;

public class GiveCard implements Action {
    //TODO: this should be agreed between players

    Card card;
    int otherPlayer;

    public GiveCard(Card card, int otherPlayer) {
        this.card = card;
        this.otherPlayer = otherPlayer;
    }

    @Override
    public boolean execute(GameState gs) {
        //TODO
        return false;
    }
}

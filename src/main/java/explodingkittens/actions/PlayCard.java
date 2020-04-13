package explodingkittens.actions;

import actions.Action;
import components.Card;
import core.GameState;

public class PlayCard implements Action {

    Card card;
    int playerID;

    public PlayCard(int playerID, Card card){
        this.card = card;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(GameState gs) {
        boolean succes = gs.findDeck("Player" + this.playerID + "HandCards").remove(card);
        gs.findDeck("DiscardDeck").add(card);
        return succes;
    }
}

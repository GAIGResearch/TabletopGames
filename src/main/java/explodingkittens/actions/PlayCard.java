package explodingkittens.actions;

import actions.Action;
import components.Card;
import core.GameState;
import explodingkittens.ExplodingKittensGameState;

public class PlayCard implements Action {

    Card card;
    int playerID;

    public PlayCard(int playerID, Card card){
        this.card = card;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(GameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        boolean success = ekgs.getPlayerHand(playerID).remove(card);
        ekgs.getDiscardDeck().add(card);
        return success;
    }
}

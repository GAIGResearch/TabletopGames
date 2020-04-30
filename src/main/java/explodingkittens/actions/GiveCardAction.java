package explodingkittens.actions;

import actions.Action;
import components.Card;
import core.GameState;
import explodingkittens.ExplodingKittensCardTypeProperty;
import explodingkittens.ExplodingKittensGamePhase;
import explodingkittens.ExplodingKittensGameState;

public class GiveCardAction implements Action {

    Card card;
    int giverID;
    int receiverID;

    public GiveCardAction(Card card, int giverID, int receiverID) {
        this.card = card;
        this.receiverID = receiverID;
        this.giverID = giverID;
    }

    @Override
    public boolean execute(GameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        ekgs.getPlayerHand(giverID).remove(card);
        ekgs.getPlayerHand(receiverID).add(card);
        ekgs.gamePhase = ExplodingKittensGamePhase.PlayerMove;
        ekgs.playerAskingForFavorID = -1;
        ekgs.setActivePlayer(receiverID);
        return true;
    }

    @Override
    public String toString(){
        String cardtype = ((ExplodingKittensCardTypeProperty) card.getProperty(ExplodingKittensGameState.cardTypeHash)).value.toString();
        return String.format("Player %d gives %s Card to %d for a favor", giverID, cardtype, receiverID);
    }

}

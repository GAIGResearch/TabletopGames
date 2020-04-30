package explodingkittens.actions;

import components.Card;
import core.GameState;
import explodingkittens.ExplodingKittensGamePhase;
import explodingkittens.ExplodingKittensGameState;

public class PlaceExplodingKittenAction extends PlayCard {
    int targetIndex;

    public PlaceExplodingKittenAction(int playerID, Card card, int index) {
        super(playerID, card);
        this.targetIndex = index;
    }

    @Override
    public boolean execute(GameState gs) {
        ExplodingKittensGameState ekgs = (ExplodingKittensGameState) gs;
        boolean success = ekgs.getPlayerHand(this.playerID).remove(card);
        ekgs.getDrawDeck().add(card, targetIndex);
        ekgs.gamePhase = ExplodingKittensGamePhase.PlayerMove;
        int nextPlayer = ekgs.nextPlayerToDraw(playerID);
        if (nextPlayer != playerID)
            ekgs.remainingDraws = 1;
        else
            ekgs.remainingDraws -= 1;

        ekgs.setActivePlayer(nextPlayer);
        return success;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d defuses the kitten places it at index  %d", playerID, targetIndex);
    }
}

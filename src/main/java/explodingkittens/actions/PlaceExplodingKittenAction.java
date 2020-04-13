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
        boolean succes = gs.findDeck("Player" + this.playerID + "HandCards").remove(card);
        gs.findDeck("DrawDeck").add(card, targetIndex);
        ((ExplodingKittensGameState) gs).gamePhase = ExplodingKittensGamePhase.PlayerMove;
        int nextPlayer = ((ExplodingKittensGameState) gs).nextPlayerToDraw(playerID);
        if (nextPlayer != playerID)
            ((ExplodingKittensGameState) gs).remainingDraws = 1;
        else
            ((ExplodingKittensGameState) gs).remainingDraws -= 1;

        ((ExplodingKittensGameState) gs).setActivePlayer(nextPlayer);
        return succes;
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d defuses the kitten places it at index  %d", playerID, targetIndex);
    }
}

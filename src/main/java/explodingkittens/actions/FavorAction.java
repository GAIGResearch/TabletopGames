package explodingkittens.actions;

import components.Card;
import core.GameState;
import explodingkittens.ExplodingKittensGamePhase;
import explodingkittens.ExplodingKittensGameState;


public class FavorAction extends PlayCard implements IsNopeable{
    int target;

    public FavorAction(int playerID, Card card, int target) {
        super(playerID, card);
        this.target = target;
    }

    @Override
    public boolean execute(GameState gs) {
        super.execute(gs);
        ExplodingKittensGameState ekgs = ((ExplodingKittensGameState)gs);
        ekgs.gamePhase = ExplodingKittensGamePhase.FavorPhase;
        ekgs.playerAskingForFavorID = playerID;
        ekgs.setActivePlayer(target);
        return true;
    }

    public boolean nopedExecute(GameState gs) {
        return super.execute(gs);
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player %d asks Player %d for a favor", playerID, target);
    }

}

package games.explodingkittens.actions;

import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGamePhase;
import games.explodingkittens.ExplodingKittensGameState;
import core.turnorder.TurnOrder;


public class FavorAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    final int target;
    final int playerAskingForFavor;

    public FavorAction(T card, IDeck<T> playerDeck, IDeck<T> discardPile, int target, int playerID) {
        super(card, playerDeck, discardPile);
        this.target = target;
        this.playerAskingForFavor = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        ExplodingKittensGameState ekgs = ((ExplodingKittensGameState)gs);
        ekgs.gamePhase = ExplodingKittensGamePhase.FavorPhase;
        ekgs.playerGettingAFavor = playerAskingForFavor;

        ExplodingKittenTurnOrder ekto = (ExplodingKittenTurnOrder) gs.getTurnOrder();
        ekto.registerFavorAction(target);
        return true;
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player asks Player %d for a favor", target);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }

}

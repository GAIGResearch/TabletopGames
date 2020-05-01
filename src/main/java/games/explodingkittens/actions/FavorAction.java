package updated_core.games.explodingkittens.actions;

import components.Deck;
import components.IDeck;
import updated_core.actions.IPrintable;
import updated_core.games.explodingkittens.ExplodingKittenTurnOrder;
import updated_core.games.explodingkittens.ExplodingKittensGamePhase;
import updated_core.games.explodingkittens.ExplodingKittensGameState;
import updated_core.gamestates.AbstractGameState;
import updated_core.turn_order.TurnOrder;


public class FavorAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    final int target;
    final int playerAskingForFavor;

    public FavorAction(T card, IDeck<T> playerDeck, IDeck<T> discardPile, int target, int playerID) {
        super(card, playerDeck, discardPile);
        this.target = target;
        this.playerAskingForFavor = playerID;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        super.Execute(gs, turnOrder);

        ExplodingKittensGameState ekgs = ((ExplodingKittensGameState)gs);
        ekgs.gamePhase = ExplodingKittensGamePhase.FavorPhase;
        ekgs.playerGettingAFavor = playerAskingForFavor;

        ExplodingKittenTurnOrder ekto = (ExplodingKittenTurnOrder) turnOrder;
        ekto.registerFavorAction(target);
        return true;
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.Execute(gs, turnOrder);
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

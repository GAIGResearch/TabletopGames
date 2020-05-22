package games.explodingkittens.actions;

import core.components.Card;
import core.AbstractGameState;
import core.components.Component;
import core.components.Deck;
import core.observations.IPrintable;
import games.explodingkittens.ExplodingKittenTurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import core.turnorder.TurnOrder;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.Favor;

public class FavorAction<T extends Component> extends PlayCard<T> implements IsNopeable, IPrintable {
    final int target;
    final int playerAskingForFavor;

    public FavorAction(T card, Deck<T> playerDeck, Deck<T> discardPile, int target, int playerID) {
        super(card, playerDeck, discardPile);
        this.target = target;
        this.playerAskingForFavor = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        ExplodingKittensGameState ekgs = ((ExplodingKittensGameState)gs);
        ekgs.setGamePhase(Favor);
        ekgs.setPlayerGettingAFavor(playerAskingForFavor);

        ExplodingKittenTurnOrder ekto = (ExplodingKittenTurnOrder) gs.getTurnOrder();
        ekto.registerFavorAction(target);
        return true;
    }

    @Override
    public Card getCard() {
        return null;
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public String toString(){//overriding the toString() method
        return String.format("Player asks Player %d for a favor", target);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }

}

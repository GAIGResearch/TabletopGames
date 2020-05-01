package games.explodingkittens.actions;

import components.IDeck;
import core.AbstractGameState;
import observations.IPrintable;
import components.IPartialObservableDeck;
import games.explodingkittens.ExplodingKittensGamePhase;
import games.explodingkittens.ExplodingKittensGameState;
import turnorder.TurnOrder;

public class SeeTheFutureAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    private final IPartialObservableDeck<T> drawPile;
    private final int playerID;

    public SeeTheFutureAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck, int playerID, IPartialObservableDeck<T> drawPile) {
        super(card, playerDeck, discardDeck);
        this.drawPile = drawPile;
        this.playerID = playerID;
    }

    @Override
    public boolean Execute(AbstractGameState gs, TurnOrder turnOrder) {
        super.Execute(gs, turnOrder);
        ((ExplodingKittensGameState)gs).gamePhase = ExplodingKittensGamePhase.SeeTheFuturePhase;
        return false;
    }

    @Override
    public String toString() {//overriding the toString() method
        return "Player wants to see the future // Not Implemented Yet";
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.Execute(gs, turnOrder);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

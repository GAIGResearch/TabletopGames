package games.explodingkittens.actions;

import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import core.components.IPartialObservableDeck;
import games.explodingkittens.ExplodingKittensGamePhase;
import games.explodingkittens.ExplodingKittensGameState;
import core.turnorder.TurnOrder;

public class SeeTheFutureAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    private final IPartialObservableDeck<T> drawPile;
    private final int playerID;

    public SeeTheFutureAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck, int playerID, IPartialObservableDeck<T> drawPile) {
        super(card, playerDeck, discardDeck);
        this.drawPile = drawPile;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        ((ExplodingKittensGameState)gs).gamePhase = ExplodingKittensGamePhase.SeeTheFuturePhase;
        return false;
    }

    @Override
    public String toString() {//overriding the toString() method
        return "Player wants to see the future // Not Implemented Yet";
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public void PrintToConsole() {
        System.out.println(this.toString());
    }
}

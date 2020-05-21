package games.explodingkittens.actions;

import core.components.Card;
import core.components.Deck;
import core.components.IDeck;
import core.AbstractGameState;
import core.observations.IPrintable;
import core.turnorder.TurnOrder;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.SeeTheFuture;

public class SeeTheFutureAction<T> extends PlayCard<T> implements IsNopeable, IPrintable {
    private final Deck<T> drawPile;
    private final int playerID;

    public SeeTheFutureAction(T card, IDeck<T> playerDeck, IDeck<T> discardDeck, int playerID, Deck<T> drawPile) {
        super(card, playerDeck, discardDeck);
        this.drawPile = drawPile;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);
        gs.setGamePhase(SeeTheFuture);
        return false;
    }

    @Override
    public Card getCard() {
        return null;
    }

    @Override
    public String toString() {//overriding the toString() method
        return "Player wants to see the future // Not Implemented Yet";
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return super.execute(gs);
    }

    @Override
    public void printToConsole() {
        System.out.println(this.toString());
    }
}

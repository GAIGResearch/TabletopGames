package games.explodingkittens.actions;

import core.actions.DrawCard;
import core.AbstractGameState;
import core.observations.IPrintable;
import core.turnorder.TurnOrder;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.SeeTheFuture;

public class SeeTheFuture extends DrawCard implements IsNopeable, IPrintable {

    public SeeTheFuture(int deckFrom, int deckTo, int fromIndex) {
        super(deckFrom, deckTo, fromIndex);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        super.execute(gs);

        gs.setGamePhase(SeeTheFuture);
        return true;
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

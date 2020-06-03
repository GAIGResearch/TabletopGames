package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import core.turnorders.TurnOrder;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.SeeTheFuture;

public class SeeTheFuture extends AbstractAction implements IsNopeable, IPrintable {

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setGamePhase(SeeTheFuture);
        return true;
    }

    @Override
    public String toString() {//overriding the toString() method
        return "Player wants to see the future";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    public boolean nopedExecute(AbstractGameState gs, TurnOrder turnOrder) {
        return true; //super.execute(gs);
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new SeeTheFuture();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SeeTheFuture;
    }

    @Override
    public int hashCode() {
        return 0;
    }
}

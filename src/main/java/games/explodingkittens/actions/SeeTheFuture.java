package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.interfaces.IPrintable;
import core.turnorders.TurnOrder;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.SeeTheFuture;

public class SeeTheFuture extends AbstractAction implements IsNopeable, IPrintable {

    int playerID;

    public SeeTheFuture(int playerID) {
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setGamePhase(SeeTheFuture);
        gs.getTurnOrder().setTurnOwner(playerID);
        return true;
    }

    @Override
    public String toString() {//overriding the toString() method
        return "Player" + playerID + "wants to see the future";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "See the future";
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
        return new SeeTheFuture(playerID);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SeeTheFuture && ((SeeTheFuture) obj).playerID == playerID;
    }

    @Override
    public int hashCode() {
        return playerID;
    }
}

package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import core.turnorders.TurnOrder;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensParameters;
import games.explodingkittens.cards.ExplodingKittensCard;

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

        ExplodingKittensGameState ekgs = ((ExplodingKittensGameState)gs);
        PartialObservableDeck<ExplodingKittensCard> drawPile = ekgs.getDrawPile();

        // make the first three cards visible since the player needs to know what they are to choose their order
        int n = Math.min(((ExplodingKittensParameters) ekgs.getGameParameters()).nSeeFutureCards, drawPile.getSize());
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < ekgs.getNPlayers(); j++){
                // this player knows the order
                drawPile.setVisibilityOfComponent(i, j, j == playerID);        // other players don't know the order anymore
            }
        }

        return true;
    }

    @Override
    public String toString() {//overriding the toString() method
        return "Player " + playerID + " wants to see the future";
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

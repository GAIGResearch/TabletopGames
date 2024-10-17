package games.explodingkittens.actions;

import core.actions.AbstractAction;
import core.AbstractGameState;
import core.actions.DrawCard;
import core.components.PartialObservableDeck;
import core.interfaces.IPrintable;
import games.explodingkittens.ExplodingKittensGameState;
import games.explodingkittens.ExplodingKittensParameters;
import games.explodingkittens.cards.ExplodingKittensCard;

import java.util.Arrays;

import static games.explodingkittens.ExplodingKittensGameState.ExplodingKittensGamePhase.SeeTheFuture;

public class SeeTheFuture extends DrawCard implements IsNopeable, IPrintable {

    int playerID;

    public SeeTheFuture(int deckFrom, int deckTo, int fromIndex, int playerID) {
        super(deckFrom, deckTo, fromIndex);
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setGamePhase(SeeTheFuture);
        gs.setTurnOwner(playerID);

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

    @Override
    public void nopedExecute(AbstractGameState gs) {
        super.execute(gs);
    }

    @Override
    public void actionPlayed(AbstractGameState gs) {
        // Mark card as visible in the player's deck to all other players
        PartialObservableDeck<ExplodingKittensCard> from = (PartialObservableDeck<ExplodingKittensCard>) gs.getComponentById(deckFrom);
        boolean[] vis = new boolean[gs.getNPlayers()];
        Arrays.fill(vis, true);
        from.setVisibilityOfComponent(fromIndex, vis);
    }

    @Override
    public void printToConsole(AbstractGameState gameState) {
        System.out.println(this.toString());
    }

    @Override
    public AbstractAction copy() {
        return new SeeTheFuture(deckFrom, deckTo, fromIndex, playerID);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SeeTheFuture && ((SeeTheFuture) obj).playerID == playerID && super.equals(obj);
    }

    @Override
    public int hashCode() {
        return playerID;
    }
}

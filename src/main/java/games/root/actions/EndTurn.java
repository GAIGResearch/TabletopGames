package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;

import java.util.Objects;

public class EndTurn extends AbstractAction {
    public final int playerID;
    public final boolean setup;

    public EndTurn(int playerID, boolean setup) {
        this.playerID = playerID;
        this.setup = setup;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        //Handled in forward model;
        RootGameState currentState = (RootGameState) gs;
        if (playerID == currentState.getCurrentPlayer()) {
            if (currentState.getGamePhase() == RootGameState.RootGamePhase.Setup) {
                if(setup){
                    currentState.incrementPlayersSetUp();
                }
                return true;
            } else if (currentState.getGamePhase() == RootGameState.RootGamePhase.Evening) {
                return true;
            } else {
                throw new RuntimeException("Trying to end turn in incorrectGamePhase");
            }
        }
        return false;
    }

    @Override
    public EndTurn copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EndTurn endTurn = (EndTurn) o;
        return playerID == endTurn.playerID && setup == endTurn.setup;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, setup);
    }

    @Override
    public String toString() {
        return "p" + playerID + " ended turn";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " ended turn";
    }
}

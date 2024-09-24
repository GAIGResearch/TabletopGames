package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;

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
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, "EndingTurn");
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " ended turn";
    }
}

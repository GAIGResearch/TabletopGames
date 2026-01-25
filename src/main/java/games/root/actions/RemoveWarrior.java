package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class RemoveWarrior extends AbstractAction {
    public final int playerID;
    public final int locationID;

    public RemoveWarrior(int playerID, int locationID){
        this.playerID = playerID;
        this.locationID = locationID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) != RootParameters.Factions.Vagabond){
            RootBoardNodeWithRootEdges location = currentState.getGameMap().getNodeByID(locationID);
            if (location.getWarrior(currentState.getPlayerFaction(playerID))> 0){
                location.removeWarrior(currentState.getPlayerFaction(playerID));
                currentState.addWarrior(currentState.getPlayerFaction(playerID));
                return true;
            }
        }
        return false;
    }

    @Override
    public RemoveWarrior copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){
            return true;
        }
        if (obj instanceof RemoveWarrior rw){
            return playerID == rw.playerID && locationID == rw.locationID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("RemoveWarrior", playerID, locationID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " removes a warrior from " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " removes a warrior from " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

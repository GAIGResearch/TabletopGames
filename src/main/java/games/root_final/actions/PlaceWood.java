package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.pandemic.actions.AddResearchStationFrom;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class PlaceWood extends AbstractAction {
    public int playerID;
    public int locationID;

    public PlaceWood(int playerID, int locationID) {
        this.playerID = playerID;
        this.locationID = locationID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat) {
            currentState.getGameMap().getNodeByID(locationID).addWood();
            currentState.removeWood();
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlaceWood(playerID, locationID);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj instanceof PlaceWood) {
            PlaceWood otherAction = (PlaceWood) obj;
            return playerID == otherAction.playerID && locationID == otherAction.locationID;

        } else return false;

    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " places wood at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

import java.util.Objects;

public class PlaceWood extends AbstractAction {
    public final int playerID;
    public final int locationID;

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
    public PlaceWood copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaceWood placeWood = (PlaceWood) o;
        return playerID == placeWood.playerID && locationID == placeWood.locationID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " places wood at " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " places wood at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

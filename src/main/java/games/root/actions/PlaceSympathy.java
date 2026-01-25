package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

import java.util.Objects;

public class PlaceSympathy extends AbstractAction {
    public final int playerID;
    public final int locationID;

    public PlaceSympathy(int playerID, int locationID){
        this.playerID = playerID;
        this.locationID = locationID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        if(playerID == state.getCurrentPlayer() && state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            state.addGameScorePlayer(playerID, rp.sympathyPoints.get(state.getTokenCount(RootParameters.TokenType.Sympathy)));
            state.getGameMap().getNodeByID(state.getGameMap().getNodeByID(locationID).getComponentID()).setSympathy();
            state.removeSympathyTokens();
            return true;
        }
        return false;
    }

    @Override
    public PlaceSympathy copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaceSympathy that = (PlaceSympathy) o;
        return playerID == that.playerID && locationID == that.locationID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " places sympathy token at " + locationID;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " places sympathy token at location " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

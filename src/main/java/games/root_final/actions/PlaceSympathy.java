package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class PlaceSympathy extends AbstractAction {
    public final int playerID;
    public RootBoardNodeWithRootEdges location;

    public PlaceSympathy(int playerID, RootBoardNodeWithRootEdges location){
        this.playerID = playerID;
        this.location = location;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState state = (RootGameState) gs;
        RootParameters rp = (RootParameters) gs.getGameParameters();
        if(playerID == state.getCurrentPlayer() && state.getPlayerFaction(playerID) == RootParameters.Factions.WoodlandAlliance){
            state.addGameScorePLayer(playerID, rp.sympathyPoints.get(state.getTokenCount(RootParameters.TokenType.Sympathy)));
            state.getGameMap().getNodeByID(location.getComponentID()).setSympathy();
            state.removeSympathyTokens();
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new PlaceSympathy(playerID, location);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof PlaceSympathy){
            PlaceSympathy other = (PlaceSympathy) obj;
            return playerID == other.playerID && location.getComponentID()==other.location.getComponentID();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("PlaceSympathy", playerID, location.hashCode());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " places sympathy token at location " + location.identifier;
    }
}

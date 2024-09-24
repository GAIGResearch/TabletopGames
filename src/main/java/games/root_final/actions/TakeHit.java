package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class TakeHit extends AbstractAction {
    public final int playerID;
    public final int locationID;
    public final RootParameters.BuildingType buildingType;
    public final RootParameters.TokenType tokenType;
    public TakeHit(int playerID, int location, RootParameters.BuildingType buildingType, RootParameters.TokenType tokenType){
        this.playerID = playerID;
        this.buildingType = buildingType;
        this.tokenType = tokenType;
        this.locationID = location;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if(currentState.getCurrentPlayer() == playerID){
            if(buildingType != null){
                if(currentState.getGameMap().getNodeByID(locationID).hasBuilding(buildingType)) {
                    currentState.getGameMap().getNodeByID(locationID).removeBuilding(buildingType);
                    currentState.addBuilding(buildingType);
                    return true;
                }else{
                    return false;
                }

            } else if (tokenType != null) {
                if(currentState.getGameMap().getNodeByID(locationID).hasToken(tokenType)){
                    currentState.getGameMap().getNodeByID(locationID).removeToken(tokenType);
                    currentState.addToken(tokenType);
                    return true;
                }else{
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new TakeHit(playerID, locationID, buildingType, tokenType);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if(obj instanceof TakeHit){
            TakeHit other = (TakeHit) obj;
            return playerID == other.playerID && locationID == other.locationID && buildingType == other.buildingType && tokenType == other.tokenType;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("TakeHit", playerID, locationID, buildingType, tokenType);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        if(buildingType != null) {
            RootGameState gs = (RootGameState) gameState;
            return gs.getPlayerFaction(playerID).toString()  + " chooses to remove " + buildingType + " at " + gs.getGameMap().getNodeByID(locationID).identifier;
        } else if (tokenType != null) {
            RootGameState gs = (RootGameState) gameState;
            return gs.getPlayerFaction(playerID).toString()  + " chooses to remove " + tokenType + " at " + gs.getGameMap().getNodeByID(locationID).identifier;
        }
        return "";
    }
}

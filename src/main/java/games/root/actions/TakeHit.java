package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;

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
    public TakeHit copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TakeHit takeHit = (TakeHit) o;
        return playerID == takeHit.playerID && locationID == takeHit.locationID && buildingType == takeHit.buildingType && tokenType == takeHit.tokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationID, buildingType, tokenType);
    }

    @Override
    public String toString() {
        return "p" + playerID + " chooses to remove " + (buildingType != null? buildingType.toString() : tokenType.toString()) + " at " + locationID;
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

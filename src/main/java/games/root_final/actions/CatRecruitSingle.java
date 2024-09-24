package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class CatRecruitSingle extends AbstractAction {
    public final int playerID;
    public final int locationID;

    public CatRecruitSingle(int playerID, int locationID){
        this.playerID = playerID;
        this.locationID = locationID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            RootBoardNodeWithRootEdges clearing = currentState.getGameMap().getNodeByID(locationID);
            if (currentState.getCatWarriors() > 0){
                clearing.addWarrior(RootParameters.Factions.MarquiseDeCat);
                currentState.removeCatWarrior();
                return true;
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
        if (obj == this){return true;}
        if (obj instanceof CatRecruitSingle c){
            return playerID == c.playerID && locationID == c.locationID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("CatRecruitSingle", playerID, locationID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " recruits 1 warrior at " + gs.getGameMap().getNodeByID(locationID).identifier;
    }
}

package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.Objects;

public class PlaceWoodEverywhere extends AbstractAction {
    public final int playerID;

    public PlaceWoodEverywhere(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            for (RootBoardNodeWithRootEdges node: currentState.getGameMap().getNonForrestBoardNodes()){
                for (int i = 0; i < node.getSawmill(); i++){
                    node.addWood();
                    currentState.removeWood();
                }
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
        if(obj == this){return true;}
        if(obj instanceof PlaceWoodEverywhere){
            PlaceWoodEverywhere other = (PlaceWoodEverywhere) obj;
            return playerID == other.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("PlaceWoodEverywhere", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " has enough wood to place one at each sawmill";
    }
}

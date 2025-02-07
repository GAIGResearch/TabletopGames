package games.root.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.components.RootBoardNodeWithRootEdges;

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
    public PlaceWoodEverywhere copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlaceWoodEverywhere that = (PlaceWoodEverywhere) o;
        return playerID == that.playerID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID);
    }

    @Override
    public String toString() {
        return "p" + playerID + " has enough wood to place one at each sawmill";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " has enough wood to place one at each sawmill";
    }
}

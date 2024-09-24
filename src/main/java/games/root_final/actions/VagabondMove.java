package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.Item;
import games.root_final.components.RootBoardNodeWithRootEdges;
import scala.collection.parallel.ParIterableLike;

import java.util.Objects;

public class VagabondMove extends AbstractAction {
    public final int playerID;
    public final int targetNodeID;
    public VagabondMove(int playerID, int targetNodeID){
        this.playerID = playerID;
        this.targetNodeID = targetNodeID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.Vagabond){
            RootBoardNodeWithRootEdges currentNode = currentState.getGameMap().getVagabondClearing();
            RootBoardNodeWithRootEdges targetNode = currentState.getGameMap().getNodeByID(targetNodeID);
            if (targetNode.getNeighbours().contains(currentNode) && targetNode.getClearingType() != RootParameters.ClearingTypes.Forrest){
                for (Item item: currentState.getSachel()){
                    if (item.itemType == Item.ItemType.boot && item.refreshed && !item.damaged){
                        item.refreshed = false;
                        currentNode.removeWarrior(RootParameters.Factions.Vagabond);
                        targetNode.addWarrior(RootParameters.Factions.Vagabond);
                        currentState.increaseActionsPlayed();
                        //if the a
                        if ((currentState.getRelationship(RootParameters.Factions.MarquiseDeCat) == RootParameters.Relationship.Hostile && targetNode.getWarrior(RootParameters.Factions.MarquiseDeCat) > 0) || (currentState.getRelationship(RootParameters.Factions.WoodlandAlliance) == RootParameters.Relationship.Hostile && targetNode.getWarrior(RootParameters.Factions.WoodlandAlliance) > 0) || (currentState.getRelationship(RootParameters.Factions.EyrieDynasties) == RootParameters.Relationship.Hostile && targetNode.getWarrior(RootParameters.Factions.EyrieDynasties) > 0)){
                            for (Item item1: currentState.getSachel()){
                                if (item1.itemType == Item.ItemType.boot && item1.refreshed && !item1.damaged){
                                    item1.refreshed = false;
                                    return true;
                                }
                            }
                            return false;
                        }
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this; //immutable
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == this){return true;}
        if (obj instanceof VagabondMove vm){
            return playerID == vm.playerID && targetNodeID == vm.targetNodeID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("VagabondMove", playerID, targetNodeID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " moves to " + gs.getGameMap().getNodeByID(targetNodeID).identifier;
    }
}

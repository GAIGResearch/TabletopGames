package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class PlaceWoodSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public List<Integer> locationIDs;

    public boolean canPlaceEverywhere;

    public boolean done = false;

    public PlaceWoodSequence(int playerID){
        this.playerID = playerID;
        locationIDs = new ArrayList<>();
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            int sawmills = 0;
            for (RootBoardNodeWithRootEdges node: currentState.getGameMap().getNonForrestBoardNodes()){
                if(node.getSawmill() > 0){
                    for (int i = 0; i < node.getSawmill(); i++){
                        sawmills++;
                        locationIDs.add(node.getComponentID());
                    }
                }
            }
            if (currentState.getWood() >= sawmills){
                canPlaceEverywhere = true;
            }else {
                canPlaceEverywhere = false;
            }
            currentState.increaseActionsPlayed();
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gs = (RootGameState) state;
        List<AbstractAction> actions = new ArrayList<>();
        if(canPlaceEverywhere){
            actions.add(new PlaceWoodEverywhere(playerID));
            return actions;
        }else {
            for (RootBoardNodeWithRootEdges node: gs.getGameMap().getNonForrestBoardNodes()){
                if (node.hasBuilding(RootParameters.BuildingType.Sawmill) && locationIDs.contains(node.getComponentID()) && gs.getWood() > 0){
                    actions.add(new PlaceWood(playerID, node.getComponentID()));
                }
            }
            if (actions.isEmpty()){
                actions.add(new Pass(playerID, " No more wood tokens"));
            }
            return actions;
        }
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        if (action instanceof PlaceWoodEverywhere){
            done = true;
        } else if (action instanceof  PlaceWood p) {
            for (Integer i : locationIDs){
                if (i == p.locationID){
                    locationIDs.remove(i);
                    break;
                }
            }
            if (locationIDs.isEmpty()){
                done = true;
            }
        } else if (action instanceof Pass) {
            done = true;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public PlaceWoodSequence copy() {
        PlaceWoodSequence copy = new PlaceWoodSequence(playerID);
        copy.done = done;
        copy.canPlaceEverywhere = canPlaceEverywhere;
        copy.locationIDs.addAll(locationIDs);
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof PlaceWoodSequence){
            PlaceWoodSequence other = (PlaceWoodSequence) obj;
            return playerID == other.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("PlaceWoodSequence", playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " starts placing wood";
    }
}

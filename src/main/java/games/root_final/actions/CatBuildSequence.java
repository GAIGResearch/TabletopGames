package games.root_final.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root_final.RootGameState;
import games.root_final.RootParameters;
import games.root_final.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CatBuildSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public enum Stage{
        selectBuildingType,
        selectLocation,
        removeWood,
        build,
    }
    public Stage stage = Stage.selectBuildingType;
    public RootParameters.BuildingType bt;
    public int locationID;
    public int cost;
    public boolean done = false;

    public CatBuildSequence(int playerID){
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (playerID == currentState.getCurrentPlayer() && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            currentState.setActionInProgress(this);
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gameState = (RootGameState) state;
        RootParameters rp = (RootParameters) state.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        switch (stage){
            case selectBuildingType:
                int sawmillCost = rp.getCatBuildingCost(gameState.getBuildingCount(RootParameters.BuildingType.Sawmill));
                if (gameState.canBuildSpecificCatBuilding(playerID, sawmillCost)){
                    actions.add(new ChooseCatBuilding(playerID, RootParameters.BuildingType.Sawmill, sawmillCost));
                }
                int workshopCost = rp.getCatBuildingCost(gameState.getBuildingCount(RootParameters.BuildingType.Workshop));
                if (gameState.canBuildSpecificCatBuilding(playerID, workshopCost)){
                    actions.add(new ChooseCatBuilding(playerID, RootParameters.BuildingType.Workshop, workshopCost));
                }
                int recruiterCost = rp.getCatBuildingCost(gameState.getBuildingCount(RootParameters.BuildingType.Recruiter));
                if (gameState.canBuildSpecificCatBuilding(playerID, recruiterCost)){
                    actions.add(new ChooseCatBuilding(playerID, RootParameters.BuildingType.Recruiter, recruiterCost));
                }
                return actions;
            case selectLocation:
                for (RootBoardNodeWithRootEdges clearing: gameState.getGameMap().getNonForrestBoardNodes()){
                    if (clearing.rulerID == playerID && clearing.hasBuildingRoom() && gameState.hasEnoughAvailableWood(playerID, clearing.getComponentID(), cost)){
                        actions.add(new ChooseNode(playerID, clearing.getComponentID()));
                    }
                }
                return actions;
            case removeWood:
                if (cost > 0){
                    for (RootBoardNodeWithRootEdges clearing: gameState.getGameMap().getNonForrestBoardNodes()){
                        if (clearing.hasToken(RootParameters.TokenType.Wood) && gameState.isConnected(playerID, clearing.getComponentID(), locationID)){
                            actions.add(new TakeHit(playerID, clearing.getComponentID(), null, RootParameters.TokenType.Wood));
                        }
                    }
                }else {
                    actions.add(new Pass(playerID, "No building cost"));
                }
                return actions;
            case build:
                actions.add(new Build(locationID, playerID, bt, false));
                return actions;
        }
        return null;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        RootGameState gs = (RootGameState) state;
        RootParameters rp = (RootParameters) state.getGameParameters();
        if (action instanceof ChooseCatBuilding b){
            bt = b.bt;
            cost = b.cost;
            stage = Stage.selectLocation;
        } else if (action instanceof ChooseNode c){
            locationID = c.nodeID;
            stage = Stage.removeWood;
        } else if (action instanceof TakeHit t){
            cost--;
            if (cost == 0){
                stage = Stage.build;
            }
        } else if (action instanceof RemoveAllWood r){
            cost = 0;
            stage = Stage.build;
        } else if (action instanceof  Build b) {
            done = true;
        } else if (stage == Stage.removeWood && action instanceof Pass){
            stage = Stage.build;
        }
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return done;
    }

    @Override
    public CatBuildSequence copy() {
        CatBuildSequence copy = new CatBuildSequence(playerID);
        copy.stage = stage;
        copy.bt = bt;
        copy.cost = cost;
        copy.done = done;
        copy.locationID = locationID;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this){return true;}
        if (obj instanceof CatBuildSequence cb){
            return playerID == cb.playerID && stage == cb.stage && cost == cb.cost && bt == cb.bt && done == cb.done && locationID == cb.locationID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash("BuildSequence", playerID, stage, cost, bt, done, locationID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to build";
    }
}

package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.Build;
import games.root.actions.Pass;
import games.root.actions.RemoveAllWood;
import games.root.actions.TakeHit;
import games.root.actions.choosers.ChooseCatBuilding;
import games.root.actions.choosers.ChooseNode;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CatBuildSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    public enum Stage {
        SelectBuildingType,
        SelectLocation,
        RemoveWood,
        Build,
    }

    Stage stage = Stage.SelectBuildingType;
    RootParameters.BuildingType bt;
    int locationID;
    int cost;
    boolean done = false;

    public CatBuildSequence(int playerID){
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (playerID == currentState.getCurrentPlayer() && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            currentState.setActionInProgress(this);
            return true;
        }
        return false;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        RootGameState gameState = (RootGameState) state;
        RootParameters rp = (RootParameters) state.getGameParameters();
        List<AbstractAction> actions = new ArrayList<>();
        switch (stage){
            case SelectBuildingType:
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
            case SelectLocation:
                for (RootBoardNodeWithRootEdges clearing: gameState.getGameMap().getNonForrestBoardNodes()){
                    if (clearing.rulerID == playerID && clearing.hasBuildingRoom() && gameState.hasEnoughAvailableWood(playerID, clearing.getComponentID(), cost)){
                        actions.add(new ChooseNode(playerID, clearing.getComponentID()));
                    }
                }
                return actions;
            case RemoveWood:
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
            case Build:
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
        if (action instanceof ChooseCatBuilding b){
            bt = b.bt;
            cost = b.cost;
            stage = Stage.SelectLocation;
        } else if (action instanceof ChooseNode c){
            locationID = c.nodeID;
            stage = Stage.RemoveWood;
        } else if (action instanceof TakeHit t){
            cost--;
            if (cost == 0){
                stage = Stage.Build;
            }
        } else if (action instanceof RemoveAllWood r){
            cost = 0;
            stage = Stage.Build;
        } else if (action instanceof  Build b) {
            done = true;
        } else if (stage == Stage.RemoveWood && action instanceof Pass){
            stage = Stage.Build;
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
    public boolean equals(Object o) {
        if (!(o instanceof CatBuildSequence that)) return false;
        return playerID == that.playerID && locationID == that.locationID && cost == that.cost && done == that.done && stage == that.stage && bt == that.bt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, stage, bt, locationID, cost, done);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to build";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to build";
    }
}

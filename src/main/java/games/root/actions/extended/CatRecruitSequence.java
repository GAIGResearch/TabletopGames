package games.root.actions.extended;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.root.RootGameState;
import games.root.RootParameters;
import games.root.actions.CatRecruit;
import games.root.actions.CatRecruitSingle;
import games.root.actions.Pass;
import games.root.components.RootBoardNodeWithRootEdges;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CatRecruitSequence extends AbstractAction implements IExtendedSequence {
    public final int playerID;

    List<Integer> locationIDs;
    boolean canRecruitEverywhere;
    boolean done = false;

    public CatRecruitSequence(int playerID){
        this.playerID = playerID;
        locationIDs = new ArrayList<>();
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RootGameState currentState = (RootGameState) gs;
        if (currentState.getCurrentPlayer() == playerID && currentState.getPlayerFaction(playerID) == RootParameters.Factions.MarquiseDeCat){
            int recruiters = 0;
            for (RootBoardNodeWithRootEdges clearing: currentState.getGameMap().getNonForrestBoardNodes()){
                if (clearing.getRecruiters() > 0){
                    for (int i = 0; i < clearing.getRecruiters(); i++){
                        recruiters++;
                        locationIDs.add(clearing.getComponentID());
                    }
                }
            }
            canRecruitEverywhere = currentState.getCatWarriors() >= recruiters;
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
        if(canRecruitEverywhere){
            actions.add(new CatRecruit(playerID));
        } else {
            for (RootBoardNodeWithRootEdges clearing: gs.getGameMap().getNonForrestBoardNodes()){
                if (clearing.hasBuilding(RootParameters.BuildingType.Recruiter) && locationIDs.contains(clearing.getComponentID()) && gs.getCatWarriors() > 0){
                    actions.add(new CatRecruitSingle(playerID, clearing.getComponentID()));
                }
            }
            if (actions.isEmpty()){
                actions.add(new Pass(playerID, " No more warriors to place"));
            }
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof CatRecruit){
            done = true;
        } else if (action instanceof CatRecruitSingle cs) {
            for (Integer id: locationIDs){
                if (id == cs.locationID){
                    locationIDs.remove(id);
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
    public CatRecruitSequence copy() {
        CatRecruitSequence copy = new CatRecruitSequence(playerID);
        copy.done = done;
        copy.canRecruitEverywhere = canRecruitEverywhere;
        copy.locationIDs.addAll(locationIDs);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CatRecruitSequence that)) return false;
        return playerID == that.playerID && canRecruitEverywhere == that.canRecruitEverywhere && done == that.done && Objects.equals(locationIDs, that.locationIDs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, locationIDs, canRecruitEverywhere, done);
    }

    @Override
    public String toString() {
        return "p" + playerID + " wants to recruit.";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        RootGameState gs = (RootGameState) gameState;
        return gs.getPlayerFaction(playerID).toString() + " wants to recruit";
    }
}

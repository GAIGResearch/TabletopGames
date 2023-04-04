package games.catan.actions.dev;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;
import games.catan.CatanGameState;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static games.catan.CatanActionFactory.getBuyRoadActions;

public class DeepRoadBuilding extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final AbstractAction road;  // First road to build
    public final int nSteps;

    int nStepsTaken = 1;

    public DeepRoadBuilding(int playerID, AbstractAction road, int nSteps) {
        this.playerID = playerID;
        this.road = road;
        this.nSteps = nSteps;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Execute first step
        new PlayRoadBuilding(playerID, new AbstractAction[]{road}).execute(gs);

        // Set in progress for the rest of the roads
        if (nSteps != nStepsTaken) {
            return gs.setActionInProgress(this);
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        List<AbstractAction> roads = getBuyRoadActions((CatanGameState) state, playerID, true);
        for (AbstractAction road: roads) {
            actions.add(new PlayRoadBuilding(playerID, new AbstractAction[]{road}));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void registerActionTaken(AbstractGameState state, AbstractAction action) {
        nStepsTaken ++;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return nStepsTaken == nSteps;
    }

    @Override
    public DeepRoadBuilding copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepRoadBuilding)) return false;
        DeepRoadBuilding that = (DeepRoadBuilding) o;
        return playerID == that.playerID && nSteps == that.nSteps && nStepsTaken == that.nStepsTaken && Objects.equals(road, that.road);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, road, nSteps, nStepsTaken);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return playerID + " road building (" + road.toString() + ")";
    }
}

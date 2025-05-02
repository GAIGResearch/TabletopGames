package games.catan.actions.dev;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.catan.CatanGameState;
import games.catan.CatanParameters;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DeepYearOfPlenty extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final CatanParameters.Resource resource;  // First resource to take
    public final int nSteps;

    int nStepsTaken = 1;

    public DeepYearOfPlenty(int playerID, CatanParameters.Resource resource, int nSteps) {
        this.playerID = playerID;
        this.resource = resource;
        this.nSteps = nSteps;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Execute first step
        new PlayYearOfPlenty(new CatanParameters.Resource[]{resource}, playerID, true).execute(gs);

        // Set in progress for the rest of the resources
        if (nSteps != nStepsTaken && _computeAvailableActions(gs).size() > 0) {
            gs.setActionInProgress(this);
        } else {
            nStepsTaken = nSteps;
        }
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState) state;
        for (CatanParameters.Resource res: CatanParameters.Resource.values()) {
            if (res != CatanParameters.Resource.WILD && gs.getResourcePool().get(res).getValue() > 0)
                actions.add(new PlayYearOfPlenty(new CatanParameters.Resource[]{res}, playerID, false));
        }
        return actions;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return playerID;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        if (action instanceof DoNothing) nStepsTaken = nSteps;
        else nStepsTaken ++;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return nStepsTaken == nSteps;
    }

    @Override
    public DeepYearOfPlenty copy() {
        DeepYearOfPlenty copy = new DeepYearOfPlenty(playerID, resource, nSteps);
        copy.nStepsTaken = nStepsTaken;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeepYearOfPlenty)) return false;
        DeepYearOfPlenty that = (DeepYearOfPlenty) o;
        return playerID == that.playerID && nSteps == that.nSteps && nStepsTaken == that.nStepsTaken && resource == that.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, resource, nSteps, nStepsTaken);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "p" + playerID + " Dev:YearOfPlenty (" + resource + ")";
    }
}

package games.catan.actions.dev;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.catan.CatanActionFactory;
import games.catan.CatanGameState;
import games.catan.components.CatanCard;

import java.util.List;
import java.util.Objects;

public class PlayDevCard extends AbstractAction implements IExtendedSequence {
    public final int playerID;
    public final CatanCard.CardType type;
    public final int nSteps;  // Number of steps needed for the action to complete, depending on its type and game parameters settings

    int nStepsTaken = 1;

    public PlayDevCard(int playerID, CatanCard.CardType type, int nSteps) {
        this.playerID = playerID;
        this.type = type;
        this.nSteps = nSteps;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return _computeAvailableActions(state, state.getCoreGameParameters().actionSpace);
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state, ActionSpace actionSpace) {
        List<AbstractAction> actions = CatanActionFactory.getDevCardActions((CatanGameState) state, actionSpace, playerID, type);
        if (actions.size() == 0) {
            // Can't actually do anything useful, just do nothing
            actions.add(new DoNothing());
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
        else nStepsTaken++;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return nSteps == nStepsTaken;
    }

    @Override
    public PlayDevCard copy() {
        PlayDevCard copy = new PlayDevCard(playerID, type, nSteps);
        copy.nStepsTaken = nStepsTaken;
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PlayDevCard)) return false;
        PlayDevCard that = (PlayDevCard) o;
        return playerID == that.playerID && nSteps == that.nSteps && nStepsTaken == that.nStepsTaken && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerID, type, nSteps, nStepsTaken);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "p" + playerID + " plays Dev:" + type;
    }
}

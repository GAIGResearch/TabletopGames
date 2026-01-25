package games.puertorico.roles;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.actions.SelectRole;

import javax.annotation.OverridingMethodsMustInvokeSuper;
import java.util.*;

public abstract class PuertoRicoRole<T extends PuertoRicoRole<T>> implements IExtendedSequence {

    public final int roleOwner;
    protected boolean[] hasFinished;
    protected int currentPlayer;
    public final PuertoRicoConstants.Role roleType;

    public PuertoRicoRole(PuertoRicoGameState state, PuertoRicoConstants.Role roleType) {
        this.roleOwner = state.getCurrentPlayer();
        currentPlayer = roleOwner;
        hasFinished = new boolean[state.getNPlayers()];
        this.roleType = roleType;
        if (!roleType.allPlayers) {  // only the roleOwner takes an action
            for (int i = 0; i < hasFinished.length; i++) {
                if (i != roleOwner) {
                    hasFinished[i] = true;
                }
            }
        }
    }

    protected PuertoRicoRole(T toCopy) {
        this.roleOwner = toCopy.roleOwner;
        this.currentPlayer = toCopy.currentPlayer;
        this.hasFinished = toCopy.hasFinished.clone();
        this.roleType = toCopy.roleType;
    }

    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return currentPlayer;
    }

    @Override
    public void _afterAction(AbstractGameState gs, AbstractAction action) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        if (action instanceof SelectRole) {
            // this is the one we ignore, as it puts us into this role
            return;
        }
        if (action instanceof DoNothing) {
            // any time a player does nothing, they are done
            hasFinished[currentPlayer] = true;
        }

        // if the rotationType is MULTIPLE_ACTIONS, then we move the player on iff the current one has finished
        if (roleType.rotationType == PuertoRicoConstants.ActionRotation.MULTIPLE_ACTIONS && !(_computeAvailableActions(state).get(0) instanceof DoNothing)) {
            // we stay with the same player if they have actions left
        } else {
            // we move on to the next one
            if (roleType.rotationType == PuertoRicoConstants.ActionRotation.SINGLE_ACTION) {
                // if this is a once-round role, then a Single Action is all we get
                hasFinished[currentPlayer] = true;
            }
            // we move the currentPlayer on by one, until we find a player who has not finished
            // if all players have finished, then we have completed this action
            setNextPlayerWithAvailableAction(state, (currentPlayer + 1) % state.getNPlayers());
        }

        if (executionComplete(state)) {
            // if we get here, then all players have finished
            state.setCurrentRole(null);
            postPhaseProcessing(state);
        }
    }

    @Override
    public void afterRemovalFromQueue(AbstractGameState state, IExtendedSequence completedSequence) {
        throw new AssertionError("Should not be reachable");
    }

    private void setNextPlayerWithAvailableAction(PuertoRicoGameState state, int fromPlayer) {
        // we move the currentPlayer on by one, until we find a player who has not finished
        // if all players have finished, then we have completed this action
        for (int i = 0; i < state.getNPlayers(); i++) {
            int nextPlayer = (fromPlayer + i) % state.getNPlayers();
            if (!hasFinished[nextPlayer]) {
                currentPlayer = nextPlayer;
                List<AbstractAction> availableActions = _computeAvailableActions(state);
                if (availableActions.get(0) instanceof DoNothing) {
                    hasFinished[currentPlayer] = true;  // we can skip their action
                } else {
                    return;
                }
            }
        }
    }

    public void startNewPhase(PuertoRicoGameState state) {
        prePhaseProcessing(state);
        setNextPlayerWithAvailableAction(state, roleOwner);
        // it is possible that no players have anything to do, in which case we are done
        // otherwise we stick the role on the Stack to take control of the role actions
        if (executionComplete(state)) {
            state.setCurrentRole(null);
            postPhaseProcessing(state);
        } else {
            state.setActionInProgress(this);
        }
    }

    protected void prePhaseProcessing(PuertoRicoGameState state) {
        // do nothing by default...override in subclasses if needed
    }

    protected void postPhaseProcessing(PuertoRicoGameState state) {
        // do nothing by default...override in subclasses if needed
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        // if all players have finished, then we are done
        for (boolean b : hasFinished) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    public abstract T copy();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PuertoRicoRole<?>) {
            PuertoRicoRole<?> c = (PuertoRicoRole<?>) obj;
            return this.roleOwner == c.roleOwner && c.currentPlayer == currentPlayer &&
                    roleType == c.roleType &&
                    Arrays.equals(this.hasFinished, c.hasFinished);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleOwner, currentPlayer, roleType) + 31 * Arrays.hashCode(hasFinished);
    }

}

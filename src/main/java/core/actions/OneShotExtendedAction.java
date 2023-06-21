package core.actions;

import core.AbstractGameState;
import core.interfaces.IExtendedSequence;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 *  This is useful where we have one action that requires a single follow-on action by the same player that
 *  we are 100% confident *must* be the next action taken in the game.
 *
 *  This is a wrapper around a function that computes the available actions for the next step in the sequence.
 */
public class OneShotExtendedAction extends AbstractAction implements IExtendedSequence {

    protected int player;
    protected boolean executed;
    final String name;
    final Function<AbstractGameState, List<AbstractAction>> actionFunction;

    public OneShotExtendedAction(String name, Function<AbstractGameState, List<AbstractAction>> computeAvailableActions) {
        actionFunction = computeAvailableActions;
        this.name = name;
    }

    public boolean execute(AbstractGameState gs) {
        player = gs.getCurrentPlayer();
        gs.setActionInProgress(this);
        return true;
    }

    @Override
    public List<AbstractAction> _computeAvailableActions(AbstractGameState state) {
        return actionFunction.apply(state);
    }
    @Override
    public int getCurrentPlayer(AbstractGameState state) {
        return player;
    }

    @Override
    public void _afterAction(AbstractGameState state, AbstractAction action) {
        executed = true;
    }

    @Override
    public boolean executionComplete(AbstractGameState state) {
        return executed;
    }

    @Override
    public OneShotExtendedAction copy() {
        OneShotExtendedAction retValue = new OneShotExtendedAction(name, actionFunction);
        retValue.player = player;
        retValue.executed = executed;
        return retValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, executed, actionFunction);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof OneShotExtendedAction) {
            OneShotExtendedAction other = (OneShotExtendedAction) obj;
            return player == other.player && executed == other.executed && actionFunction.equals(other.actionFunction);
        }
        return false;
    }

    @Override
    public String toString() {
        return name;
    }
    @Override
    public String getString(AbstractGameState state) {
        return name;
    }

}

package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;

import java.util.List;

/**
 * An Action (usually) that entails a sequence of linked actions/decisions. This takes temporary control of deciding
 * which player is currently making a decision (the currentPlayer) from TurnOrder, and of what actions they have
 * available from ForwardModel.
 *
 * ForwardModel will register all actions taken and the current state just before execution of each action in next().
 *
 * IExtendedSequence is then responsible for tracking all local state necessary for its set of actions, and marking
 * itself as complete. (ForwardModel will then detect this, and remove it from the Stack of open actions.)
 *
 * Assuming this interface is used by an Action, then the execute(AbstractGameState state) method should call:
 *      state.setActionInProgress(this)
 * The core framework will then trigger delegation from ForwardModel and TurnOrder.
 */
public interface IExtendedSequence {

    /**
     * Forward Model delegates to this from computeAvailableActions() if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return the list of possible actions for the currentPlayer
     */
    List<AbstractAction> _computeAvailableActions(AbstractGameState state);
    default List<AbstractAction> _computeAvailableActions(AbstractGameState state, ActionSpace actionSpace) {
        return _computeAvailableActions(state);
    }

    /**
     * TurnOrder delegates to this from getCurrentPlayer() if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return The player Id whose move it is
     */
    int getCurrentPlayer(AbstractGameState state);

    /**
     * This is called by ForwardModel whenever an action has just been taken. It enables the IExtendedSequence
     * to maintain local state in whichever way is most suitable.
     *
     * After this call, the state of IExtendedSequence should be correct ahead of the next decision to be made.
     * In some cases there is no need to implement anything in this method - if for example you can tell if all
     * actions are complete from the state directly, then that can be implemented purely in executionComplete()
     *
     *
     * @param state The current game state
     * @param action The action that has just been taken
     */
    void _afterAction(AbstractGameState state, AbstractAction action);

    /**
     * Return true if this extended sequence has now completed and there is nothing left to do.
     *
     * @param state The current game state
     * @return True if all decisions are now complete
     */
    boolean executionComplete(AbstractGameState state);

    /**
     * Usual copy() standards apply.
     * NO REFERENCES TO COMPONENTS TO BE KEPT, PRIMITIVE TYPES ONLY.
     *
     * @return a copy of the Object
     */
    IExtendedSequence copy();
}
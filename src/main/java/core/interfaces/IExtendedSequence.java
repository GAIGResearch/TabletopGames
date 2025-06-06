package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;

import java.util.List;

/**
 * This is a mini-ForwardModel that takes temporary control of:
 *      i) which player is currently making a decision (the getCurrentPlayer()),
 *      ii) what actions they have (computeAvailableActions()), and
 *      iii) what happens after an action is taken (_afterAction()).
 * These are the three normal responsibilities of ForwardModel.
 *
 * IExtendedSequence is also responsible for tracking all local state necessary for its set of actions, and marking
 * itself as complete. (ForwardModel will then detect this, and remove it from the Stack of open actions.)
 * This means that - unlike ForwardModel - IExtendedSequence is not stateless, and hence must implement a copy() method.
 * Effectively an IExtendedSequence also incorporates a mini-GameState that tracks game progress within the sequence.
 *
 * ForwardModel retains responsibility for applying all actions (via next()).
 *
 * The GameState stores a Stack of IExtendedSequences, and the current one is always the one at the top of the stack.
 * This stack is deep-copied whenever the GameState is copied, so that the IExtendedSequences are also copied.
 *
 * To trigger an IExtendedSequence, it is added to the stack by calling:
 *      state.setActionInProgress(sequenceObject)
 * The core framework will then trigger delegation from ForwardModel.
 *
 * There are two common patterns for IExtendedSequence:
 *     i) Extending an Action directly, so that this then controls the later decisions that are part of the action.
 *     ii) A distinct sub-phase of the game, encapsulating a linked series of decisions.
 * In general the current advice is not to extend an Action directly, but to use the second pattern.
 * For example:
 *      - Player chooses Action A that requires a number of other decisions to be made.
 *      - Action A does not extend IExtendedSequence, but created a new Object (let's call it SubPhaseA) that does.
 *      - in execute() of Action A, it adds SubPhaseA to the stack with state.setActionInProgress(SubPhaseA)
 *      - SubPhaseA then controls the next set of decisions. Once the last decision is taken, SubPhaseA marks itself as complete.
 *
 * It does not need to be the Action that puts the IExtendedSequence on the stack. It could be triggered by any event.
 * Another common pattern is for this to be done in the _afterAction() method of the ForwardModel once certain
 * preconditions for SubPhaseA are met.
 *
 * After every action is taken, the ForwardModel will check the top of the stack to see if it is finished (and will
 * continue until it finds one that is not). If it is finished, it will remove it from the stack.
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
     * It is called as well as (and before) the _afterAction method on the ForwardModel.
     * This means that ForwardModel._afterAction() may need check to see if an action is in progress and skip
     * its own logic in this case:
     *          if (state.isActionInProgress()) continue;
     * This line of code has not yet been incorporated into the framework due to a couple of older games.
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
     * This is called whenever the IExtendedSequence is moved to the top of the queue.
     * It provides the extended sequence that was just removed (likely to be a child created by this sequence)
     * so that any clean up can take place.
     *
     * The default behaviour is to call _afterAction() on the completed sequence if it is an AbstractAction.
     * If it is *not* an AbstractAction, then this will need to be overridden.
     * @param state
     * @param completedSequence
     */
    default void afterRemovalFromQueue(AbstractGameState state, IExtendedSequence completedSequence) {
        if (completedSequence instanceof AbstractAction action) {
            this._afterAction(state, action);
        }
    }

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
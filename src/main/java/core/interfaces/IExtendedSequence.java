package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;

import java.util.Collections;
import java.util.List;

/**
 * This is a mini-ForwardModel that takes temporary control of:
 *      i) which player is currently making a decision (the getCurrentPlayer()),
 *      ii) what actions they have (computeAvailableActions()), and
 *      iii) what happens after an action is taken (_afterAction()).
 * These are the three normal responsibilities of ForwardModel.
 *
 * IExtendedSequence is also responsible for tracking all local state necessary for its set of actions, and marking
 * itself as complete. (ForwardModel will then detect this and remove it from the Stack of open actions.)
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
 *
 * When an action is executed with an IExtendedSequence on the stack, _afterAction() is called straight afterwards on
 * the sequence that was at the top of the stack before the action was executed, and on no other. This is the case even
 * if the action has put itself on the stack to continue as a sequence (pattern i above). Any sequence the action starts
 * (directly or via nested actions it executes) is not told about it.
 *
 * A sequence may therefore be complete while another sequence is still above it on the stack (for example, when the
 * second play of a card is itself an extended sequence). It is removed from the stack once everything above it has
 * completed. Only the top of the stack is checked, so a sequence that needs to wait for those above it (e.g. to decide
 * what to offer next) can check the state in executionComplete().
 */
public interface IExtendedSequence {

    /**
     * AbstractGameState delegates to this from getCurrentPlayer() if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return The player Id whose move it is
     */
    int getCurrentPlayer(AbstractGameState state);

    /**
     * If the IExtendedSequence represents a simultaneous action, then it is necessary to override
     * the getCurrentSimultaneousPlayers method
     * @return
     */
    default List<Integer> getCurrentSimultaneousPlayers(AbstractGameState state) {
        return Collections.singletonList(getCurrentPlayer(state));
    }

    /**
     * Forward Model delegates to this from computeAvailableActions() if this Extended Sequence is currently active.
     *
     * @param state The current game state
     * @return the list of possible actions for the currentPlayer
     */
    List<AbstractAction> _computeAvailableActions(AbstractGameState state);
    /**
     * If the IExtendedSequence represents a simultaneous action, then it is necessary instead to implement
     * _computeAvailableActions for a specific player
     * @param state
     * @param activePlayer
     * @return
     */
    default List<AbstractAction> _computeAvailableActions(AbstractGameState state, int activePlayer) {
        return _computeAvailableActions(state);
    }

    /**
     * Override this method if (and only if) you are implementing multiple action spaces for a game
     */
    default List<AbstractAction> _computeAvailableActions(AbstractGameState state, ActionSpace actionSpace) {
        return _computeAvailableActions(state);
    }

    /**
     * Override this method if (and only if) you are implementing multiple action spaces for a game AND we have simultaneous moves
     */
    default List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace, int activePlayer) {
        return _computeAvailableActions(gameState, actionSpace);
    }

    /**
     * This is called by ForwardModel just before an action is executed while this sequence is on top of the stack.
     * It is called instead of the ForwardModel's own _beforeAction().
     *
     * @param state The current game state
     * @param action The action about to be taken
     */
    default void _beforeAction(AbstractGameState state, AbstractAction action) {
    }

    /**
     * This is called by ForwardModel whenever an action has just been taken. It enables the IExtendedSequence
     * to maintain local state in whichever way is most suitable.
     *
     * The ForwardModel's own _afterAction() is only called if no sequence is left on the stack after this one has
     * been told (and any completed sequences removed). So a decision that completes the last sequence on the stack is
     * passed to the ForwardModel as well; otherwise it is not.
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

package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;

/**
 * Terraforming Mars sequences are told (via _afterAction) only about the decisions they offered, as soon as each is
 * taken (see StandardForwardModelWithTurnOrder._next). Each checks that it was given one of these, and throws an
 * AssertionError otherwise, so that a mis-routed notification is found where it happens rather than through a
 * corrupted game state later.
 */
public interface TMExtendedSequence extends IExtendedSequence {

    /**
     * A sequence above this one on the stack has completed. This is not passed on to _afterAction (the default):
     * this sequence was already told about its own decision when it was taken, and the completed sequence may be
     * unrelated, e.g. a sibling pushed by the same card after this one (Place Ocean above a ChoiceAction).
     */
    @Override
    default void afterRemovalFromQueue(AbstractGameState state, IExtendedSequence completedSequence) {
    }

    /**
     * @return true if the action is the plain pass offered when there is no legal choice
     */
    static boolean isPass(AbstractAction action) {
        return action.getClass() == TMAction.class && ((TMAction) action).pass;
    }

    default AssertionError unexpectedAction(AbstractAction action) {
        return new AssertionError(getClass().getSimpleName() + " [" + this + "] was told about an action it did not offer: "
                + action.getClass().getSimpleName() + " [" + action + "]");
    }
}

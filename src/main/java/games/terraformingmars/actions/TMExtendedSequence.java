package games.terraformingmars.actions;

import core.actions.AbstractAction;
import core.interfaces.IExtendedSequence;

/**
 * Terraforming Mars sequences are told (via _afterAction) only about the decisions they offered, as soon as each is
 * taken (see StandardForwardModel._next). Each checks that it was given one of these, and throws an
 * AssertionError otherwise, so that a mis-routed notification is found where it happens rather than through a
 * corrupted game state later.
 */
public interface TMExtendedSequence extends IExtendedSequence {

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

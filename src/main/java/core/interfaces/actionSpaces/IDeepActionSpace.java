package core.interfaces.actionSpaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

public interface IDeepActionSpace {
    /**
     * Calculates the list of currently available <b>legal</b> actions, possibly depending on the game phase.
     * Hierarchical space, high level actions as {@link core.interfaces.IExtendedSequence} which spawn low-level actions.
     *
     * @return - List of AbstractAction objects.
     */
    List<AbstractAction> computeAvailableDeepActions(AbstractGameState gameState);
}

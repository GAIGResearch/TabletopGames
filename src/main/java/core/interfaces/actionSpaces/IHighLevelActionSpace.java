package core.interfaces.actionSpaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

public interface IHighLevelActionSpace extends IActionSpace {
    /**
     * Calculates the list of currently available <b>legal</b> actions, possibly depending on the game phase.
     * Hierarchical space, high level actions as {@link core.interfaces.IExtendedSequence} which spawn low-level actions.
     *
     * @return - List of AbstractAction objects.
     */
    List<AbstractAction> computeAvailableHighLevelActions(AbstractGameState gameState);

    @Override
    default List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return computeAvailableHighLevelActions(gameState);
    }
}

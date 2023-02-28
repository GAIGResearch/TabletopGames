package core.interfaces.actionSpaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

public interface IFlatActionSpace extends IActionSpace {

    /**
     * Calculates the list of currently available actions, possibly depending on the game phase.
     * Combinatorial space, all possible <b>legal</b> actions
     *
     * @return - List of AbstractAction objects.
     */
    List<AbstractAction> computeAvailableFlatActions(AbstractGameState gameState);

    @Override
    default List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return computeAvailableFlatActions(gameState);
    }
}

package core.interfaces.actionSpaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

public interface IActionSpace {
    List<AbstractAction> _computeAvailableActions(AbstractGameState gameState);
}

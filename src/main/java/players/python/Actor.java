package players.python;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.List;

public interface Actor {
    /* Dummy interface that may be used in PyTAG to implement custom action selection.
    *
    *  Jpype only allows implementing interfaces, but not classes in python hence this is workaround
    *  to implement custom controllers.
    * */
    AbstractAction getAction(AbstractGameState gameState, List<AbstractAction> possibleActions);
}
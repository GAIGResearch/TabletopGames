package players.python;
// Dummy agent to know when to return obs, info for python agent

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;

public class PythonAgent extends AbstractPlayer {
    public PythonAgent() {
        super(null, "PythonAgent");
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gameState, List<AbstractAction> possibleActions) {
        return null;
    }

    @Override
    public AbstractPlayer copy() {
        return null;
    }
}

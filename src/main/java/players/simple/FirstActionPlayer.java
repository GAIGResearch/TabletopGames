package players.simple;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;

public class FirstActionPlayer extends AbstractPlayer{
    public FirstActionPlayer() {
        super(null, "FirstActionPlayer");
    }

    /**
     * Always chooses the first action for this agent.
     * Mainly being used to test Poker to keep choices fair (no raises, just calls)
     */

    @Override
    public AbstractAction _getAction(AbstractGameState observation, List<AbstractAction> possibleActions) {
        return possibleActions.get(0);
    }

    @Override
    public String toString() {
        return "FirstAction";
    }

    @Override
    public FirstActionPlayer copy() {
        return this;
    }
}

package players.simple;

import core.AbstractGameState;
import core.actions.AbstractAction;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;

import java.util.List;
import java.util.Random;

public class SameActionPlayer extends AbstractPlayer{
    /**
     * Always chooses the same action for this agent.
     * Mainly being used to test Poker to keep choices fair (no raises, just calls)
     */

    @Override
    public AbstractAction getAction(AbstractGameState observation, List<AbstractAction> actions) {

        return actions.get(0);
    }

    @Override
    public String toString() {
        return "SameAction";
    }
}

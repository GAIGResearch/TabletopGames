package players.mcts;

import core.actions.AbstractAction;

import java.util.function.Function;

public class MCTSEnums {

    public enum strategies {
        RANDOM
    }

    public enum SelectionPolicy {
        ROBUST, SIMPLE
    }

    public enum TreePolicy {
        UCB, EXP3, AlphaGo, RegretMatching
    }

    public enum OpponentTreePolicy {
        SelfOnly, Paranoid, Paranoid2
        // TODO: MaxN to be added at some point - but that needs a vector reward to be back-propagated
    }

}

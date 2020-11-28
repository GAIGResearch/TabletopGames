package players.mcts;

import core.actions.AbstractAction;

import java.util.function.Function;

public class MCTSEnums {

    public enum strategies {
        RANDOM, Dominion_BigMoney, Dominion_PlayActions
    }

    public enum SelectionPolicy {
        ROBUST, SIMPLE
    }

    public enum TreePolicy {
        UCB, EXP3, AlphaGo, RegretMatching
    }

    public enum OpponentTreePolicy {
        SelfOnly, Paranoid, MaxN
    }

}

package players.mcts;

public class MCTSEnums {

    public enum Strategies {
        RANDOM, MAST
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

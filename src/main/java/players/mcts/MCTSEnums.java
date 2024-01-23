package players.mcts;

public class MCTSEnums {

    public enum Strategies {
        RANDOM, MAST, CLASS, PARAMS, DEFAULT
    }

    public enum Information {
        Closed_Loop, Open_Loop, Information_Set
    }

    public enum MASTType {
        Rollout, Tree, Both
    }

    public enum SelectionPolicy {
        ROBUST, SIMPLE, TREE
        // ROBUST uses the most visited, SIMPLE the highest scoring
        // TREE uses the node tree policy (for EXP3 or Regret Matching)
    }

    public enum TreePolicy {
        UCB, UCB_Tuned, AlphaGo, EXP3, RegretMatching, Hedge
    }

    public enum RolloutTermination {
        DEFAULT, END_TURN, START_TURN, END_ROUND;
    }

    public enum OpponentTreePolicy {
        SelfOnly(true), OneTree(false),
        MultiTree(true),
        OMA(false), OMA_All(false),
        MCGS(false), MCGSSelfOnly(true);

        public final boolean selfOnlyTree;
        OpponentTreePolicy(boolean selfOnlyTree) {
            this.selfOnlyTree = selfOnlyTree;
        }
    }

}

package players.mcts;

public class MCTSEnums {

    public enum Strategies {
        RANDOM, MAST, CLASS, PARAMS
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
        UCB, EXP3, AlphaGo, RegretMatching, UCB_Tuned
    }

    public enum RolloutTermination {
        DEFAULT, END_TURN, START_TURN, END_ROUND;
    }

    public enum OpponentTreePolicy {
        SelfOnly(true), OneTree(false),
        MultiTree(true),
        OMA(false), OMA_All(false);

        boolean selfOnlyTree;
        OpponentTreePolicy(boolean selfOnlyTree) {
            this.selfOnlyTree = selfOnlyTree;
        }
    }

}

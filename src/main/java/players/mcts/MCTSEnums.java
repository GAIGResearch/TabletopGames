package players.mcts;

public class MCTSEnums {

    public enum Strategies {
        RANDOM, MAST, CLASS, PARAMS, DEFAULT
    }

    public enum Information {
        Closed_Loop, Open_Loop, Information_Set
    }

    public enum MASTType {
        None, Rollout, Tree, Both
    }

    public enum SelectionPolicy {
        ROBUST, SIMPLE, TREE
        // ROBUST uses the most visited, SIMPLE the highest scoring
        // TREE uses the node tree policy (for EXP3 or Regret Matching)
    }

    public enum TreePolicy {
        UCB, UCB_Tuned, AlphaGo, EXP3, RegretMatching
    }

    public enum RolloutTermination {
        DEFAULT, END_ACTION, END_TURN, START_ACTION, END_ROUND
        // ???_ACTION refers to the acting player (regardless of Turn or Round)
        // END_ACTION will stop a rollout when the player changes from the acting player; and START_ACTION will keep going until it is their action again
        // END_TURN|ROUND is triggered when the game round/turn changes
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

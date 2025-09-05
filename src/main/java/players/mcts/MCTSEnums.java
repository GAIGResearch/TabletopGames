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
        ROBUST, SIMPLE
        // ROBUST uses the most visited, SIMPLE the highest scoring
        // for EXP3 or Regret Matching override this
    }

    public enum TreePolicy {
        UCB, UCB_Tuned, AlphaGo, EXP3, RegretMatching, NoAveragingRM, Uniform, Greedy
    }

    public enum BackupPolicy {
        MonteCarlo, Lambda, MaxLambda, MaxMC
        // MonteCarlo is the standard backup policy
        // Lambda uses a SARSA-style on-policy backup; then specify the backupLambda to use. (lambda = 1 is equivalent to MonteCarlo)
        // MaxLambda uses a similar off-policy backup (with Q from argmax over actions); then specify the backupLambda to use. (lambda = 1 is equivalent to MonteCarlo)
        // MaxMC uses a different approach (dependent on the maxBackupThreshold parameter). It will mix in the max option once this threshold is passed at any given node.
        // Additionally, MaxMC only applies if the best action was not taken - this gives different behaviour to lambda-style backups, which always mis in the Q value
        // even if the best action was taken (this down-weights the actual observed reward from that iteration.)
    }

    public enum RolloutIncrement {
        TICK, TURN, ROUND
        // Determine which event will trigger an increment on the rolloutDepth counter.
        // For example, it can be set to TURN when the game's score is only affected by the last move made by a player,
        // or ROUND when scores are only tallied at the end of a round.
        // Useful when the number of moves made per turn or per round is not a fixed amount.
    }

    public enum RolloutTermination {
        EXACT, END_ACTION, END_TURN, START_ACTION, END_ROUND
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

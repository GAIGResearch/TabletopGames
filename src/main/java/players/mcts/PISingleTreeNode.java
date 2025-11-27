package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.PlayerConstants;

import java.util.*;

import static players.PlayerConstants.*;

/**
 * PI-MCTS (Perfect-Information Monte Carlo Tree Search) wrapper for MCTS
 * Uses multiple determinised SingleTreeNode roots to create several
 * determinisations, running MCTS on each determinised state, and then aggregating the suggested
 * root actions across trees using a chosen aggregation policy.
 *
 * This class does the following:
 *  - Build N determinized trees from the decision point.
 *  - Runs MCTS on each determinized root.
 *  - Aggregates action statistics to inform the final choice.
 */
public class PISingleTreeNode extends SingleTreeNode {
    //One root MCTS tree per determinisation
    SingleTreeNode[] roots;
    AbstractGameState state;
    //Number of determinisations(roots) to create;
    int numDeterminizations = 1;
    MCTSPlayer mctsPlayer;
    double epsilon = 1e-6;

    /**
     * Aggregated action statistics across determinisations.
     * Key:   action
     * Value: accumulated stats
     */
    Map<AbstractAction, ActionStats> accumulatedActionStats;

    //Instantiate A new Tree Node for PI-MCTS
    public PISingleTreeNode(MCTSPlayer player, AbstractGameState state, Random rnd) {

        this.decisionPlayer = state.getCurrentPlayer();
        this.params = player.getParameters();
        this.forwardModel = player.getForwardModel();
        this.rnd = rnd;
        this.state = state;
        mctsPlayer = player;
        MASTStatistics = new ArrayList<>();
        for (int i = 0; i < state.getNPlayers(); i++)
            MASTStatistics.add(new HashMap<>());
        if (params.useMASTAsActionHeuristic) {
            params.actionHeuristic = new MASTActionHeuristic(MASTStatistics, params.MASTActionKey, params.MASTDefaultValue);
        }
        numDeterminizations = params.numDeterminizations;
        roots = new SingleTreeNode[params.numDeterminizations];
    }


     //Run PI-MCTS search where for each determinisation, state is copied, a SingleTreeNode root is created, and MCTS search is performed.

    @Override
    public void mctsSearch(long initialisationTime) {
        initialiseRootMetrics();
        initialisationTimeTaken = initialisationTime;

        // Create determinised trees and run MCTS on each one independently.
        for (int i = 0; i < params.numDeterminizations; i++) {
            roots[i] = SingleTreeNode.createRootNode(mctsPlayer, state.copy(state.getCurrentPlayer()), rnd, mctsPlayer.getFactory());
            // MCTS on the determinised root.
            roots[i].mctsSearch(initialisationTime);
        }
    }


     //The choice is made by aggregating recommendations across determinisations using the chosen PerfectInformationPolicy.

    @Override
    public AbstractAction bestAction()
    {
        AbstractAction calculatedAction = null;

        switch (params.perfectInformationPolicy)
        {
            case SingleVote :
            {
                return bestAction_SingleVote();
            }
            case AverageValue:
            {
                return bestAction_AccumulatedResults(MCTSEnums.PerfectInformationPolicy.AverageValue);
            }
            case TotalValue:
            {
                return bestAction_AccumulatedResults(MCTSEnums.PerfectInformationPolicy.TotalValue);
            }
            case TotalVisits:
            {
                return  bestAction_AccumulatedResults(MCTSEnums.PerfectInformationPolicy.TotalVisits);
            }
        }
        return calculatedAction;
    }

    /**
     * Single-vote aggregation:
     *  - Let each determinised root pick its own best action.
     *  - Aggregate votes for actions across all determinisations.
     *  - Pick the action with the highest count with tiny noise to break ties.
     */
    AbstractAction bestAction_SingleVote() {
        //Voting
        Map<AbstractAction, Integer> actionCounts = new HashMap<>();
        for (int i = 0; i < params.numDeterminizations; i++) {
            AbstractAction action = roots[i].bestAction();
            actionCounts.put(action, actionCounts.getOrDefault(action, 0) + 1);
        }
        //Vote Counting
        AbstractAction bestAction_vote = null;
        double maxCount = -1;
        for (Map.Entry<AbstractAction, Integer> entry : actionCounts.entrySet()) {
            double totValue = entry.getValue() +( rnd.nextGaussian() * epsilon);
            if (totValue > maxCount) {
                maxCount = totValue ;
                bestAction_vote = entry.getKey();
            }
        }
        return bestAction_vote;
    }

    /**
     * Aggregation by accumulated statistics from all determinised roots based on selected aggregation policy
     *  TotalValue:   Total Value for decisionPlayer per action node
     *  TotalVisits:  total visits to the action node
     *  AverageValue: TotalValue / TotalVisits
     */
    AbstractAction bestAction_AccumulatedResults(MCTSEnums.PerfectInformationPolicy policy) {
        accumulatedActionStats = new HashMap<>();
        for (int i = 0; i < params.numDeterminizations; i++) {
            Map<AbstractAction, ActionStats> currentActionStats = roots[i].actionValues;
            for (Map.Entry<AbstractAction, ActionStats> entry : currentActionStats.entrySet()) {
                AbstractAction action = entry.getKey();
                ActionStats stats = entry.getValue();
                if (!accumulatedActionStats.containsKey(action)) {
                    accumulatedActionStats.put(action, stats.copy());
                } else {
                    ActionStats accumulated = accumulatedActionStats.get(action);
                    for (int j = 0; j < stats.totValue.length; j++) {
                        accumulated.totValue[j] += stats.totValue[j];
                        accumulated.squaredTotValue[j] += stats.squaredTotValue[j];
                    }
                    accumulated.nVisits += stats.nVisits;
                    accumulated.validVisits += stats.validVisits;
                }
            }
        }
        //Picking the best action based on aggregated statistics and chosen aggregation policy
        AbstractAction currentBestAction = null;
        double bestValue = Double.NEGATIVE_INFINITY;
        for (Map.Entry<AbstractAction, ActionStats> entry : accumulatedActionStats.entrySet()) {
            AbstractAction action = entry.getKey();
            ActionStats stats = entry.getValue();
            if (stats.nVisits > 0) {
                double totValue = 0.0;
                switch (policy) {
                    case AverageValue:
                        totValue = (stats.totValue[decisionPlayer] + (rnd.nextGaussian() * epsilon)) / (stats.nVisits +( rnd.nextGaussian() * epsilon)) ;
                        break;
                    case TotalValue:
                        totValue = stats.totValue[decisionPlayer] + (rnd.nextGaussian() * epsilon);
                        break;
                    case TotalVisits:
                        totValue = stats.nVisits + (rnd.nextGaussian() * epsilon) ;
                }
                if (totValue > bestValue) {
                    bestValue = totValue;
                    currentBestAction = action;
                }
            }
        }
        return currentBestAction;
    }
}



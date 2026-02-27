package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import utilities.Pair;
import java.util.*;

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
public class ForestNode extends SingleTreeNode {
    //One root MCTS tree per determinisation
    SingleTreeNode[] roots;
    AbstractGameState state;
    MCTSPlayer mctsPlayer;

    /**
     * Aggregated action statistics across determinisations.
     * Key:   action
     * Value: accumulated stats
     */
    Map<AbstractAction, ActionStats> accumulatedActionStats;

    //Instantiate A new Tree Node for PI-MCTS
    public ForestNode(MCTSPlayer player, AbstractGameState state, Random rnd) {

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
            params.actionHeuristic = new MASTActionHeuristic(params.MASTActionKey, params.MASTDefaultValue);
            ((MASTActionHeuristic) params.actionHeuristic).setMASTStats(MASTStatistics);
        }
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
            roots[i].MASTStatistics = MASTStatistics;
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
            double totValue = entry.getValue() +( rnd.nextGaussian() * params.noiseEpsilon);
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
                        totValue = (stats.totValue[decisionPlayer] + (rnd.nextGaussian() * params.noiseEpsilon)) / (stats.nVisits +( rnd.nextGaussian() * params.noiseEpsilon)) ;
                        break;
                    case TotalVisits:
                        totValue = stats.nVisits + (rnd.nextGaussian() * params.noiseEpsilon) ;
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



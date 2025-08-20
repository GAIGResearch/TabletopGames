package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import players.PlayerConstants;

import java.util.*;

import static players.PlayerConstants.*;

public class PISingleTreeNode extends SingleTreeNode {
    SingleTreeNode[] roots;
    AbstractGameState state;
    int numDeterminizations = 1;
    MCTSPlayer mctsPlayer;
    double epsilon = 1e-6;
    //Action Stats variable to hold aggregated values
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

    @Override
    public void mctsSearch(long initialisationTime) {
        initialiseRootMetrics();
        initialisationTimeTaken = initialisationTime;
        //Create Determinized Trees and Run MCTS on the Determinized Trees
        for (int i = 0; i < params.numDeterminizations; i++) {
            roots[i] = SingleTreeNode.createRootNode(mctsPlayer, state.copy(state.getCurrentPlayer()), rnd, mctsPlayer.getFactory());
            roots[i].mctsSearch(initialisationTime);
        }
    }
    //Returns the best action after search based on the chosen AggrergationPolicy
    @Override
    public AbstractAction bestAction()
    {
        AbstractAction calulatedAction = null;

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
        return calulatedAction;
    }

    //Returns the best action based on the SingleVote Aggregation Policy
    AbstractAction bestAction_SingleVote()
    {
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

    AbstractAction bestAction_AccumulatedResults(MCTSEnums.PerfectInformationPolicy policy)
    {
        //Aggregating Statistics
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



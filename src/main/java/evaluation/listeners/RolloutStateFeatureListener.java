package evaluation.listeners;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import evaluation.metrics.Event;
import utilities.Pair;

import java.util.Arrays;

/**
 * A Rollout state feature listener applies a specified rollout policy from the given game state, and records the final end game value reached
 * (with the state features of the starting game state).
 * This can be modified to have N rollouts, of which the average is then taken
 * <p>
 * The only functionality that this overrides from the parent is to populate the Value and Ordinal fields based on
 * the rollout. This will ensure that
 */
public class RolloutStateFeatureListener extends StateFeatureListener {

    AbstractForwardModel forwardModel;
    AbstractPlayer[] rolloutPlayers;
    int rollouts = 1;
    double[][] rolloutResults = null;

    public RolloutStateFeatureListener(IStateFeatureVector phi,
                                       AbstractPlayer[] rolloutPlayers,
                                       AbstractForwardModel forwardModel) {
        // We always start a new rollout when an action is taken, and we just record the value from the perspective of the acting player
        super(phi, Event.GameEvent.ACTION_CHOSEN, false);
        for (AbstractPlayer p : rolloutPlayers) {
            p.setForwardModel(forwardModel);
        }
        this.rolloutPlayers = rolloutPlayers;
        this.forwardModel = forwardModel;
    }

    public RolloutStateFeatureListener setRollouts(int rollouts) {
        this.rollouts = rollouts;
        return this;
    }

    @Override
    public String[] names() {
        // we add Value and Ordinal to the list, which will be calculated from the rollouts
        String[] names = new String[super.names().length + 5];
        System.arraycopy(super.names(), 0, names, 0, super.names().length);
        names[names.length - 5] = "FinalHeuristic";
        names[names.length - 4] = "Win";
        names[names.length - 3] = "FinalScore";
        names[names.length - 2] = "FinalScoreAdv";
        names[names.length - 1] = "Ordinal";
        return names;
    }

    @Override
    public void preProcessing(AbstractGameState state, AbstractAction action) {
        rolloutResults = rolloutFrom(state);
    }

    @Override
    public double[] extractDoubleVector(AbstractAction action, AbstractGameState state, int perspectivePlayer) {
        double[] base = super.extractDoubleVector(action, state, perspectivePlayer);
        double[] retValue = new double[base.length + 5];
        System.arraycopy(base, 0, retValue, 0, base.length);
        retValue[base.length] = rolloutResults[0][perspectivePlayer];
        retValue[base.length + 1] = rolloutResults[1][perspectivePlayer];
        retValue[base.length + 2] = rolloutResults[2][perspectivePlayer];
        retValue[base.length + 3] = rolloutResults[3][perspectivePlayer];
        retValue[base.length + 4] = rolloutResults[4][perspectivePlayer];
        return retValue;
    }

    @Override
    public Object[] extractFeatureVector(AbstractAction action, AbstractGameState state, int perspectivePlayer) {
        Object[] base = super.extractFeatureVector(action, state, perspectivePlayer);
        Object[] retValue = new Object[base.length + 5];
        System.arraycopy(base, 0, retValue, 0, base.length);
        retValue[base.length] = rolloutResults[0][perspectivePlayer];
        retValue[base.length + 1] = rolloutResults[1][perspectivePlayer];
        retValue[base.length + 2] = rolloutResults[2][perspectivePlayer];
        retValue[base.length + 3] = rolloutResults[3][perspectivePlayer];
        retValue[base.length + 4] = rolloutResults[4][perspectivePlayer];
        return retValue;
    }

    /**
     * We roll out from the given state using the specified policies (AbstractPlayers)
     *
     * @param state
     * @return
     */
    protected double[][] rolloutFrom(AbstractGameState state) {
        double[] totalScore = new double[state.getNPlayers()];
        double[] totalOrdinal = new double[state.getNPlayers()];
        double[] totalWin = new double[state.getNPlayers()];
        double[] totalLead = new double[state.getNPlayers()];
        double[] totalHeuristic = new double[state.getNPlayers()];
        for (int i = 0; i < rollouts; i++) {
            // firstly we reset our players (to remove any information they may have from previous rollouts)
            for (AbstractPlayer p : rolloutPlayers) {
                p.initializePlayer(state);
            }
            AbstractGameState copy = state.copy(state.getCurrentPlayer());
            while (copy.isNotTerminal()) {
                AbstractPlayer player = rolloutPlayers[copy.getCurrentPlayer()];
                AbstractAction nextAction = player.getAction(copy, forwardModel.computeAvailableActions(copy));
                forwardModel.next(copy, nextAction);
            }
            for (int p = 0; p < state.getNPlayers(); p++) {
                totalScore[p] += copy.getGameScore(p);
                double bestOtherScore = Double.NEGATIVE_INFINITY;
                for (int p2 = 0; p2 < copy.getNPlayers(); p2++) {
                    if (p2 == p) continue;
                    if (copy.getGameScore(p2) > bestOtherScore)
                        bestOtherScore = copy.getGameScore(p2);
                }
                totalLead[p] += copy.getGameScore(p) - bestOtherScore;
                if (copy.getWinners().contains(p)) {
                    if (copy.getWinners().size() == 1) {
                        totalWin[p] += 1.0;
                    } else {
                        totalWin[p] += 0.5;
                    }
                }
                totalOrdinal[p] += copy.getOrdinalPosition(p);
                totalHeuristic[p] += heuristic == null ? copy.getHeuristicScore(p) : heuristic.evaluateState(copy, p);
            }
        }
        Arrays.setAll(totalHeuristic, p -> totalHeuristic[p] / rollouts);
        Arrays.setAll(totalWin, p -> totalWin[p] / rollouts);
        Arrays.setAll(totalScore, p -> totalScore[p] / rollouts);
        Arrays.setAll(totalLead, p -> totalLead[p] / rollouts);
        Arrays.setAll(totalOrdinal, p -> totalOrdinal[p] / rollouts);
        return new double[][]{totalHeuristic, totalWin, totalLead, totalScore, totalOrdinal};
    }
}

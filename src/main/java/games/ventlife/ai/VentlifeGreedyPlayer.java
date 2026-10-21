package games.ventlife.ai;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.ventlife.VentlifeForwardModel;
import games.ventlife.VentlifeGameState;
import players.IAnyTimePlayer;

import java.util.*;

/**
 * Chooses each creature placement or move by one-step look-ahead on VentlifeHeuristic. A tile placement is chosen
 * by playing out the rest of the turn: each tile is valued one step ahead, then the best tileLookahead of them (as
 * many as the budget allows) are followed by greedy choices to the end of the turn, and valued there.
 */
public class VentlifeGreedyPlayer extends AbstractPlayer implements IAnyTimePlayer {

    private static final VentlifeForwardModel fm = new VentlifeForwardModel();

    public VentlifeGreedyPlayer(VentlifeGreedyParams params) {
        super(params, "VentlifeGreedy");
    }

    public VentlifeGreedyPlayer() {
        this(new VentlifeGreedyParams());
    }

    @Override
    public VentlifeGreedyParams getParameters() {
        return (VentlifeGreedyParams) parameters;
    }

    private double value(VentlifeGameState state, int player) {
        return VentlifeHeuristic.value(player, VentlifeScorer.scores(state), getParameters().oppMaxWeight,
                getParameters().oppMeanWeight);
    }

    private static boolean isTileDecision(AbstractGameState state) {
        return state.getGamePhase() == VentlifeGameState.Phase.PLACE_TILE && !state.isActionInProgress();
    }

    @Override
    public AbstractAction _getAction(AbstractGameState gs, List<AbstractAction> actions) {
        long start = System.nanoTime();
        if (actions.size() == 1)
            return actions.get(0);
        VentlifeGameState state = (VentlifeGameState) gs;
        int player = state.getCurrentPlayer();
        int n = actions.size();
        VentlifeGameState[] next = new VentlifeGameState[n];
        double[] oneStep = new double[n];
        for (int i = 0; i < n; i++) {
            next[i] = (VentlifeGameState) state.copy();
            fm.next(next[i], actions.get(i).copy());
            oneStep[i] = value(next[i], player) + rnd.nextDouble() * 1e-6;
        }
        Integer[] order = new Integer[n];
        for (int i = 0; i < n; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingDouble(i -> -oneStep[i]));
        int k = getParameters().tileLookahead;
        if (!isTileDecision(state) || k <= 0)
            return actions.get(order[0]);
        // play out the rest of the turn after the best tiles
        long deadline = start + (long) (getParameters().budget * 0.8e6);
        int best = order[0];
        double bestValue = Double.NEGATIVE_INFINITY;
        for (int j = 0; j < Math.min(k, n); j++) {
            if (j > 0 && System.nanoTime() > deadline)
                break;
            int i = order[j];
            VentlifeGameState s = next[i];
            playOutTurn(s, player);
            double v = value(s, player);
            if (v > bestValue) {
                bestValue = v;
                best = i;
            }
        }
        return actions.get(best);
    }

    /**
     * Greedy one-step choices, for whoever is to decide, until the next tile placement.
     */
    private void playOutTurn(VentlifeGameState s, int player) {
        while (s.isNotTerminal() && !isTileDecision(s)) {
            int decider = s.getCurrentPlayer();
            List<AbstractAction> options = fm.computeAvailableActions(s);
            AbstractAction bestAction = options.get(0);
            if (options.size() > 1) {
                double bestValue = Double.NEGATIVE_INFINITY;
                for (AbstractAction a : options) {
                    VentlifeGameState c = (VentlifeGameState) s.copy();
                    fm.next(c, a.copy());
                    double v = value(c, decider) + rnd.nextDouble() * 1e-6;
                    if (v > bestValue) {
                        bestValue = v;
                        bestAction = a;
                    }
                }
            }
            fm.next(s, bestAction.copy());
        }
    }

    @Override
    public VentlifeGreedyPlayer copy() {
        VentlifeGreedyPlayer retValue = new VentlifeGreedyPlayer((VentlifeGreedyParams) getParameters().copy());
        retValue.setName(toString());
        return retValue;
    }

    @Override
    public void setBudget(int budget) {
        parameters.setParameterValue("budget", budget);
    }

    @Override
    public int getBudget() {
        return parameters.budget;
    }
}

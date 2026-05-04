package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.actions.AbstractAction;
import core.interfaces.IStateHeuristic;
import it.unimi.dsi.fastutil.ints.Int2BooleanSortedMap;
import players.mcts.MCTSPlayer;

import java.util.List;
import java.util.stream.IntStream;

public class UnderdogAdviser extends AbstractMCTSAdviser {

    IStateHeuristic heuristic;
    double losingThreshold;

    public UnderdogAdviser(IStateHeuristic heuristic, double losingThreshold, double adviceThreshold) {
        super(adviceThreshold);
        this.heuristic = heuristic;
        this.losingThreshold = losingThreshold;
    }

    @Override
    public boolean payAttention(AbstractGameState state, AbstractAction proposedAction, AbstractPlayer advisee) {
        // we decide if the current player is losing by more than the losingThreshold
        List<Double> stateValues = IntStream.range(0, state.getNPlayers())
                .mapToObj(playerId -> heuristic.evaluateState(state, playerId))
                .toList();
        double maxValue = stateValues.stream().max(Double::compare).orElseThrow();
        double adviseeValue = stateValues.get(advisee.getPlayerID());
        return maxValue - adviseeValue > losingThreshold;
    }

}

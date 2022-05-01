package players.heuristics;

import core.AbstractGameState;
import core.interfaces.ILearner;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;

public class SimpleRegressionStateHeuristic implements IStateHeuristic, ILearner {
    @Override
    public void learnFrom(IStateFeatureVector[] stateVectors, double[] values) {
        // not yet implemented
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        // placeholder for testing
        return state.getGameScore(playerId);
    }
}

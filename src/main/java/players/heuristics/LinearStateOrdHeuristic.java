package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import utilities.Utils;


public class LinearStateOrdHeuristic extends LinearStateHeuristic {

    public LinearStateOrdHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClass) {
        super(featureVectorClassName, coefficientsFile, defaultHeuristicClass);
    }
    public LinearStateOrdHeuristic(String featureVectorClassName, String coefficientsFile) {
        super(featureVectorClassName, coefficientsFile, "");
    }
    public LinearStateOrdHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        minValue = -state.getNPlayers();
        maxValue = -1.0;
        if (state.isNotTerminalForPlayer(playerId)) {
            return super.evaluateState(state, playerId);
        }

        return -state.getOrdinalPosition(playerId);
    }
}

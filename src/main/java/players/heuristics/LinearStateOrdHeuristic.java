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
        if (state.isNotTerminalForPlayer(playerId)) {
            double retValue = super.evaluateState(state, playerId);
            return Utils.range(retValue, -state.getNPlayers(), -1.0);
        }

        return -state.getOrdinalPosition(playerId);
    }
}

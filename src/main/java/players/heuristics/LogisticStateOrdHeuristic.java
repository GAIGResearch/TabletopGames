package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;


public class LogisticStateOrdHeuristic extends LogisticStateHeuristic {

    public LogisticStateOrdHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClass) {
        super(featureVectorClassName, coefficientsFile, defaultHeuristicClass);
    }

    public LogisticStateOrdHeuristic(String featureVectorClassName, String coefficientsFile) {
        super(featureVectorClassName, coefficientsFile, "");
    }

    public LogisticStateOrdHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        if (state.isNotTerminalForPlayer(playerId)) {
            return super.evaluateState(state, playerId);
        }

        double ordinalPos = state.getOrdinalPosition(playerId);
        double playerCount = state.getNPlayers();
        return (playerCount - ordinalPos) / (playerCount - 1.0);
    }
}

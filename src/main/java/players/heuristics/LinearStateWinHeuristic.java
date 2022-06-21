package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import utilities.Utils;


public class LinearStateWinHeuristic extends LinearStateHeuristic {

    public LinearStateWinHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClass) {
        super(featureVectorClassName, coefficientsFile, defaultHeuristicClass);
    }
    public LinearStateWinHeuristic(String featureVectorClassName, String coefficientsFile) {
        super(featureVectorClassName, coefficientsFile, "");
    }
    public LinearStateWinHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        if (state.isNotTerminalForPlayer(playerId)) {
            double retValue = super.evaluateState(state, playerId);
            return Utils.range(retValue, 0.0, 1.0);
        }

        switch (state.getPlayerResults()[playerId]) {
            case WIN:
                return 1.0;
            case LOSE:
                return 0.0;
            case DRAW:
                return 0.5;
            default:
                throw new AssertionError("Not reachable for " + state.getPlayerResults()[playerId]);
        }
    }
}

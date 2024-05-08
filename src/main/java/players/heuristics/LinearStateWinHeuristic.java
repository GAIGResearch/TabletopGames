package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;


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
        minValue = 0.0;
        maxValue = 1.0;
        if (state.isNotTerminalForPlayer(playerId)) {
            return super.evaluateState(state, playerId);
        }

        switch (state.getPlayerResults()[playerId]) {
            case WIN_GAME:
                return 1.0;
            case LOSE_GAME:
                return 0.0;
            case DRAW_GAME:
                return 0.5;
            default:
                throw new AssertionError("Not reachable for " + state.getPlayerResults()[playerId]);
        }
    }
}

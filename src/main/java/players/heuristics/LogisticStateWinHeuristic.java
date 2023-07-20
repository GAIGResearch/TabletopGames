package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;


public class LogisticStateWinHeuristic extends LogisticStateHeuristic {

    public LogisticStateWinHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClass) {
        super(featureVectorClassName, coefficientsFile, defaultHeuristicClass);
    }
    public LogisticStateWinHeuristic(String featureVectorClassName, String coefficientsFile) {
        super(featureVectorClassName, coefficientsFile, "");
    }
    public LogisticStateWinHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        if (state.isNotTerminalForPlayer(playerId)) {
            return super.evaluateState(state, playerId);
        }

        switch (state.getPlayerResults()[playerId]) {
            case WIN_GAME:
            case WIN_ROUND:
                return 1.0;
            case LOSE_GAME:
            case LOSE_ROUND:
                return 0.0;
            case DRAW_GAME:
                return 0.5;
            default:
                throw new AssertionError("Not reachable for " + state.getPlayerResults()[playerId]);
        }
    }
}

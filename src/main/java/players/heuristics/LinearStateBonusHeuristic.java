package players.heuristics;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;
import core.interfaces.IStateHeuristic;
import utilities.Utils;


public class LinearStateBonusHeuristic extends LinearStateHeuristic {

    public LinearStateBonusHeuristic(String featureVectorClassName, String coefficientsFile, String defaultHeuristicClass) {
        super(featureVectorClassName, coefficientsFile, defaultHeuristicClass);
    }
    public LinearStateBonusHeuristic(String featureVectorClassName, String coefficientsFile) {
        super(featureVectorClassName, coefficientsFile, "");
    }
    public LinearStateBonusHeuristic(IStateFeatureVector featureVector, String coefficientsFile, IStateHeuristic defaultHeuristic) {
        super(featureVector, coefficientsFile, defaultHeuristic);
    }

    @Override
    public double evaluateState(AbstractGameState state, int playerId) {
        if (state.isNotTerminalForPlayer(playerId)) {
            return super.evaluateState(state, playerId);
        }

        switch (state.getPlayerResults()[playerId]) {
            case WIN:
                return 1.5 * super.evaluateState(state, playerId);
            case LOSE:
                return 0.75 * super.evaluateState(state, playerId);
            case DRAW:
                return super.evaluateState(state, playerId);
            default:
                throw new AssertionError("Not reachable for " + state.getPlayerResults()[playerId]);
        }
    }
}

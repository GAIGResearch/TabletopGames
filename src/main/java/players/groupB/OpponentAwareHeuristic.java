package players.groupB;

import core.AbstractGameState;

/**
 * OpponentAwareHeuristic
 * Blueprint version:
 * - Evaluates the game state using basic scoring.
 * - Later: adjusts heuristic based on opponent model predictions.
 */
public class OpponentAwareHeuristic {

    private FrequencyOpponentModel opponentModel;

    public OpponentAwareHeuristic(FrequencyOpponentModel model) {
        this.opponentModel = model;
    }

    /**
     * Placeholder evaluation function.
     * Later will combine own score and predicted opponent behaviour.
     */
    public double evaluate(AbstractGameState gameState, int playerId) {
        // TODO: Implement real heuristic logic in Phase 2
        return 0.0;
    }
}

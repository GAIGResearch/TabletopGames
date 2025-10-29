package players.groupB;

import core.AbstractGameState;

/**
 * OpponentAwareHeuristic
 * ----------------------
 * Evaluates the quality of a game state for the active player.
 * Combines:
 *   (1) Self utility (e.g., your Sushi Go! score)
 *   (2) A penalty for strong / aggressive opponents.
 *
 * This allows the agent to avoid greedy drafting when it predicts
 * that other players will heavily compete for the same cards.
 */
public class OpponentAwareHeuristic {

    private final FrequencyOpponentModel opponentModel;

    public OpponentAwareHeuristic(FrequencyOpponentModel model) {
        this.opponentModel = model;
    }

    /**
     * Evaluate a given AbstractGameState from the perspective of playerId.
     * @param gameState The current state of the game
     * @param playerId  The ID of the evaluating player
     * @return A numeric score: higher = better for playerId
     */
    public double evaluate(AbstractGameState gameState, int playerId) {
        // Step 1: Estimate player's own score (self utility)
        double selfUtility;
        try {
            selfUtility = gameState.getGameScore(playerId);
        } catch (Exception e) {
            // Some games may not expose getGameScore early; fail-safe to 0
            selfUtility = 0.0;
        }

        // Step 2: Estimate how aggressive opponents appear overall
        double totalAggressiveness = 0.0;
        int opponentCount = Math.max(1, gameState.getNPlayers() - 1);

        for (int opp = 0; opp < gameState.getNPlayers(); opp++) {
            if (opp == playerId) continue;
            totalAggressiveness += opponentModel.getAggressivenessScore(opp);
        }

        double avgAggressiveness = totalAggressiveness / opponentCount;

        // Step 3: Combine both components
        // The lambda weight (0.1) controls how much the penalty matters
        double lambda = 0.1;
        double heuristicValue = selfUtility - lambda * avgAggressiveness;

        return heuristicValue;
    }
}

package players.groupB;

import core.AbstractGameState;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;
import java.util.List;

/**
 * OpponentAwareHeuristic
 * Evaluates SushiGo! states using self-value + predicted opponent threats.
 */
public class OpponentAwareHeuristic {

    private final FrequencyOpponentModel opponentModel;

    public OpponentAwareHeuristic(FrequencyOpponentModel model) {
        this.opponentModel = model;
    }

    /**
     * Evaluates how good the state is for a given player.
     * Combines self score + opponent threat estimation.
     */
    public double evaluate(AbstractGameState gameState, int playerId) {
        if (!(gameState instanceof SGGameState)) return 0.0;
        SGGameState sgs = (SGGameState) gameState;

        // Self value (simplified): current score
        double selfScore = sgs.getGameScore(playerId);

        // Opponent threat: sum of their strongest move probabilities
        double threatScore = 0.0;
        for (int opp = 0; opp < sgs.getNPlayers(); opp++) {
            if (opp == playerId) continue;
            // Example: estimate if opponent tends to grab high-value cards (e.g., Sashimi)
            threatScore += opponentModel.getAggressivenessScore(opp) * 0.1;
        }

        // Combine both
        return selfScore - threatScore; // lower if opponents are “dangerous”
    }
}

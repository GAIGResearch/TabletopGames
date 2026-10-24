package players.sushiGoAgent;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;

public class SpecialScoreHeuristic implements IStateHeuristic {

    /**
     * This cares mostly about the raw game score - but will treat winning as a 50% bonus
     * and losing as halving it
     *
     * @param gs       - game state to evaluate and score.
     * @param playerId - player id
     * @return
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        double score = gs.getGameScore(playerId);
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.WIN_GAME)
            return score * 1.5;
        if (gs.getPlayerResults()[playerId] == CoreConstants.GameResult.LOSE_GAME)
            return score * 0.5;
        // MODIFIED: puddings only score at the end of the game, so a search that stops at the end of a
        // round never sees them. While the game is still running, add the pudding points this player would
        // get if the game ended now. At the end of the game the real pudding points are already in the score.
        if (gs.isNotTerminal() && gs instanceof SGGameState sggs)
            score += estimatedPuddingScore(sggs, playerId);
        return score;
    }

    /**
     * MODIFIED: estimated end-of-game pudding points for a player, from the puddings played so far.
     * Uses the same rule as the game (SGCard.Pudding.onGameEnd), with the values read from SGParameters:
     * - most puddings: valuePuddingMost (+6), split evenly (integer division) between tied players
     * - fewest puddings: valuePuddingLeast (-6), split the same way, only with more than 2 players
     * - if everyone has the same number of puddings, nobody scores
     * Puddings are not discarded between rounds, so the played counts include all rounds so far.
     *
     * @param gs       - Sushi Go game state
     * @param playerId - player to estimate for
     * @return - estimated pudding points for playerId
     */
    private double estimatedPuddingScore(SGGameState gs, int playerId) {
        SGParameters params = (SGParameters) gs.getGameParameters();
        int nPlayers = gs.getNPlayers();

        // Count every player's puddings and find the highest and lowest counts
        int[] puddings = new int[nPlayers];
        int most = Integer.MIN_VALUE, least = Integer.MAX_VALUE;
        for (int p = 0; p < nPlayers; p++) {
            puddings[p] = gs.getPlayedCardTypes()[p].get(SGCard.SGCardType.Pudding).getValue();
            most = Math.max(most, puddings[p]);
            least = Math.min(least, puddings[p]);
        }
        if (most == least) return 0;   // no difference in puddings: nobody scores

        int nMost = 0, nLeast = 0;
        for (int p = 0; p < nPlayers; p++) {
            if (puddings[p] == most) nMost++;
            if (puddings[p] == least) nLeast++;
        }

        double estimate = 0;
        if (puddings[playerId] == most)
            estimate += params.valuePuddingMost / nMost;
        if (puddings[playerId] == least && nPlayers > 2)
            estimate += params.valuePuddingLeast / nLeast;
        return estimate;
    }
    @Override
    public double minValue() {
        return Double.NEGATIVE_INFINITY;
    }
    @Override
    public double maxValue() {
        return Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        return "ScoreHeuristic";
    }
    @Override
    public boolean equals(Object obj) {
        return obj instanceof SpecialScoreHeuristic;
    }
    @Override
    public int hashCode() {
        return 3;
    }
}

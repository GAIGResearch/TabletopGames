package games.diamant;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class DiamantHeuristic implements IStateHeuristic {

    /**
     * Get the score of a player given the game state
     * The score is estimating taking into account the number of gems on the treasure chest of the player
     * with respect the gems of the other players.
     * If the player has more -> 1.0
     * Else if the player has less -> - 1.0
     * Else -> 0.0
     *
     * @param gs        - game state to evaluate and score.
     * @param playerId: player for whom we want to estimate the score
     * @return a value -1,0,1 indicating how good is to be in this game state
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];
        if (playerResult == Utils.GameResult.LOSE)
            return -1;
        if (playerResult == Utils.GameResult.WIN)
            return 1;

        DiamantGameState dgs = (DiamantGameState) gs;
        int max_ngens = 0;      // small number
        for (int i = 0; i < dgs.getNPlayers(); i++) {
            if (i != playerId) {
                int nGems = dgs.treasureChests.get(i).getValue();
                if (nGems > max_ngens)
                    max_ngens = nGems;
            }
        }

        int player_gems = dgs.treasureChests.get(playerId).getValue();
        double score = player_gems / 139.0; // so is 1.0 if a player has every single gem....

        if (player_gems == max_ngens) score += 0.05;

        return score;
    }
}

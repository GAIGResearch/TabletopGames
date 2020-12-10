package games.diamant;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;

public class DiamantHeuristic implements IStateHeuristic {

    /**
     * Get the score of a player given the game state
     * The score is estimating taking into account the number of gems on the treasure chest of the player
     * with respect the gems of the other players.
     * If the player has more -> 1.0
     * Else if the player has less -> - 1.0
     * Else -> 0.0
     * @param gs - game state to evaluate and score.
     * @param playerId: player for whom we want to estimate the score
     * @return a value -1,0,1 indicating how good is to be in this game state
     */
    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiamantGameState dgs = (DiamantGameState) gs;

        int max_ngens= 0;      // small number
        for (int i=0; i<dgs.getNPlayers(); i++)
        {
            if (i != playerId)
            {
                int nGems = dgs.treasureChests.get(i).GetNumberGems();
                if (nGems > max_ngens)
                    max_ngens = nGems;

            }
        }

        int player_gems = dgs.treasureChests.get(playerId).GetNumberGems();
        double score;
        if      (player_gems > max_ngens) score = 1.0;
        else if (player_gems < max_ngens) score = -1.0;
        else                              score = 0.0;
        return score;
    }
}

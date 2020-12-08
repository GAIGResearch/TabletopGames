package games.diamant;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Utils;

public class DiamantHeuristic implements IStateHeuristic {


    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        DiamantGameState dgs = (DiamantGameState) gs;

        int min_ngens = 1000;  // big number
        int max_ngens= 0;      // small number
        for (int i=0; i<dgs.getNPlayers(); i++)
        {
            int nGems = dgs.treasureChests.get(i).GetNumberGems();
            if (nGems > max_ngens)
                max_ngens = nGems;
            if (nGems < min_ngens)
                min_ngens = nGems;
        }

        return (dgs.treasureChests.get(playerId).GetNumberGems() - min_ngens) / (float) (max_ngens - min_ngens);
    }
}

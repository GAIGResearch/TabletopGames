package games.diamant;

import core.AbstractGameState;
import core.interfaces.IStateFeatureVector;

public class DiamantStateFeatures implements IStateFeatureVector {

    String[] names = new String[]{"BANKED POINTS", "CAVE POINTS", "NPLAYERS", "CAVES LEFT", "TRAP1", "TRAP2", "TRAP3", "TRAP4"
    , "TRAP5"};

    @Override
    public double[] featureVector(AbstractGameState state, int playerID) {
        DiamantGameState dgs = (DiamantGameState) state;
        double[] retVal = new double[names.length];
        retVal[0] = dgs.getTreasureChests().get(playerID).getValue();
        retVal[1] = dgs.nGemsOnPath;
        retVal[3] = dgs.playerInCave.size();
        retVal[4] = dgs.nCave;
        retVal[5] = dgs.nHazardExplosionsOnPath;
        retVal[6] = dgs.nHazardPoissonGasOnPath;
        retVal[7] = dgs.nHazardRockfallsOnPath;
        retVal[8] = dgs.nHazardScorpionsOnPath;
        retVal[9] = dgs.nHazardSnakesOnPath;
        return retVal;
    }

    @Override
    public String[] names() {
        return new String[0];
    }
}

package games.resistance;

import core.AbstractParameters;
import games.resistance.components.ResGameBoard;

public class ResParameters extends AbstractParameters {
    public String dataPath = "data/resistance/";

    public int[][] playersPerMission = {
            {2, 3, 2, 3, 3},
            {2, 3, 4, 3, 4},
            {2, 3, 3, 4, 4},
            {3, 4, 4, 5, 5},
            {3, 4, 4, 5, 5},
            {3, 4, 4, 5, 5}
    };

    public int[] spiesByPlayerCount = {2, 2, 3, 3, 3, 4};

    public int[] playersPerMission(int players) {
        return playersPerMission[players - 5];
    }

    public ResGameBoard getPlayerBoard(int numberPlayers) {
        return new ResGameBoard(playersPerMission(numberPlayers));
    }


    public int[] getFactions(int numberPlayers) {
        int[] factions = new int[2];
        factions[1] = spiesByPlayerCount[numberPlayers - 5];
        factions[0] = numberPlayers - factions[1];
        return factions;
    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        ResParameters retValue = new ResParameters();
        retValue.dataPath = dataPath;
        retValue.playersPerMission = playersPerMission.clone();
        retValue.spiesByPlayerCount = spiesByPlayerCount.clone();
        return retValue;
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ResParameters;
    }
}

package games.resistance;

import core.AbstractParameters;
import games.resistance.components.ResGameBoard;

import java.util.*;

public class ResParameters extends AbstractParameters {
    public String dataPath = "data/resistance/";

    public ResParameters(long seed) {
        super(seed);
    }

    public int getMaxRounds() {
        return 5;
    }

    public ResGameBoard getPlayerBoard(int numberPlayers) {
        if (numberPlayers == 5) {
            return new ResGameBoard(new int[]{2, 3, 2, 3, 3});
        }
        if (numberPlayers == 6) {
            return new ResGameBoard(new int[]{2, 3, 4, 3, 4});
        }
        if (numberPlayers == 7) {
            return new ResGameBoard(new int[]{2, 3, 3, 4, 4});
        }
        if (numberPlayers == 8 || numberPlayers == 9 || numberPlayers == 10) {
            return new ResGameBoard(new int[]{3, 4, 4, 5, 5});
        }
        throw new AssertionError("shouldn't be null, incorrect players:" + numberPlayers);
    }



    public int[] getFactions(int numberPlayers) {

        if (numberPlayers == 5) {
            return new int[]{3, 2};
        }
        if (numberPlayers == 6) {
            return new int[]{4, 2};
        }
        if (numberPlayers == 7) {
            return new int[]{4, 3};
        }
        if (numberPlayers == 8) {
            return new int[]{5, 3};
        }
        if (numberPlayers == 9) {
            return new int[]{6, 3};
        }
        if (numberPlayers == 10) {
            return new int[]{6, 4};
        }
        return null;

    }

    public String getDataPath() {
        return dataPath;
    }

    @Override
    protected AbstractParameters _copy() {
        return new ResParameters(System.currentTimeMillis());
    }

    @Override
    protected boolean _equals(Object o) {
        return o instanceof ResParameters;
    }
}

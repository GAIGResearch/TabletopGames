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

    public static List<Boolean> randomiseSpies(int spies, ResGameState state, int playerID) {
        // We want to randomly assign the number of spies across the total number of players
        // and return a boolean[] with length of total, and spies number of true values
        // we also need to ensure that there is at least one spy per historically failed mission
        boolean valid = true;
        int total = state.getNPlayers();
        boolean[] retValue;
        do {
            retValue = new boolean[state.getNPlayers()];
            for (int i = 0; i < spies; i++) {
                boolean done = false;
                int count = 0;
                while (!done) {
                    int rndIndex = state.rnd.nextInt(total);
                    if (!retValue[rndIndex] && rndIndex != playerID) {
                        retValue[rndIndex] = true;
                        done = true;
                    }
                    count++;
                    if (count > 200)
                        throw new AssertionError(String.format("Infinite loop allocating %d spies amongst %d players", spies, total));
                }
                // now check constraints
                valid = true;
                for (List<Integer> failedMission : state.getFailedTeams()) {
                    boolean[] finalRetValue = retValue;
                    if (failedMission.stream().noneMatch(s -> finalRetValue[s])) {
                        // we have a failed team without a spy
                        valid = false;
                    }
                    if (!valid)
                        break;
                }
            }
        } while (!valid);
        List<Boolean> RV = new ArrayList<>();
        for (boolean b : retValue) {
            RV.add(b);
        }
        return RV;
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

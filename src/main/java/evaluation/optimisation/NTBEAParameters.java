package evaluation.optimisation;

import core.AbstractParameters;
import games.GameType;

import java.util.Arrays;
import java.util.stream.IntStream;

import static utilities.Utils.getArg;

public class NTBEAParameters {

    public NTBEAParameters(String[] args) {

    }

    public boolean tuningGame;
    public int iterationsPerRun;
    public int repeats;
    public int evalGame;
    public double kExplore;
    public int tournamentGames;
    public int neighbourhoodSize;
    public String opponentDescriptor;
    public String evalMethod;
}

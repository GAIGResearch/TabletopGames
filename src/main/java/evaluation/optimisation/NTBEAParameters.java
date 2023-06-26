package evaluation.optimisation;

import core.AbstractParameters;
import games.GameType;
import utilities.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.IntStream;

import static utilities.Utils.getArg;

public class NTBEAParameters {

    public enum Mode {
        NTBEA, MultiNTBEA, CoopNTBEA
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
    public boolean useThreeTuples;
    public long seed;
    public boolean verbose;
    public Mode mode;
    public String logFile;
    public List<String> listenerClasses;
    public String destDir;

    public NTBEAParameters(String[] args) {
        tuningGame = getArg(args, "tuneGame", false);
        iterationsPerRun = getArg(args, "nGames", 1000);
        repeats = getArg(args, "repeats", 10);
        evalGame = getArg(args, "evalGame", iterationsPerRun / 5);
        kExplore = getArg(args, "kExplore", 1.0);
        tournamentGames = getArg(args, "tournamentGames", 0);
        neighbourhoodSize = getArg(args, "neighbourhoodSize", 100);
        opponentDescriptor = getArg(args, "opponent", "");
        evalMethod = getArg(args, "evalMethod", "Win");
        useThreeTuples = getArg(args, "useThreeTuples", false);
        seed = getArg(args, "seed", System.currentTimeMillis());
        verbose = getArg(args, "verbose", false);
        mode = getArg(args, "mode", Mode.NTBEA);
        logFile = getArg(args, "logFile", "NTBEA.log");
        listenerClasses = new ArrayList<>(Arrays.asList(getArg(args, "listener", "").split("\\|")));
        destDir = getArg(args, "destDir", "NTBEA");
        if (tuningGame && opponentDescriptor.equals("")) {
            throw new IllegalArgumentException("Must specify opponent descriptor when tuning a game");
        }
    }

}

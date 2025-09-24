package evaluation.optimisation;

import core.AbstractParameters;
import core.interfaces.ITunableParameters;
import evaluation.RunArg;
import evaluation.optimisation.ntbea.SearchSpace;
import games.GameType;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static utilities.JSONUtils.parser;

public class NTBEAParameters extends TunableParameters<NTBEA> {

    public enum Mode {
        NTBEA, CoopNTBEA, StableNTBEA
    }

    // variables that are tunable
    public int iterationsPerRun;
    public int repeats;
    public int budget;
    public int evalGames;
    public double kExplore;
    public int tournamentGames;
    public int neighbourhoodSize;
    public String opponentDescriptor;
    public int seed;
    public String evalMethod;
    public boolean useThreeTuples;
    public boolean useTwoTuples;
    public boolean useNTuples;
    public double noiseMeanType;
    public boolean simpleRegret;
    public boolean verbose;
    public Mode mode;
    public int quantile = 0;
    public int evaluationsPerTrial = 1;
    public int OSDBudget = 0;
    public boolean OSDTournament = false;
    public double OSDConfidence = 0.9;

    // and those that are not (so must be included separately in copy etc)
    public boolean tuningGame = false;
    public String logFile = "NTBEA.log";
    public List<String> listenerClasses = Collections.emptyList();
    public String destDir = "NTBEA";
    public SearchSpace searchSpace;
    public AbstractParameters gameParams;
    public boolean byTeam = false;
    public GameType gameType;
    public int nPlayers;

    public NTBEAParameters() {
        addTunableParameter("iterations", 1000);
        addTunableParameter("repeats", 5);
        addTunableParameter("budget", 50);
        addTunableParameter("evalGames", -1);
        addTunableParameter("kExplore", 1.0);
        addTunableParameter("matchups", 1000);
        addTunableParameter("neighbourhood", 50);
        addTunableParameter("opponentDescriptor", "random");
        addTunableParameter("seed", (int) System.currentTimeMillis());
        addTunableParameter("evalMethod", "Win");
        addTunableParameter("useThreeTuples", false);
        addTunableParameter("useTwoTuples", true);
        addTunableParameter("useNTuples", false);
        addTunableParameter("noiseCombination", 1.0);
        addTunableParameter("simpleRegret", false);
        addTunableParameter("verbose", false);
        addTunableParameter("mode", Mode.NTBEA);
        addTunableParameter("quantile", 0);
        addTunableParameter("evalsPerTrial", 1);
        addTunableParameter("OSDBudget", 0);
        addTunableParameter("OSDTournament", false);
        addTunableParameter("OSDConfidence", 0.9);
    }

    @Override
    public void _reset() {
        iterationsPerRun = (int) getParameterValue("iterations");
        repeats = (int) getParameterValue("repeats");
        budget = (int) getParameterValue("budget");
        evalGames = (int) getParameterValue("evalGames");
        kExplore = (double) getParameterValue("kExplore");
        tournamentGames = (int) getParameterValue("matchups");
        neighbourhoodSize = (int) getParameterValue("neighbourhood");
        opponentDescriptor = (String) getParameterValue("opponentDescriptor");
        seed = (int) getParameterValue("seed");
        evalMethod = (String) getParameterValue("evalMethod");
        useThreeTuples = (boolean) getParameterValue("useThreeTuples");
        useTwoTuples = (boolean) getParameterValue("useTwoTuples");
        useNTuples = (boolean) getParameterValue("useNTuples");
        noiseMeanType = (double) getParameterValue("noiseCombination");
        simpleRegret = (boolean) getParameterValue("simpleRegret");
        verbose = (boolean) getParameterValue("verbose");
        mode = (Mode) getParameterValue("mode");
        quantile = (int) getParameterValue("quantile");
        evaluationsPerTrial = (int) getParameterValue("evalsPerTrial");
        OSDBudget = (int) getParameterValue("OSDBudget");
        OSDTournament = (boolean) getParameterValue("OSDTournament");
        OSDConfidence = (double) getParameterValue("OSDConfidence");

        if (evalGames == -1) evalGames = iterationsPerRun / 5;
    }

    // Now we need to use args to provide the searchSpace and other non-tunable parameters
    public NTBEAParameters(Map<RunArg, Object> args) {
        this();
        setParameterValue("iterations", args.get(RunArg.iterations));
        setParameterValue("repeats", args.get(RunArg.repeats));
        setParameterValue("budget", args.get(RunArg.budget));
        setParameterValue("evalGames", args.get(RunArg.evalGames));
        setParameterValue("kExplore", args.get(RunArg.kExplore));
        setParameterValue("matchups", args.get(RunArg.matchups));
        setParameterValue("neighbourhood", args.get(RunArg.neighbourhood));
        setParameterValue("opponentDescriptor", args.get(RunArg.opponent));
        setParameterValue("seed", args.get(RunArg.seed));
        setParameterValue("evalMethod", args.get(RunArg.evalMethod));
        setParameterValue("useThreeTuples", args.get(RunArg.useThreeTuples));
        setParameterValue("useTwoTuples", args.get(RunArg.useTwoTuples));
        setParameterValue("useNTuples", args.get(RunArg.useNTuples));
        setParameterValue("noiseCombination", args.get(RunArg.noiseCombination));
        setParameterValue("simpleRegret", args.get(RunArg.simpleRegret));
        setParameterValue("verbose", args.get(RunArg.verbose));
        setParameterValue("mode", Mode.valueOf(args.get(RunArg.NTBEAMode).toString()));
        setParameterValue("quantile", args.get(RunArg.quantile));
        setParameterValue("evalsPerTrial", args.get(RunArg.evalsPerTrial));
        setParameterValue("OSDBudget", args.get(RunArg.OSDBudget));
        setParameterValue("OSDTournament", args.get(RunArg.OSDTournament));
        setParameterValue("OSDConfidence", args.get(RunArg.OSDConfidence));

        _reset();

        // then the non-tunable parameters
        tuningGame = (boolean) args.get(RunArg.tuneGame);
        byTeam = (boolean) args.get(RunArg.byTeam);
        gameType = GameType.valueOf(args.get(RunArg.game).toString());
        nPlayers = (int) args.get(RunArg.nPlayers);
        gameParams = args.get(RunArg.gameParams).equals("") ? null :
                AbstractParameters.createFromFile(gameType, (String) args.get(RunArg.gameParams));

        listenerClasses = (List<String>) args.get(RunArg.listener);
        destDir = (String) args.get(RunArg.destDir);
        if (destDir.isEmpty()) destDir = "NTBEA";
        if (tuningGame && opponentDescriptor.isEmpty()) {
            throw new IllegalArgumentException("Must specify opponent descriptor when tuning a game");
        }

        String searchSpaceFile =  (String) args.get(RunArg.searchSpace);
        if (!searchSpaceFile.equals("functionTest")) {
            boolean fileExists = (new File(searchSpaceFile)).exists();
            JSONObject json = null;
            try {
                String className = searchSpaceFile;
                Constructor<ITunableParameters<?>> constructor;
                if (fileExists) {
                    // We import the file as a JSONObject
                    json = JSONUtils.loadJSONFile(searchSpaceFile);
                    className = (String) json.get("class");
                    if (className == null) {
                        System.out.println("No class property found in SearchSpaceJSON file. This is required to specify the ITunableParameters class that the file complements");
                        return;
                    }
                }
                Class<ITunableParameters<?>> itpClass = (Class<ITunableParameters<?>>) Class.forName(className);
                constructor = itpClass.getConstructor();
                ITunableParameters<?> itp = constructor.newInstance();
                // We then initialise the ITPSearchSpace with this ITP and the JSON details
                searchSpace = fileExists ? new ITPSearchSpace(itp, json) : new ITPSearchSpace(itp);
            } catch (Exception e) {
                e.printStackTrace();
                throw new AssertionError(e.getClass() + " : " + e.getMessage() + "\nError loading ITunableParameters class in " + searchSpaceFile);
            }
        }
    }

    public void printSearchSpaceDetails() {

        int searchSpaceSize = IntStream.range(0, searchSpace.nDims()).reduce(1, (acc, i) -> acc * searchSpace.nValues(i));
        int twoTupleSize = IntStream.range(0, searchSpace.nDims() - 1)
                .map(i -> searchSpace.nValues(i) *
                        IntStream.range(i + 1, searchSpace.nDims())
                                .map(searchSpace::nValues).sum()
                ).sum();
        int threeTupleSize = IntStream.range(0, searchSpace.nDims() - 2)
                .map(i -> searchSpace.nValues(i) *
                        IntStream.range(i + 1, searchSpace.nDims()).map(j ->
                                searchSpace.nValues(j) * IntStream.range(j + 1, searchSpace.nDims())
                                        .map(searchSpace::nValues).sum()
                        ).sum()
                ).sum();

        System.out.printf("Search space consists of %d states and %d possible 2-Tuples%s%n",
                searchSpaceSize, twoTupleSize, useThreeTuples ? String.format(" and %d 3-Tuples", threeTupleSize) : "");

        for (int i = 0; i < searchSpace.nDims(); i++) {
            int finalI = i;
            String allValues = IntStream.range(0, searchSpace.nValues(i))
                    .mapToObj(j -> searchSpace.value(finalI, j))
                    .map(Object::toString)
                    .collect(joining(", "));
            System.out.printf("%30s has %d values %s%n", searchSpace.name(i), searchSpace.nValues(i), allValues);
        }
    }


    @Override
    public NTBEA instantiate() {
        return new NTBEA(this, gameType, nPlayers);
    }

    @Override
    protected NTBEAParameters _copy() {
        NTBEAParameters ntp = new NTBEAParameters();
        ntp.searchSpace = searchSpace;
        ntp.gameParams = gameParams == null ? null : gameParams.copy();
        ntp.tuningGame = tuningGame;
        ntp.byTeam = byTeam;
        ntp.listenerClasses = listenerClasses;
        ntp.destDir = destDir;
        ntp.gameType = gameType;
        ntp.nPlayers = nPlayers;
        ntp.logFile = logFile;
        return ntp;
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof NTBEAParameters parameters) {
            return searchSpace.equals(parameters.searchSpace) &&
                    ((gameParams == null && parameters.gameParams == null) || gameParams.equals(parameters.gameParams)) &&
                    tuningGame == parameters.tuningGame &&
                    byTeam == parameters.byTeam &&
                    listenerClasses.equals(parameters.listenerClasses) &&
                    destDir.equals(parameters.destDir) &&
                    gameType.equals(parameters.gameType) &&
                    logFile.equals(parameters.logFile) &&
                    nPlayers == parameters.nPlayers;
        }
        return false;
    }

}

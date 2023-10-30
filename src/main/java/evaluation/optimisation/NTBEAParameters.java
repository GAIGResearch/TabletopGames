package evaluation.optimisation;

import core.AbstractParameters;
import core.interfaces.ITunableParameters;
import evaluation.RunArg;
import games.GameType;
import org.json.simple.JSONObject;
import utilities.JSONUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.IntStream;

import static evaluation.RunArg.gameParams;
import static java.util.stream.Collectors.joining;
import static utilities.JSONUtils.parser;
import static utilities.Utils.*;

public class NTBEAParameters {

    public enum Mode {
        NTBEA, MultiNTBEA, CoopNTBEA
    }

    public boolean tuningGame;
    public int iterationsPerRun;
    public int repeats;
    public int evalGames;
    public double kExplore;
    public int tournamentGames;
    public int neighbourhoodSize;
    public String opponentDescriptor;
    public long seed;
    public String evalMethod;
    public boolean useThreeTuples;
    public boolean verbose;
    public Mode mode;
    public String logFile;
    public List<String> listenerClasses;
    public String destDir;
    public ITPSearchSpace searchSpace;
    public AbstractParameters gameParams;
    public boolean byTeam;

    public NTBEAParameters(Map<RunArg, Object> args) {
        this(args, Function.identity());
    }

    public NTBEAParameters(Map<RunArg, Object> args, Function<String, String> preprocessor) {
        tuningGame = (boolean) args.get(RunArg.tuneGame);
        iterationsPerRun = (int) args.get(RunArg.iterations);
        repeats = (int) args.get(RunArg.repeats);
        tournamentGames = (int) args.get(RunArg.matchups);
        evalGames = (int) args.get(RunArg.evalGames);
        if (evalGames == -1) evalGames = iterationsPerRun / 5;
        kExplore = (double) args.get(RunArg.kExplore);
        neighbourhoodSize = (int) args.get(RunArg.neighbourhood);
        opponentDescriptor = (String) args.get(RunArg.opponent);
        evalMethod = (String) args.get(RunArg.evalMethod);
        useThreeTuples = (boolean) args.get(RunArg.useThreeTuples);
        verbose = (boolean) args.get(RunArg.verbose);
        seed = args.get(RunArg.seed) instanceof Long ? ((Long)args.get(RunArg.seed)).intValue() : (int) args.get(RunArg.seed)  ;
        byTeam = (boolean) args.get(RunArg.byTeam);
        GameType game = GameType.valueOf(args.get(RunArg.game).toString());
        gameParams = args.get(RunArg.gameParams).equals("") ? null :
                AbstractParameters.createFromFile(game, (String) args.get(RunArg.gameParams));

        mode = Mode.valueOf((String) args.get(RunArg.NTBEAmode));
        logFile = (String) args.get(RunArg.output);
        if (logFile.isEmpty()) logFile = "NTBEA.log";
        listenerClasses = (List<String>) args.get(RunArg.listener);
        destDir = (String) args.get(RunArg.destDir);
        if (destDir.isEmpty()) destDir = "NTBEA";
        if (tuningGame && opponentDescriptor.equals("")) {
            throw new IllegalArgumentException("Must specify opponent descriptor when tuning a game");
        }

        String searchSpaceFile =  (String) args.get(RunArg.searchSpace);
        boolean fileExists = (new File(searchSpaceFile)).exists();
        JSONObject json = null;
        try {
            String className = searchSpaceFile;
            Constructor<ITunableParameters> constructor;
            if (fileExists) {
                // We import the file as a JSONObject
                String rawJSON = JSONUtils.readJSONFile(searchSpaceFile, preprocessor);
                json = (JSONObject) parser.parse(rawJSON);
                className = (String) json.get("class");
                if (className == null) {
                    System.out.println("No class property found in SearchSpaceJSON file. This is required to specify the ITunableParameters class that the file complements");
                    return;
                }
            }
            Class<ITunableParameters> itpClass = (Class<ITunableParameters>) Class.forName(className);
            constructor = itpClass.getConstructor();
            ITunableParameters itp = constructor.newInstance();
            // We then initialise the ITPSearchSpace with this ITP and the JSON details
            searchSpace = fileExists ? new ITPSearchSpace(itp, json) : new ITPSearchSpace(itp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getClass() + " : " + e.getMessage() + "\nError loading ITunableParameters class in " + searchSpaceFile);
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

}

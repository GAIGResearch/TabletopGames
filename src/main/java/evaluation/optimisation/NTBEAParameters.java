package evaluation.optimisation;

import core.interfaces.ITunableParameters;
import org.json.simple.JSONObject;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
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
    public String evalMethod;
    public boolean useThreeTuples;
    public long seed;
    public boolean verbose;
    public Mode mode;
    public String logFile;
    public List<String> listenerClasses;
    public String destDir;
    public ITPSearchSpace searchSpace;


    public NTBEAParameters(String[] args) {
        this(args, Function.identity());
    }
    public NTBEAParameters(String[] args, Function<String, String> preprocessor) {
        tuningGame = getArg(args, "tuneGame", false);
        iterationsPerRun = getArg(args, "nGames", 1000);
        repeats = getArg(args, "repeats", 10);
        evalGames = getArg(args, "evalGame", iterationsPerRun / 5);
        kExplore = getArg(args, "kExplore", 1.0);
        tournamentGames = getArg(args, "tournamentGames", 0);
        neighbourhoodSize = getArg(args, "neighbourhoodSize", 50);
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

        String searchSpaceFile = getArg(args, "searchSpace", "");
        boolean fileExists = (new File(searchSpaceFile)).exists();
        JSONObject json = null;
        try {
            String className = searchSpaceFile;
            Constructor<ITunableParameters> constructor;
            if (fileExists) {
                // We import the file as a JSONObject
                String rawJSON = readJSONFile(searchSpaceFile, preprocessor);
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
            throw new AssertionError(e.getClass() + " : " + e.getMessage() + "Error loading ITunableParameters class in " + args[0]);
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

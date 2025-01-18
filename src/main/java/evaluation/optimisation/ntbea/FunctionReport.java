package evaluation.optimisation.ntbea;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.interfaces.IGameHeuristic;
import evaluation.RunArg;
import evaluation.optimisation.*;
import games.GameType;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import players.PlayerFactory;
import players.heuristics.OrdinalPosition;
import players.heuristics.PureScoreHeuristic;
import players.heuristics.WinOnlyHeuristic;
import utilities.JSONUtils;
import utilities.Pair;
import utilities.StatSummary;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.IntStream;

import static evaluation.RunArg.*;

public class FunctionReport {

    public static void main(String[] args) {
        List<String> argsList = Arrays.asList(args);
        if (argsList.contains("--help") || argsList.contains("-h")) {
            RunArg.printHelp(Usage.ParameterSearch);
            return;
        }

        // Config
        Map<RunArg, Object> config = parseConfig(args, Collections.singletonList(Usage.ParameterSearch));

        String setupFile = config.getOrDefault(RunArg.config, "").toString();
        if (!setupFile.isEmpty()) {
            // Read from file instead
            try {
                FileReader reader = new FileReader(setupFile);
                JSONParser parser = new JSONParser();
                JSONObject json = (JSONObject) parser.parse(reader);
                config = parseConfig(json, Usage.ParameterSearch);
            } catch (FileNotFoundException ignored) {
                throw new AssertionError("Config file not found : " + setupFile);
                //    parseConfig(runGames, args);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }

        config.put(searchSpace, "functionTest");
        config.put(game, "TicTacToe");
        NTBEAParameters params = new NTBEAParameters(config);
        NTBEAFunction function = new TestFunction002();
        params.searchSpace = new FunctionSearchSpace((Integer) config.get(nPlayers),  function);
        FunctionEvaluator evaluator = new FunctionEvaluator(function, params.searchSpace);

        params.printSearchSpaceDetails();

// Now initialise the other bits and pieces needed for the NTBEA package
        NTupleSystem landscapeModel = new NTupleSystem(params.searchSpace, params.kExplore);
        landscapeModel.use3Tuple = params.useThreeTuples;
        landscapeModel.useNTuple = params.useNTuples;
        landscapeModel.simpleRegret = params.simpleRegret;
        landscapeModel.generalisedMeanCoefficient = params.noiseMeanType;
        landscapeModel.addTuples();

        NTupleBanditEA searchFramework = new NTupleBanditEA(landscapeModel, params.neighbourhoodSize);

        double[] valuesFound = new double[params.repeats];

        for (int currentIteration = 0; currentIteration < params.repeats; currentIteration++) {
            landscapeModel.reset();

            evaluator.reset();
            searchFramework.runTrial(evaluator, params.iterationsPerRun);

            if (params.verbose)
                logResults(landscapeModel, params);

            int[] thisWinnerSettings = landscapeModel.getBestSampled();
            valuesFound[currentIteration] = evaluator.actualBaseValue(thisWinnerSettings);
        }

        // Now print out details
        StatSummary summary = new StatSummary();
        summary.add(valuesFound);
        System.out.printf("Function: %20s Max: %.3f, Mean: %.3f, Min: %.3f, SD: %.3f%n",
                function.getClass().getSimpleName(), summary.max(),
                summary.mean(), summary.min(), summary.sd());
    }


    private static void logResults(NTupleSystem landscapeModel, NTBEAParameters params) {

        System.out.println("Current best sampled point (using mean estimate): " +
                Arrays.toString(landscapeModel.getBestSampled()) +
                String.format(", %.3g", landscapeModel.getMeanEstimate(landscapeModel.getBestSampled())));

        String tuplesExploredBySize = Arrays.toString(IntStream.rangeClosed(1, params.searchSpace.nDims())
                .map(size -> landscapeModel.getTuples().stream()
                        .filter(t -> t.tuple.length == size)
                        .mapToInt(it -> it.ntMap.size())
                        .sum()
                ).toArray());

        System.out.println("Tuples explored by size: " + tuplesExploredBySize);
        System.out.printf("Summary of 1-tuple statistics after %d samples:%n", landscapeModel.numberOfSamples());

        IntStream.range(0, params.searchSpace.nDims()) // assumes that the first N tuples are the 1-dimensional ones
                .mapToObj(i -> new Pair<>(params.searchSpace.name(i), landscapeModel.getTuples().get(i)))
                .forEach(nameTuplePair ->
                        nameTuplePair.b.ntMap.keySet().stream().sorted().forEach(k -> {
                            StatSummary v = nameTuplePair.b.ntMap.get(k);
                            System.out.printf("\t%20s\t%s\t%d trials\t mean %.3g +/- %.2g%n", nameTuplePair.a, k, v.n(), v.mean(), v.stdErr());
                        })
                );

        System.out.println("\nSummary of 10 most tried full-tuple statistics:");
        landscapeModel.getTuples().stream()
                .filter(t -> t.tuple.length == params.searchSpace.nDims())
                .forEach(t -> t.ntMap.keySet().stream()
                        .map(k -> new Pair<>(k, t.ntMap.get(k)))
                        .sorted(Comparator.comparing(p -> -p.b.n()))
                        .limit(10)
                        .forEach(item ->
                                System.out.printf("\t%s\t%d trials\t mean %.3g +/- %.2g\t(NTuple estimate: %.3g)%n",
                                        item.a, item.b.n(), item.b.mean(), item.b.stdErr(), landscapeModel.getMeanEstimate(item.a.v))
                        )
                );


    }
}

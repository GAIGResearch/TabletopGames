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
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }

        config.put(searchSpace, "functionTest");
        config.put(game, "TicTacToe");
        NTBEAParameters params = new NTBEAParameters(config);
        params.searchSpace = new FunctionSearchSpace((Integer) config.get(discretisation),  (NTBEAFunction) config.get(function));
        FunctionEvaluator evaluator = new FunctionEvaluator((NTBEAFunction) config.get(function), params.searchSpace);

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
                landscapeModel.logResults(params);

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
}

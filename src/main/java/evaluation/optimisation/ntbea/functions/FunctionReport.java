package evaluation.optimisation.ntbea.functions;

import evaluation.RunArg;
import evaluation.optimisation.*;
import evaluation.optimisation.ntbea.NTupleBanditEA;
import evaluation.optimisation.ntbea.NTupleSystem;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.StatSummary;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

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
                config = parseConfig(json, Usage.ParameterSearch, true);
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
        NTupleSystem landscapeModel = new NTupleSystem(params);
        NTupleBanditEA searchFramework = new NTupleBanditEA(landscapeModel, params);

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

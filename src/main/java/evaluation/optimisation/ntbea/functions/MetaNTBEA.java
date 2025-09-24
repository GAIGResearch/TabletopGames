package evaluation.optimisation.ntbea.functions;

import evaluation.RunArg;
import evaluation.optimisation.NTBEA;
import evaluation.optimisation.NTBEAParameters;
import evaluation.optimisation.ntbea.NTBEAEvaluator;
import evaluation.optimisation.ntbea.NTupleBanditEA;
import evaluation.optimisation.ntbea.NTupleSystem;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Pair;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.IntStream;

import static evaluation.RunArg.*;

public class MetaNTBEA {

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
                //    parseConfig(runGames, args);
            } catch (IOException | ParseException e) {
                throw new RuntimeException(e);
            }
        }

        config.put(game, "TicTacToe");
        NTBEAParameters params = new NTBEAParameters(config);

        NTBEAEvaluator evaluator = new NTBEAEvaluator((NTBEAFunction) config.get(function),
                (int) config.get(discretisation),
                params.searchSpace,
                params);

        params.printSearchSpaceDetails();

// Now initialise the other bits and pieces needed for the NTBEA package
        NTupleSystem landscapeModel = new NTupleSystem(params);

        NTupleBanditEA searchFramework = new NTupleBanditEA(landscapeModel, params);

        double bestValue = Double.NEGATIVE_INFINITY;
        int[] bestSettings = new int[params.searchSpace.nDims()];

        for (int currentIteration = 0; currentIteration < params.repeats; currentIteration++) {
            landscapeModel.reset();

            evaluator.reset();
            searchFramework.runTrial(evaluator, params.iterationsPerRun);

            if (params.verbose)
                landscapeModel.logResults(params);

            // we now use evalGames to get a better estimate of the value
            // the NTBEAEvaluator here returns the actual underlying value of the single recommended setting
            double[] results = IntStream.range(0, params.evalGames)
                    .mapToDouble(answer -> evaluator.evaluate(landscapeModel.getBestSampled())).toArray();
            Arrays.sort(results);

            double avg = Arrays.stream(results).average().orElse(0.0);
            double quantileValue = results[(int) (results.length * params.quantile / 100.0)];
            double stdErr = Math.sqrt(Arrays.stream(results).map(d -> Math.pow(d - avg, 2.0)).sum()) / (params.evalGames - 1.0);
            Pair<Pair<Double, Double>, int[]> resultToReport = (params.quantile > 0) ?
                    new Pair<>(new Pair<>(quantileValue, avg), landscapeModel.getBestSampled()) :
                    new Pair<>(new Pair<>(avg, stdErr), landscapeModel.getBestSampled());

            NTBEA.logSummary(resultToReport, params);

            if (resultToReport.a.a > bestValue) {
                bestValue = resultToReport.a.a;
                System.arraycopy(landscapeModel.getBestSampled(), 0, bestSettings, 0, bestSettings.length);
            }
        }

        // print out final best settings
        System.out.println("Best settings found: " + Arrays.toString(bestSettings));
        for (int i = 0; i < bestSettings.length; i++) {
            System.out.println(params.searchSpace.name(i) + " : " + params.searchSpace.value(i, bestSettings[i]));
        }
    }

}

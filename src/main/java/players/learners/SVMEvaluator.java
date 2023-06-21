package players.learners;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import core.interfaces.IStateFeatureVector;
import core.interfaces.ITunableParameters;
import evaluation.ITPSearchSpace;
import evaluation.ParameterSearch;
import evodef.SearchSpace;
import evodef.SolutionEvaluator;
import games.GameType;
import libsvm.svm_parameter;
import ntbea.NTupleBanditEA;
import ntbea.NTupleSystem;
import org.json.simple.JSONObject;
import players.PlayerFactory;
import players.heuristics.LeaderHeuristic;
import players.heuristics.SVMStateHeuristic;
import players.mcts.MCTSPlayer;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;
import static utilities.Utils.getArg;
import static utilities.Utils.loadJSONFile;

public class SVMEvaluator implements SolutionEvaluator {

    GameType game;
    AbstractParameters gameParams;
    MCTSPlayer tuningPlayer;
    ITPSearchSpace searchSpace;
    int nPlayers, gamesPerEval;
    AbstractPlayer opponent;
    Random rnd;
    IStateFeatureVector phiFn;
    String trainingData;
    int nEvals = 0;

    public SVMEvaluator(GameType game, ITPSearchSpace parametersToTune,
                        AbstractParameters gameParams,
                        int nPlayers,
                        AbstractPlayer opponent,
                        MCTSPlayer svmPlayer,
                        long seed, int gamesPerEval,
                        String trainingData,
                        IStateFeatureVector statePhi
    ) {
        this.game = game;
        this.gameParams = gameParams;
        this.searchSpace = parametersToTune;
        this.nPlayers = nPlayers;
        this.opponent = opponent;
        this.rnd = new Random(seed);
        this.phiFn = statePhi;
        this.trainingData = trainingData;
        this.gamesPerEval = gamesPerEval;
        this.tuningPlayer = svmPlayer;
    }

    @Override
    public void reset() {
        nEvals = 0;
    }

    @Override
    public double evaluate(double[] solution) {
        throw new AssertionError("No need for implementation according to NTBEA library javadoc");
    }

    @Override
    public double evaluate(int[] solution) {

        // why run new games each time....
        // we just have a directory of training data!

        // The crucial section
        // we instantiate the SVM
        long startTime = System.currentTimeMillis();
        svm_parameter params = (svm_parameter) searchSpace.getAgent(solution);
        SVMLearner learner = new SVMLearner(params);

        // train it on the available data
        learner.learnFrom(trainingData);
        // then create the SVMHeuristic to use the trained SVM

        long trainTime = System.currentTimeMillis();
        SVMStateHeuristic heuristic = new SVMStateHeuristic(phiFn, learner.model, new LeaderHeuristic());

        tuningPlayer.setStateHeuristic(heuristic);


        double meanResult = 0.0;
        // We can reduce variance here by cycling the playerIndex on each iteration
        for (int gameLoop = 0; gameLoop < gamesPerEval; gameLoop++) {
            int playerIndex = (nEvals + gameLoop) % nPlayers;
            List<AbstractPlayer> allPlayers = new ArrayList<>(nPlayers);
            int count = 0;
            for (int i = 0; i < nPlayers; i++) {
                if (i != playerIndex) {
                    count = (count + 1) % nPlayers;
                    allPlayers.add(opponent.copy());
                } else {
                    allPlayers.add(tuningPlayer);
                }
            }

            Game newGame = game.createGameInstance(nPlayers, gameParams);
            // always reset the random seed for each new game
            newGame.reset(allPlayers, rnd.nextLong());
            newGame.run();

            int result = newGame.getGameState().getOrdinalPosition(playerIndex);
            meanResult += result;
        }
        long endTime = System.currentTimeMillis();
        System.out.printf("Training: %d, Evaluating: %d, Result: %.2f%n", trainTime - startTime, endTime - trainTime, meanResult / gamesPerEval);
        nEvals++;
        return -meanResult / gamesPerEval;
    }

    @Override
    public SearchSpace searchSpace() {
        return searchSpace;
    }

    @Override
    public int nEvals() {
        return nEvals;
    }

    public static void main(String[] args) {

        // TODO : Once working refactor to avoid colossal code duplication with ParameterSearch

        // Create the SearchSpace, and report some useful stuff to the console.
        ITPSearchSpace searchSpace;
        boolean fileExists = (new File(args[0])).exists();
        try {
            String className = args[0];
            Constructor<ITunableParameters> constructor;
            JSONObject json = null;
            if (fileExists) {
                // We import the file as a JSONObject
                json = loadJSONFile(args[0]);
                className = (String) json.get("class");
                if (className == null)
                    throw new AssertionError("No class property found in JSON file. This is required to specify the ITunableParameters class that the file complements");
            }
            // we pull in the ITP referred to in the JSON file, or directly as args[0]
            Class<ITunableParameters> itpClass = (Class<ITunableParameters>) Class.forName(className);
            constructor = itpClass.getConstructor();
            ITunableParameters itp = constructor.newInstance();
            // We then initialise the ITPSearchSpace with this ITP and the JSON details
            searchSpace = fileExists ? new ITPSearchSpace(itp, json) : new ITPSearchSpace(itp);
        } catch (Exception e) {
            e.printStackTrace();
            throw new AssertionError(e.getClass() + " : " + e.getMessage() + "Error loading ITunableParameters class in " + args[0]);
        }

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

        boolean useThreeTuples = Arrays.asList(args).contains("useThreeTuples");

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

        // Now initialise the other bits and pieces needed for the NTBEA package
        NTupleSystem landscapeModel = new NTupleSystem(searchSpace);
        landscapeModel.setUse3Tuple(useThreeTuples);
        landscapeModel.addTuples();


        int iterationsPerRun = Integer.parseInt(args[1]);
        GameType game = GameType.valueOf(args[2]);
        int repeats = getArg(args, "repeat", 1);
        int evalGames = getArg(args, "evalGames", iterationsPerRun / 5);
        double kExplore = getArg(args, "kExplore", 1.0);
        String opponentDescriptor = getArg(args, "opponent", "");
        AbstractPlayer opponent = PlayerFactory.createPlayer(opponentDescriptor);
        MCTSPlayer player = (MCTSPlayer) PlayerFactory.createPlayer(getArg(args, "player", ""));
        IStateFeatureVector statePhi = Utils.loadClassFromString(getArg(args, "phi", ""));

        boolean verbose = Arrays.asList(args).contains("verbose");
        int nPlayers = getArg(args, "nPlayers", game.getMinPlayers());
        long seed = getArg(args, "seed", System.currentTimeMillis());
        String logfile = getArg(args, "logFile", "");
        String paramFile = getArg(args, "gameParam", "");
        AbstractParameters gameParams = AbstractParameters.createFromFile(game, paramFile);
        int gamePerEval = getArg(args, "gamesPerEval", 10);
        int hood = getArg(args, "hood", Math.min(50, searchSpaceSize / 100));
        String trainingData = getArg(args, "trainingData", "");

        NTupleBanditEA searchFramework = new NTupleBanditEA(landscapeModel, kExplore, hood);

        // Set up opponents

        SVMEvaluator evaluator = new SVMEvaluator(
                game,
                searchSpace,
                gameParams,
                nPlayers,
                opponent,
                player,
                seed,
                gamePerEval,
                trainingData,
                statePhi
        );

        // Get the results. And then log them.
        // This loops once for each complete repetition of NTBEA specified.
        // runNTBEA runs a complete set of trials, and spits out the mean and std error on the mean of the best sampled result
        // These mean statistics are calculated from the evaluation trials that are run after NTBEA is complete. (evalGames)
        Pair<Pair<Double, Double>, int[]> bestResult = new Pair<>(new Pair<>(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY), new int[0]);
        for (int mainLoop = 0; mainLoop < repeats; mainLoop++) {
            landscapeModel.reset();
            Pair<Double, Double> r = ParameterSearch.runNTBEA(evaluator, null, searchFramework, iterationsPerRun, iterationsPerRun, evalGames, verbose);
            Pair<Pair<Double, Double>, int[]> retValue = new Pair<>(r, Arrays.stream(landscapeModel.getBestOfSampled()).mapToInt(i -> (int) i).toArray());
            ParameterSearch.printDetailsOfRun(retValue, searchSpace, logfile, false, null);
            if (retValue.a.a > bestResult.a.a)
                bestResult = retValue;

        }
        System.out.println("\nFinal Recommendation: ");
        // we don't log the final run to file to avoid duplication
        ParameterSearch.printDetailsOfRun(bestResult, searchSpace, "", false, null);
    }
}

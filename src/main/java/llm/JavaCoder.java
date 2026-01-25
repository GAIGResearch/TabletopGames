package llm;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import llm.LLMAccess.LLM_MODEL;
import llm.LLMAccess.LLM_SIZE;
import players.PlayerFactory;
import players.heuristics.StringHeuristic;
import utilities.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static java.util.stream.Collectors.toList;

public class JavaCoder {

    /**
     * Args:
     * [0]: game name
     * dir: working dir
     * [2]: evaluator name
     *
     * @param args
     */
    public static void main(String[] args) throws IOException {

        //Arg. Example:  dir=llm game=TicTacToe evaluator=TicTacToeEvaluator
        // evaluator will default to <gameName>Evaluator if not provided
        String gameName = Utils.getArg(args, "game", "TicTacToe");
        int trials = Utils.getArg(args, "trials", 1);
        int max_iters = Utils.getArg(args, "iterations", 10);
        GameType gameType = GameType.valueOf(gameName);
        int playerCount = Utils.getArg(args, "nPlayers", 2);
        String opponent = Utils.getArg(args, "opponent", "random");
        AbstractPlayer opponentPlayer = PlayerFactory.createPlayer(opponent);
        String baseAgentLocation = Utils.getArg(args, "baseAgent", opponent);
        String workingDir = Utils.getArg(args, "dir", "llm");
        String modelType = Utils.getArg(args, "model", "GEMINI");
        String modelSize = Utils.getArg(args, "size", "SMALL");
        int matchups = Utils.getArg(args, "matchups", 1000);
        String resultsFile = Utils.getArg(args, "results", workingDir + "/HeuristicSearch_Results.txt");
        String evaluatorName = Utils.getArg(args, "evaluator", gameName + "Evaluator");
        workingDir = workingDir + "/" + modelType + "_" + modelSize + "/" + gameName;
        String llmLogFile = workingDir + "/LLM.log";
        String fileStem = workingDir + "/" + evaluatorName;
        File results = new File(resultsFile);

        File dir = new File(workingDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        GamePromptGenerator promptGenerator = new GamePromptGenerator();

        int currentErrors = 0;
        int maxErrorsPerIteration = 3;

        LLM_SIZE llmSize = LLM_SIZE.valueOf(modelSize);
        LLM_MODEL llmModel = LLM_MODEL.valueOf(modelType);

        LLMAccess llm = new LLMAccess(llmModel, llmSize, llmLogFile);

        String generatedCode = "";
        String error = "";

        double[][] scores = new double[trials][max_iters];
        String[] code = new String[max_iters];
        boolean[][] safeIterations = new boolean[trials][max_iters];
        int[] compileErrorsPerTrial = new int[trials];
        int[] runtimeErrorsPerTrial = new int[trials];
        int[] bestIterationsPerTrial = new int[trials];
        AbstractPlayer[] bestPlayerPerTrial = new AbstractPlayer[trials];

        // set up defaults for tournaments
        Map<RunArg, Object> tournamentConfig = RunArg.parseConfig(new String[]{},
                Collections.singletonList(RunArg.Usage.RunGames));
        // then override the ones we really want
        tournamentConfig.putAll(Map.of(
                RunArg.game, gameType,
                RunArg.matchups, matchups,
                RunArg.listener, Collections.emptyList(),
                RunArg.destDir, workingDir,
                RunArg.verbose, false
        ));
        AbstractParameters params = gameType.createParameters(System.currentTimeMillis());


        // Stats to gather are:
        // - input and output tokens for the model (in LLMAccess)
        // - failed calls (compile errors)
        // - failed calls (runtime errors)
        // Which we want to log once finished (along with the best heuristic)
        int successfulIterations = 0; // how many generate working code

        for (int t = 0; t < trials; t++) {
            System.out.printf("Trial %d of %d%n", t + 1, trials);
            int iteration = 0;
            int compileErrors = 0;
            int runtimeErrors = 0;
            AbstractPlayer[] playersPerIteration = new AbstractPlayer[max_iters];

            while (iteration < max_iters) {
                try {
                    String fileName = fileStem + String.format("%02d_%03d.java", t, iteration);
                    String className = evaluatorName + String.format("%02d_%03d", t, iteration);

                    String llmPrompt = promptGenerator.createLLMTaskPrompt(
                            GamePromptGenerator.TaskType.Heuristic,
                            gameType,
                            playerCount,
                            className,
                            true);

                    boolean atLeastOneSafePreviousIteration = false;
                    for (int index = 0; index < iteration; index++) {
                        if (safeIterations[t][index]) {
                            atLeastOneSafePreviousIteration = true;
                            break;
                        }
                    }
                    if (atLeastOneSafePreviousIteration) {
                        // find the best score so far, and extract the code for that
                        String bestCode = "";
                        double bestScore = 0.0;
                        for (int index = 0; index < iteration; index++) {
                            if (!safeIterations[t][index]) {
                                continue; // exclude iterations that failed to compile or threw exceptions
                            }
                            if (scores[t][index] > bestScore) {
                                bestScore = scores[t][index];
                                bestCode = code[index];
                            }
                        }

                        promptGenerator.createLLMFeedbackPrompt(
                                GamePromptGenerator.TaskType.Heuristic,
                                gameType,
                                playerCount,
                                className,
                                bestCode);
                    }

                    if (!error.isEmpty()) {
                        currentErrors++;
                        llmPrompt = promptGenerator.createLLMErrorPrompt(
                                GamePromptGenerator.TaskType.Heuristic,
                                gameType,
                                playerCount,
                                className,
                                generatedCode,
                                error);
                    }

                    //String.format("This class had failed to compile correctly.%n%n%s%n%nThe error message is %s%n.Rewrite this code to compile correctly%n", generatedCode, error);
                    error = "";
                    // Use regex to extract code between ```java and ```
                    // and remove any comments
                    generatedCode = llm.getResponse(llmPrompt);

                    if (generatedCode.isEmpty()) {
                        System.out.println("No code generated, stopping this iteration");
                        safeIterations[t][iteration] = false;
                        iteration++;
                        currentErrors = 0;
                        error = "";
                        continue;
                    }
                    //.replaceAll("```java\\s*(.*?)", "$1");
                    //        .replaceAll("(.*?)```", "$1")
                    //       .replaceAll("//.*\\n", "");

                    String commentPrompt = """
                                After the *** is a Java class.
                                Your task is to remove any comments or JavaDoc from this code.
                                The output should be the same code, with any syntax corrections, but without any comments.
                            
                                The output must include only the final java code.
                                ***
                            """;
                    String commentFreeCode = llm.getResponse(commentPrompt + generatedCode, llmModel, LLM_SIZE.SMALL)
                            .replaceAll("```java\\s*(.*?)", "$1")
                            .replaceAll("(.*?)```", "$1");
                    writeGeneratedCodeToFile(commentFreeCode, fileName);
                    code[iteration] = generatedCode;  // we store for future prompts (with comments, as these could be useful)

                    // Future development: Add an extra call to summarise the functionality of the code (using the version with comments)
                    // "useful to someone who wanted to write the function anew from a functional specification"
                    // that might be a good thing to pass to future iterations

                    System.out.printf("Iteration %d has generated code%n", iteration);
                    safeIterations[t][iteration] = true;

                    // We now create a StringHeuristic and associated player from the generated code
                    AbstractPlayer player = PlayerFactory.createPlayer(baseAgentLocation);
                    if (player instanceof IHasStateHeuristic hPlayer) {
                        hPlayer.setStateHeuristic(new StringHeuristic(fileName, className));
                    } else {
                        throw new IllegalArgumentException("Agent " + baseAgentLocation + " does not implement IHasStateHeuristic");
                    }
                    player.setName(String.format("%s_%03d", player, iteration));
                    playersPerIteration[iteration] = player;

                } catch (RuntimeException e) {
                    System.out.println("Error compiling: " + e.getMessage());
                    error = e.getMessage();
                } catch (IOException exception) {
                    System.out.println("Error writing file: " + exception.getMessage());
                    error = exception.getMessage();
                }

                if (!error.isEmpty()) {
                    // in this case we failed to compile the code, so we don't run the tournament
                    if (currentErrors >= maxErrorsPerIteration) {
                        System.out.println("Too many errors, stopping this iteration");
                        safeIterations[t][iteration] = false;
                        iteration++;
                        currentErrors = 0;
                        error = "";
                    } else {
                        System.out.println("Compilation error, re-asking LLM");
                        compileErrors++;
                    }
                    continue;
                }

                List<AbstractPlayer> playersForTournament = Arrays.stream(playersPerIteration)
                        .filter(Objects::nonNull)
                        .collect(toList());
                // we have at least one opponent player for comparison
                // and then pad out extra players to the required number for the player count (if needed)
                do {
                    playersForTournament.add(opponentPlayer.copy());
                } while (playersForTournament.size() < playerCount);

                tournamentConfig.put(RunArg.destDir, workingDir + File.separator + "Trial" + String.format("%02d", t));
                RoundRobinTournament tournament = setupTournament(gameType, playerCount, tournamentConfig, params, playersForTournament);

                try {
                    tournament.run();
                } catch (Exception | Error e) {
                    System.out.println("Error running up tournament: " + e.getMessage());
                    runtimeErrors++;
                    error = e.getMessage();
                    safeIterations[t][iteration] = false;  // exclude the latest heuristic from future consideration
                    playersPerIteration[iteration] = null;  // remove the last player from the list
                }

                if (safeIterations[t][iteration]) {
                    // record results if we ran safely
                    successfulIterations++;
                    for (int index = 0; index < playersPerIteration.length; index++) {
                        if (playersPerIteration[index] == null)
                            continue; // exclude iterations that failed to compile or threw exceptions
                        System.out.printf("Player %s has score %.2f%n", playersPerIteration[index], tournament.getWinRate(index));
                    }

                    // we now extract the scores of the agents, and record these
                    // the players are added in iteration order, so we can use that
                    int playerIndex = 0;
                    for (int i = 0; i <= iteration; i++) {
                        if (safeIterations[t][i]) {
                            scores[t][i] = tournament.getWinRate(playerIndex);
                            playerIndex++;
                        } else {
                            scores[t][i] = 0.0;
                        }
                    }
                }

                iteration++;
                currentErrors = 0;
            }

            // find best score after end of trial (and record the best agent)
            bestIterationsPerTrial[t] = -1;
            double bestScore = -1.0;
            for (int index = 0; index < scores.length; index++) {
                if (scores[t][index] > bestScore) {
                    bestScore = scores[t][index];
                    bestIterationsPerTrial[t] = index;
                    bestPlayerPerTrial[t] = playersPerIteration[index];
                }
            }
            System.out.printf("Best score for trial %2d is %.3f on iteration %2d%n", t, bestScore, bestIterationsPerTrial[t]);
            try (FileWriter writer = new FileWriter(results, true)) {
                if (!results.exists()) {
                    writer.write("Game, ModelType, ModelSize, Players, Trial, Iterations, CompileErrors, RuntimeErrors," +
                            " InputTokens, OutputTokens, " +
                            " SuccessfulIterations, BestHeuristic\n");
                }
                int successfulIterationsThisTrial = 0;
                for (int i = 0; i < max_iters; i++) {
                    if (safeIterations[t][i]) {
                        successfulIterationsThisTrial++;
                    }
                }
                writer.write(String.format("%s, %s, %s, %d, %d, %d, %d, %d, %d, %d, %d, %d%n",
                        gameName, modelType, modelSize, playerCount, t, max_iters, compileErrors, runtimeErrors,
                        llm.inputTokens, llm.outputTokens,
                        successfulIterationsThisTrial, bestIterationsPerTrial[t]));
                // then reset the tokens for the next trial
                llm.inputTokens = 0;
                llm.outputTokens = 0;
            }
            compileErrorsPerTrial[t] = compileErrors;
            runtimeErrorsPerTrial[t] = runtimeErrors;
        }

        // Final results
        System.out.printf("Total Iterations: %d%n", max_iters * trials);
        System.out.printf("Compile errors: %d%n", Arrays.stream(compileErrorsPerTrial)
                .reduce(0, Integer::sum));
        System.out.printf("Runtime errors: %d%n", Arrays.stream(runtimeErrorsPerTrial)
                .reduce(0, Integer::sum));
        int successfulTrials = (int) Arrays.stream(bestIterationsPerTrial)
                .filter(i -> i >= 0)
                .count();
        System.out.printf("Successful trials: %d%n", successfulTrials);
        System.out.printf("Successful iterations: %d%n", successfulIterations);

        // find the best heuristic across all trials with new tournament
        List<AbstractPlayer> playersForTournament = new ArrayList<>();
        for (int i = 0; i < trials; i++) {
            if (bestPlayerPerTrial[i] != null) {
                bestPlayerPerTrial[i].setName(String.format("Winner_Trial%02d_Iter%02d", i, bestIterationsPerTrial[i]));
                playersForTournament.add(bestPlayerPerTrial[i]);
            }
        }

        playersForTournament.add(opponentPlayer.copy());
        tournamentConfig.put(RunArg.destDir, workingDir + File.separator + "Final");
        tournamentConfig.put(RunArg.matchups, matchups * 5);  // extra resolution for the final run

        RoundRobinTournament finalTournament = setupTournament(gameType, playerCount, tournamentConfig, params, playersForTournament);
        finalTournament.run();

        // We then need to report the final results and the best agent from all trials
        System.out.println("Final results:");
        for (int i = 0; i < playersForTournament.size(); i++) {
            System.out.printf("Player %s has score %.3f%n", playersForTournament.get(i), finalTournament.getWinRate(i));
        }
    }

    private static RoundRobinTournament setupTournament(GameType gameType, int playerCount, Map<RunArg, Object> tournamentConfig, AbstractParameters params, List<AbstractPlayer> playersForTournament) {
        RoundRobinTournament tournament;
        if (playersForTournament.size() < playerCount) {
            tournamentConfig.put(RunArg.mode, "exhaustiveSP");
        } else {
            tournamentConfig.put(RunArg.mode, "exhaustive");
        }
        try {
            tournament = new RoundRobinTournament(
                    playersForTournament, gameType, playerCount, params,
                    tournamentConfig);
        } catch (IllegalArgumentException e) {
            System.out.println("Setting mode to RANDOM");
            tournamentConfig.put(RunArg.mode, "random");
            tournament = new RoundRobinTournament(
                    playersForTournament, gameType, playerCount, params,
                    tournamentConfig);
        }
        return tournament;
    }

    // Write the prompt response to file
    public static void writeGeneratedCodeToFile(String generatedCode, String filePath) throws IOException {
        List<String> lines = new ArrayList<>();
        for (String line : generatedCode.split("\n")) {
            lines.add(line.split("//", 1)[0].strip() + "\n");
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            for (String line : lines) {
                writer.write(line);
            }
        }
    }

}

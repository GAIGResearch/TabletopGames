package llm;

import core.AbstractParameters;
import core.AbstractPlayer;
import dev.langchain4j.model.openai.OpenAiTokenizer;
import evaluation.RunArg;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import llm.LLMAccess.LLM_MODEL;
import llm.LLMAccess.LLM_SIZE;
import players.PlayerFactory;
import players.heuristics.StringHeuristic;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

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
        GameType gameType = GameType.valueOf(gameName);
        int playerCount = Utils.getArg(args, "players", 2);
        String opponent = Utils.getArg(args, "opponent", "random");
        AbstractPlayer opponentPlayer = PlayerFactory.createPlayer(opponent);
        String baseAgentLocation = Utils.getArg(args, "baseAgent", opponent);
        String workingDir = Utils.getArg(args, "dir", "llm");
        String modelType = Utils.getArg(args, "model", "GEMINI");
        String modelSize = Utils.getArg(args, "size", "SMALL");
        int matchups = Utils.getArg(args, "matchups", 1000);
        String matchMode = Utils.getArg(args, "mode", "exhaustive");
        String resultsFile = Utils.getArg(args, "results", workingDir + "/HeuristicSearch_Results.txt");
        String evaluatorName = Utils.getArg(args, "evaluator", gameName + "Evaluator");
        workingDir = workingDir + "/" + modelType + "_" + modelSize + "/" + gameName;
        String llmLogFile = workingDir + "/LLM.log";
        String fileStem = workingDir + "/" + evaluatorName;
        File results = new File(resultsFile);
        boolean headersNeeded = !results.exists();


        File dir = new File(workingDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        GamePromptGenerator promptGenerator = new GamePromptGenerator();

        int iteration = 0;
        int max_iters = 10;
        int currentErrors = 0;
        int maxErrorsPerIteration = 3;

        LLM_SIZE llmSize = LLM_SIZE.valueOf(modelSize);
        LLM_MODEL llmModel = LLM_MODEL.valueOf(modelType);

        LLMAccess llm = new LLMAccess(llmModel, llmSize, llmLogFile);
        List<AbstractPlayer> playerList = new ArrayList<>();

        String generatedCode = "";
        String error = "";

        double[] scores = new double[max_iters];
        String[] code = new String[max_iters];
        boolean[] safeIterations = new boolean[max_iters];

        // Stats to gather are:
        // - input and output tokens for the model (in LLMAccess)
        // - failed calls (compile errors)
        // - failed calls (runtime errors)
        // Which we want to log once finished (along with the best heuristic)
        int compileErrors = 0;
        int runtimeErrors = 0;

        while (iteration < max_iters) {
            try {
                String fileName = fileStem + String.format("%03d.java", iteration);
                String className = evaluatorName + String.format("%03d", iteration);

                String llmPrompt = promptGenerator.createLLMTaskPrompt(
                        GamePromptGenerator.TaskType.Heuristic,
                        gameType,
                        playerCount,
                        className,
                        true);

                boolean atLeastOneSafePreviousIteration = false;
                for (int index = 0; index < iteration; index++) {
                    if (safeIterations[index]) {
                        atLeastOneSafePreviousIteration = true;
                        break;
                    }
                }
                if (atLeastOneSafePreviousIteration) {
                    // find the best score so far, and extract the code for that
                    String bestCode = "";
                    double bestScore = 0.0;
                    for (int index = 0; index < iteration; index++) {
                        if (!safeIterations[index]) {
                            continue; // exclude iterations that failed to compile or threw exceptions
                        }
                        if (scores[index] > bestScore) {
                            bestScore = scores[index];
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

                // TODO: Add an extra call to summarise the functionality of the code (using the version with comments)
                // "useful to someone who wanted to write the function anew from a functional specification"
                // that might be a good thing to pass to future iterations

                System.out.printf("Iteration %d has generated code%n", iteration);
                safeIterations[iteration] = true;

                AbstractPlayer player = PlayerFactory.createPlayer(baseAgentLocation);
                if (player instanceof IHasStateHeuristic hPlayer) {
                    hPlayer.setStateHeuristic(new StringHeuristic(fileName, className));
                } else {
                    throw new IllegalArgumentException("Agent " + baseAgentLocation + " does not implement IHasStateHeuristic");
                }

                // We now create a StringHeuristic and associated player from the generated code
                player.setName(String.format("%s_%03d", player.toString(), iteration));
                playerList.add(player);

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
                    safeIterations[iteration] = false;
                    iteration++;
                    currentErrors = 0;
                    error = "";
                } else {
                    System.out.println("Compilation error, re-asking LLM");
                    compileErrors++;
                }
                continue;
            }
            // set up defaults
            Map<RunArg, Object> tournamentConfig = RunArg.parseConfig(new String[]{},
                    Collections.singletonList(RunArg.Usage.RunGames));
            // then override the ones we really want
            tournamentConfig.putAll(Map.of(
                    RunArg.game, gameType,
                    RunArg.matchups, matchups,
                    RunArg.mode, matchMode,
                    RunArg.listener, Collections.emptyList(),
                    RunArg.destDir, workingDir,
                    RunArg.verbose, false
            ));
            AbstractParameters params = gameType.createParameters(System.currentTimeMillis());

            List<AbstractPlayer> playersForTournament = new ArrayList<>(playerList);
            // we have at least one opponent player for comparison
            // and then pad out extra players to the required number for the player count (if needed)
            playersForTournament.add(opponentPlayer.copy());
            while (playersForTournament.size() < playerCount) {
                playersForTournament.add(opponentPlayer.copy());
            }

            RoundRobinTournament tournament = new RoundRobinTournament(
                    playersForTournament, gameType, playerCount, params,
                    tournamentConfig);
            try {
                tournament.run();
            } catch (Exception | Error e) {
                e.printStackTrace();
                System.out.println("Error running up tournament: " + e.getMessage());
                runtimeErrors++;
                error = e.getMessage();
                safeIterations[iteration] = false;  // exclude the latest heuristic from future consideration
                playerList.remove(playerList.size() - 1);  // remove the last player from the list
            }
            if (safeIterations[iteration]) {
                // record results if we ran safely
                for (int index = 0; index < playerList.size(); index++) {
                    System.out.printf("Player %s has score %.2f%n", playerList.get(index).toString(), tournament.getWinRate(index));
                }

                // we now extract the scores of the agents, and record these
                // the players are added in iteration order, so we can use that
                int playerIndex = 0;
                for (int i = 0; i <= iteration; i++) {
                    if (safeIterations[i]) {
                        scores[i] = tournament.getWinRate(playerIndex);
                        playerIndex++;
                    } else {
                        scores[i] = 0.0;
                    }
                }
            }

            iteration++;
            currentErrors = 0;
        }

        // Final results
        System.out.printf("Total Iterations: %d%n", max_iters);
        System.out.printf("Compile errors: %d%n", compileErrors);
        System.out.printf("Runtime errors: %d%n", runtimeErrors);
        System.out.printf("Successful iterations: %d%n", playerList.size());
        // find best score
        int bestScoreIndex = -1;
        double bestScore = -1.0;
        for (int index = 0; index < scores.length; index++) {
            if (scores[index] > bestScore) {
                bestScore = scores[index];
                bestScoreIndex = index;
            }
        }
        System.out.printf("Best heuristic: %s%n", bestScoreIndex);
        try (FileWriter writer = new FileWriter(results, true)) {
            if (headersNeeded) {
                writer.write("Game, ModelType, ModelSize, Players, Iterations, CompileErrors, RuntimeErrors," +
                        " InputTokens, OutputTokens, " +
                        " SuccessfulIterations, BestHeuristic\n");
            }
            writer.write(String.format("%s, %s, %s, %d, %d, %d, %d, %d, %d, %d, %d%n",
                    gameName, modelType, modelSize, playerCount, max_iters, compileErrors, runtimeErrors,
                    llm.inputTokens, llm.outputTokens,
                    playerList.size(), bestScoreIndex));
        }
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

package llm;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.Game;
import evaluation.RunArg;
import evaluation.tournaments.AbstractTournament;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import players.heuristics.StringHeuristic;
import players.simple.OSLAParameters;
import players.simple.OSLAPlayer;
import players.simple.RandomPlayer;
import utilities.Utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JavaCoder {

    /**
     * Args:
     *  [0]: game name
     *  dir: working dir
     *  [2]: evaluator name
     * @param args
     */
    public static void main(String[] args) {

        //Arg. Example:  dir=llm gameName=TicTacToe evaluator=TicTacToeEvaluator
        String workingDir = Utils.getArg(args, "dir", "llm");
        String gameName = Utils.getArg(args, "gameName", "TicTacToe");
        String gameStateClassName = Utils.getArg(args, "gameStateClassName", "TicTacToeGameState");
        String evaluatorName = Utils.getArg(args, "evaluator", "TicTacToeEvaluator");
        String promptsDir = workingDir + "/prompts/" + gameName + "/";
        String llmLogFile = workingDir + "/" + gameName + "_llm_log.txt";
        String fileStem = workingDir + "/" + evaluatorName;

        TaskPrompter tp = new TaskPrompter(gameName, gameStateClassName, promptsDir);

        int iteration = 0;
        int max_iters = 3;

        //String javaSourceFileStem = fileStem.replaceAll(".*/(.*?)", "$1");
        LLMAccess llm = new LLMAccess(LLMAccess.LLM_MODEL.GEMINI, llmLogFile);
        List<AbstractPlayer> playerList = new ArrayList<>();
        playerList.add(new RandomPlayer());
        String generatedCode = "";
        String error = "";

        while (iteration < max_iters) {
            try {
                String fileName = fileStem + String.format("%03d.java", iteration);
                String className = evaluatorName + String.format("%03d", iteration);

                String llmPrompt = tp.getTaskPrompt(className);
                if (iteration > 0) {
                    llmPrompt = tp.getFeedbackPrompt(generatedCode);

                    if (!error.isEmpty())
                        llmPrompt = tp.getCompilationErrorFeedbackPrompt(generatedCode, error);
                }

                //String.format("This class had failed to compile correctly.%n%n%s%n%nThe error message is %s%n.Rewrite this code to compile correctly%n", generatedCode, error);
                error = "";
                // Use regex to extract code between ```java and ```
                // and remove any comments
                generatedCode = llm.getResponse(llmPrompt)
                        .replaceAll("```java\\s*(.*?)", "$1")
                        .replaceAll("(.*?)```", "$1")
                        .replaceAll("//.*\\n", "");
                writeGeneratedCodeToFile(generatedCode, fileName);

                System.out.printf("Iteration %d has generated code%n", iteration);

                // We now create a StringHeuristic and OSLA player from the generated code
                StringHeuristic heuristic = new StringHeuristic(fileName, className);
                OSLAParameters params = new OSLAParameters();
                params.heuristicFunc = heuristic;
                OSLAPlayer player = new OSLAPlayer(params);
                player.setName(String.format("OSLA_%03d", iteration));
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
                System.out.println("Compilation error, re-asking LLM");
                continue;
            }
            // set up defaults
            Map<RunArg, Object> tournamentConfig = RunArg.parseConfig(new String[]{},
                    Collections.singletonList(RunArg.Usage.RunGames));
            // then override the ones we really want
            tournamentConfig.putAll(Map.of(
                    RunArg.game, GameType.TicTacToe,
                    RunArg.matchups, 100,
                    RunArg.listener, Collections.emptyList(),
                    RunArg.mode, "exhaustive",
                    RunArg.output, String.format("%s_%03d_Results.txt", fileStem, iteration),
                    RunArg.verbose, false
            ));
            GameType gameType = GameType.TicTacToe;
            AbstractParameters params = GameType.TicTacToe.createParameters(System.currentTimeMillis());

            // TODO: There is scope to refactor RoundRobinTournament constructor usage to simplify this
            RoundRobinTournament tournament = new RoundRobinTournament(
                    playerList, gameType, 2, params,
                    AbstractTournament.TournamentMode.NO_SELF_PLAY, tournamentConfig);
            tournament.run();
            for (int index = 0; index < playerList.size(); index++) {
                System.out.printf("Player %s has score %.2f%n", playerList.get(index).toString(), tournament.getWinRate(index));
            }

            iteration++;
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

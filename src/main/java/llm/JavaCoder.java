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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class JavaCoder {

    public static void main(String[] args) {

        // log file to write all traffic to and from the LLM (for debugging / auditing)
        String llmLogFile = "llm/llm_log.txt";
        String fileStem = "llm/TicTacToeEvaluator";
        String javaSourceFileStem = fileStem.replaceAll(".*/(.*?)", "$1");
        String task_prompt = """
                 You are playing Tic Tac Toe. Your job is to write the evaluation logic to help an AI play this game. Don't leave parts unfinished or TODOs.
                 First, write a java class called TicTacToeEvaluator class, with only a single function with this signature:\s
                 - public double evaluateState(TicTacToeGameState gameState, int playerId)
                This is a heuristic function to play Tic Tac Toe. The variable gameState is the current state of the game, and playerId\s
                is the ID of the player we evaluate the state for. Write the contents of this function, so that we give a higher numeric\s
                evaluation to those game states that are beneficial to the player with the received playerId as id. Return:
                  - 0.0 if player playerId lost the game.
                  - 1.0 if player playerId won the game.
                 If the game is not over, return a value between 0.0 and 1.0 so that the value is close to 0.0 if player playerId is close to losing,
                 and closer to 1.0 if playerId is about to win the game.
                Take into account the whole board position, checking for lines that are about to be completed, and possible opponent moves.\s
                You can use the following API:
                 - In TicTacToeGameState, you can use the following functions:
                    - GridBoard<Token> getGridBoard(), to access the board of the game.
                    - Token getPlayerToken(int playerId), to get the Token of the player passed as a parameter.
                    - boolean isGameOver(), returns true if the game is finished.
                    - boolean getWinner(), returns the Id of the player that won the game, or -1 if the game is not over.
                    - int getCurrentPlayer(), returns the Id of the player that moves next.
                 - GridBoard<Token> has the following functions you can also use:
                   - int getWidth(), to return the width of the board.
                   - int getHeight(), to return the height of the board.
                   - Token getElement(int x, int y), that returns the Token on the position of the board with row x and column y.\s
                 - Token represents a piece placed by a player. Which player the token belongs to is represented with a string. This string\s
                   is "x" for player ID 0, and "o" for player ID 1.\s
                   - Token(String) allows your to create token objects for any comparisons.\s
                   - String getTokenType() returns the string representation of the token type.
                Assume all the other classes are implemented, and do not include a main function. Add all the import statements required,\s
                in addition to importing games.tictactoe.TicTacToeGameState, core.components.GridBoard and core.components.Token\s
                Do not include any explanation of the code. Just the raw Java code is needed.
                Do not include any comments in the code.
                                """;
        String feedbackPrompt = """
                The current best heuristic code is below.
                ```java
                %s
                ```
                Your task is to generate a new heuristic function that is better than the current one.
                A better heuristic will have a higher win rate and/or have shorter and less complex code.
                """;

        int iteration = 0;
        int max_iters = 3;

        LLMAccess llm = new LLMAccess(LLMAccess.LLM_MODEL.OPENAI, llmLogFile);
        List<AbstractPlayer> playerList = new ArrayList<>();
        playerList.add(new RandomPlayer());
        String generatedCode = "";
        String error = "";

        while (iteration < max_iters) {
            try {
                String fileName = fileStem + String.format("%03d.java", iteration);

                String llmPrompt = task_prompt;
                if (iteration > 0) {
                    llmPrompt = String.format(feedbackPrompt, generatedCode);
                }
                if (!error.isEmpty())
                    llmPrompt = String.format("This class had failed to compile correctly.%n%n%s%n%nThe error message is %s%n.Rewrite this code to compile correctly%n", generatedCode, error);
                error = "";
                // Use regex to extract code between ```java and ```
                // and remove any comments
                generatedCode = llm.getResponse(llmPrompt)
                        .replaceAll("```java\\s*(.*?)", "$1")
                        .replaceAll("(.*?)```", "$1")
                        .replaceAll("//.*\\n", "")
                        .replaceAll(javaSourceFileStem, String.format("%s%03d", javaSourceFileStem, iteration));
                writeGeneratedCodeToFile(generatedCode, fileName);

                // We now create a StringHeuristic and OSLA player from the generated code
                StringHeuristic heuristic = new StringHeuristic(fileName, "TicTacToeEvaluator");
                OSLAParameters params = new OSLAParameters();
                params.heuristic = heuristic;
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

package llm;

public class TaskPrompter {
    String gameName;

    String gameStateClassName;

    String promptsDir;

    public TaskPrompter(String gameName, String gameStateClassName, String promptsDir)
    {
        this.gameName = gameName;
        this.promptsDir = promptsDir;
        this.gameStateClassName = gameStateClassName;
    }

    private String getMainTask(String className) {
        return String.format("""
                You are playing %s. Your job is to write the evaluation logic to help an AI play this game. Don't leave parts unfinished or TODOs.
                 First, write a java class called %s, with only a single function with this signature:\s
                 - public double evaluateState(%s gameState, int playerId)
                This is a heuristic function to play %s. The variable gameState is the current state of the game, and playerId\s
                is the ID of the player we evaluate the state for. Write the contents of this function, so that we give a higher numeric\s
                evaluation to those game states that are beneficial to the player with the received playerId as id. Return:
                  - 0.0 if player playerId lost the game.
                  - 1.0 if player playerId won the game.
                 If the game is not over, return a value between 0.0 and 1.0 so that the value is close to 0.0 if player playerId is close to losing,
                 and closer to 1.0 if playerId is about to win the game.
                Take into account all the elements of the game state and possible opponent moves.\s
                """, gameName, className, gameStateClassName, gameName);
    }

    private String getAPIinformation()
    {
        return String.format("""
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
                                """);
    }

    public String getTaskPrompt(String className)
    {
        String mainTask = getMainTask(className);
        String api = getAPIinformation();
        return mainTask + api;
        }

    public String getFeedbackPrompt(String code) {
        String text = """
                The current best heuristic code is below.
                ```java
                %s
                ```
                Your task is to generate a new heuristic function that is better than the current one.
                A better heuristic will have a higher win rate and/or have shorter and less complex code.
                """;
        return String.format(text, code);
    }

    public String getCompilationErrorFeedbackPrompt(String code, String errorMessage)
    {
        String text = """
                This class had failed to compile correctly.
                ```java
                %s
                ```
                The error message is:
                %s
                
                Rewrite this code to compile correctly
                """;
        return String.format(text, code, errorMessage);
    }

}

package llm;

public class TaskPrompter {
    String gameName;

    String gameStateClassName;

    String promptsDir;

    public TaskPrompter(String gameName, String gameStateClassName, String promptsDir) {
        this.gameName = gameName;
        this.promptsDir = promptsDir;
        this.gameStateClassName = gameStateClassName;
    }

    private String getMainTask(String className) {
        return String.format("""
                You are playing %s. Your job is to write the evaluation logic to help an AI play this game. Don't leave parts unfinished or TODOs.
                First, write a java class whose name is exactly %s. It must have only a single function with this signature:\s
                 - public double evaluateState(%s gameState, int playerId)
                This is a heuristic function to play %s. The variable gameState is the current state of the game, and playerId\s
                is the ID of the player we evaluate the state for. Write the contents of this function, so that we give a higher numeric\s
                evaluation to those game states that are beneficial to the player with the received playerId as id.
                The return value must be between 0.0 and 1.0 so that the value is close to 0.0 if player playerId is close to losing,
                and closer to 1.0 if playerId is about to win the game.
                You should assume that the game is not over when this function is called.
                Take into account all the elements of the game state and possible opponent moves.\s
                """, gameName, className, gameStateClassName, gameName);
    }

    private String getAPIInformation() {
        return switch (gameName) {
            case "TicTacToe" -> """
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
            case "LoveLetter" -> """
                    You can use the following API:
                     - In LoveLetterGameState, you can use the following functions:
                        - double getGameScore(int id), to return the score of the id player passed as a parameter.
                        - int getRemainingCards(), to get number of remaining cards in the draw deck.
                        - PartialObservableDeck<LoveLetterCard> getPlayerHandCards(int id), returns the cards in hand of player id. If this id is not playerId, these cards will not be necessarily correct.
                        - PartialObservableDeck<LoveLetterCard> getDrawPile(), returns the cards still to be drawn. The game ends when this is empty.
                        - boolean getWinner(), returns the Id of the player that won the game, or -1 if the game is not over.
                        - int getCurrentPlayer(), returns your player number
                        - int getNPlayers(), returns the total number of players in the game
                     - LoveLetterCard represents a card in the game. For a LoveLetterCard card, you can use:
                       - card.cardType, an enum with the type of the card (Guard, Princess, Baron, Handmaid, Countess, King, Priest, Prince))
                       - card.cardType.getValue(), an int with the value of the card.
                     - A PartialObservableDeck<LoveLetterCard> is a deck of cards that can be partially observed. You can use:
                       - int getSize(), to get the number of cards in the deck.
                       - LoveLetterCard get(int i), to get the card at position i.
                     The current player will have two cards in their hand. All other players still in the game just have one.
                     The card values possible are listed below, along with the special ability they have when played:
                     8 - Princess: You are immediately eliminated if you discard the Princess. At the end of the round, the Princess is the highest-value card. (1 card)
                     7 - Countess: You must discard the Countess if the King or Prince is the other card in your hand. The Countess can be discarded normally without effect or kept if the card in your hand is another card. (1 card)
                     6 - King: Swap the remaining card in your hand with another player. (1 card)
                     5 - Prince: Force a player of your choice to discard the card in their hand. They do not perform the card’s action. (But if it’s the Princess, they are eliminated!) They immediately draw a new card. (2 cards)
                     4 - Handmaid: Card abilities do not affect you until your next turn. (2 cards)
                     3 - Baron: Compare the value of the remaining card in your hand with an opponent’s card. The player with the lower-value card is eliminated. (2 cards)
                     2 - Priest: Look at the card in another player’s hand. Only you see their card. (2 cards)
                     1 - Guard: Name an opponent and guess the card in their hand (other than a Guard). If you are correct, they are eliminated. They do not need to reveal their card if the guess is incorrect. (5 cards)
                    Assume all the other classes are implemented, and do not include a main function. Add all the import statements required,
                    in addition to importing games.loveletter.cards.LoveLetterCard, games.loveletter.LoveLetterGameState, core.components.PartialObservableDeck
                    """;
            default -> throw new IllegalArgumentException("Game not supported : " + gameName);
        };

    }

    public String getTaskPrompt(String className) {
        String mainTask = getMainTask(className);
        String api = getAPIInformation();
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

    public String getCompilationErrorFeedbackPrompt(String code, String errorMessage) {
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

import games.tictactoe.TicTacToeGameState;
import core.components.GridBoard;
import core.components.Token;

public class TicTacToeEvaluator {

    public double evaluateState(TicTacToeGameState gameState, int playerId) {
        if (gameState.isGameOver()) {
            if (gameState.getWinner() == playerId) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else {

            double playerScore = calculateScore(gameState, playerId);

            double opponentScore = calculateScore(gameState, 1 - playerId);

            return playerScore - opponentScore;
        }
    }

    private double calculateScore(TicTacToeGameState gameState, int playerId) {

        GridBoard<Token> board = gameState.getGridBoard();
        Token playerToken = gameState.getPlayerToken(playerId);
        double score = 0.0;


        for (int i = 0; i < board.getHeight(); i++) {
            if (checkLine(board, i, 0, 0, 1, playerToken)) {
                score += 0.3;
            }
        }


        for (int i = 0; i < board.getWidth(); i++) {
            if (checkLine(board, 0, i, 1, 0, playerToken)) {
                score += 0.3;
            }
        }


        if (checkLine(board, 0, 0, 1, 1, playerToken) || checkLine(board, 0, board.getWidth() - 1, 1, -1, playerToken)) {
            score += 0.3;
        }

        return score;
    }

    private boolean checkLine(GridBoard<Token> board, int startX, int startY, int deltaX, int deltaY, Token playerToken) {
        for (int i = 0; i < 3; i++) {
            int x = startX + i * deltaX;
            int y = startY + i * deltaY;
            if (!board.getElement(x, y).equals(playerToken)) {
                return false;
            }
        }
        return true;
    }
}

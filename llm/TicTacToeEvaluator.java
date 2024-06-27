import games.tictactoe.TicTacToeGameState;
import core.components.GridBoard;
import core.components.Token;

public class TicTacToeEvaluator {

    public double evaluateState(TicTacToeGameState gameState, int playerId) {
        if (gameState.isGameOver()) {
            int winnerId = gameState.getWinner();
            if (winnerId == playerId) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else {
            GridBoard<Token> board = gameState.getGridBoard();
            int width = board.getWidth();
            int height = board.getHeight();
            double evaluation = 0.5;


            for (int i = 0; i < height; i++) {
                double rowValue = evaluateLine(board.getElement(0, i), board.getElement(1, i), board.getElement(2, i), playerId);
                if (rowValue == 1.0) {
                    return 1.0;
                } else if (rowValue == 0.0) {
                    return 0.0;
                } else {
                    evaluation = Math.max(evaluation, rowValue);
                }
            }


            for (int i = 0; i < width; i++) {
                double colValue = evaluateLine(board.getElement(i, 0), board.getElement(i, 1), board.getElement(i, 2), playerId);
                if (colValue == 1.0) {
                    return 1.0;
                } else if (colValue == 0.0) {
                    return 0.0;
                } else {
                    evaluation = Math.max(evaluation, colValue);
                }
            }


            double diag1Value = evaluateLine(board.getElement(0, 0), board.getElement(1, 1), board.getElement(2, 2), playerId);
            double diag2Value = evaluateLine(board.getElement(0, 2), board.getElement(1, 1), board.getElement(2, 0), playerId);

            if (diag1Value == 1.0) {
                return 1.0;
            } else if (diag1Value == 0.0) {
                return 0.0;
            } else {
                evaluation = Math.max(evaluation, diag1Value);
            }

            if (diag2Value == 1.0) {
                return 1.0;
            } else if (diag2Value == 0.0) {
                return 0.0;
            } else {
                evaluation = Math.max(evaluation, diag2Value);
            }

            return evaluation;
        }
    }

    private double evaluateLine(Token token1, Token token2, Token token3, int playerId) {
        int playerCount = 0;
        int opponentCount = 0;

        if (token1.getTokenType().equals(String.valueOf(playerId))) {
            playerCount++;
        } else if (!token1.getTokenType().equals(".")) {
            opponentCount++;
        }

        if (token2.getTokenType().equals(String.valueOf(playerId))) {
            playerCount++;
        } else if (!token2.getTokenType().equals(".")) {
            opponentCount++;
        }

        if (token3.getTokenType().equals(String.valueOf(playerId))) {
            playerCount++;
        } else if (!token3.getTokenType().equals(".")) {
            opponentCount++;
        }

        if (opponentCount == 3) {
            return 0.0;
        } else if (playerCount == 3) {
            return 1.0;
        } else {
            return (double) playerCount / 3;
        }
    }
}

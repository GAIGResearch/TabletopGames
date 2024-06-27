import games.tictactoe.TicTacToeGameState;
import core.components.GridBoard;
import core.components.Token;

public class TicTacToeEvaluator {

    public double evaluateState(TicTacToeGameState gameState, int playerId) {
        if (gameState.isGameOver()) {
            int winner = gameState.getWinner();
            if (winner == playerId) {
                return 1.0;
            } else {
                return 0.0;
            }
        } else {
            GridBoard<Token> board = gameState.getGridBoard();
            int width = board.getWidth();
            int height = board.getHeight();
            Token playerToken = gameState.getPlayerToken(playerId);
            Token opponentToken = playerId == 0 ? new Token("o") : new Token("x");

            double playerScore = 0.0;
            double opponentScore = 0.0;


            for (int row = 0; row < height; row++) {
                int playerCount = 0;
                int opponentCount = 0;
                for (int col = 0; col < width; col++) {
                    Token token = board.getElement(row, col);
                    if (token.getTokenType().equals(playerToken.getTokenType())) {
                        playerCount++;
                    } else if (token.getTokenType().equals(opponentToken.getTokenType())) {
                        opponentCount++;
                    }
                }
                if (playerCount == width - 1 && opponentCount == 0) {
                    playerScore = Math.max(playerScore, 0.9);
                } else if (opponentCount == width - 1 && playerCount == 0) {
                    opponentScore = Math.max(opponentScore, 0.9);
                }
            }


            for (int col = 0; col < width; col++) {
                int playerCount = 0;
                int opponentCount = 0;
                for (int row = 0; row < height; row++) {
                    Token token = board.getElement(row, col);
                    if (token.getTokenType().equals(playerToken.getTokenType())) {
                        playerCount++;
                    } else if (token.getTokenType().equals(opponentToken.getTokenType())) {
                        opponentCount++;
                    }
                }
                if (playerCount == height - 1 && opponentCount == 0) {
                    playerScore = Math.max(playerScore, 0.9);
                } else if (opponentCount == height - 1 && playerCount == 0) {
                    opponentScore = Math.max(opponentScore, 0.9);
                }
            }


            int playerDiagonalCount1 = 0;
            int opponentDiagonalCount1 = 0;
            int playerDiagonalCount2 = 0;
            int opponentDiagonalCount2 = 0;
            for (int i = 0; i < width; i++) {
                Token token1 = board.getElement(i, i);
                Token token2 = board.getElement(i, width - 1 - i);
                if (token1.getTokenType().equals(playerToken.getTokenType())) {
                    playerDiagonalCount1++;
                } else if (token1.getTokenType().equals(opponentToken.getTokenType())) {
                    opponentDiagonalCount1++;
                }
                if (token2.getTokenType().equals(playerToken.getTokenType())) {
                    playerDiagonalCount2++;
                } else if (token2.getTokenType().equals(opponentToken.getTokenType())) {
                    opponentDiagonalCount2++;
                }
            }
            if ((playerDiagonalCount1 == width - 1 && opponentDiagonalCount1 == 0) ||
                    (playerDiagonalCount2 == width - 1 && opponentDiagonalCount2 == 0)) {
                playerScore = Math.max(playerScore, 0.9);
            } else if ((opponentDiagonalCount1 == width - 1 && playerDiagonalCount1 == 0) ||
                    (opponentDiagonalCount2 == width - 1 && playerDiagonalCount2 == 0)) {
                opponentScore = Math.max(opponentScore, 0.9);
            }

            return playerScore - opponentScore;
        }
    }
}

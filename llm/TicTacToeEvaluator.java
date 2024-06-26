import java.util.ArrayList;
import java.util.List;
import games.tictactoe.TicTacToeGameState;
import core.components.GridBoard;
import core.components.Token;

public class TicTacToeEvaluator {

    public double evaluateState(TicTacToeGameState gs, int playerId) {
        GridBoard<Token> board = gs.getGridBoard();
        int width = board.getWidth();
        int height = board.getHeight();
        int opponentId = (playerId + 1) % 2;
        int winner = checkForWinner(board, width, height);
        if (winner == playerId) {
            return 1.0;
        } else if (winner == opponentId) {
            return 0.0;
        }
        List<List<Token>> lines = new ArrayList<>();
        for (int y = 0; y < height; y++) {
            List<Token> line = new ArrayList<>();
            for (int x = 0; x < width; x++) {
                line.add(board.getElement(x, y));
            }
            lines.add(line);
        }
        for (int x = 0; x < width; x++) {
            List<Token> line = new ArrayList<>();
            for (int y = 0; y < height; y++) {
                line.add(board.getElement(x, y));
            }
            lines.add(line);
        }
        List<Token> diagonal1 = new ArrayList<>();
        List<Token> diagonal2 = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            diagonal1.add(board.getElement(i, i));
            diagonal2.add(board.getElement(i, height - 1 - i));
        }
        lines.add(diagonal1);
        lines.add(diagonal2);
        double score = 0.0;
        for (List<Token> line : lines) {
            score += evaluateLine(line, playerId, opponentId);
        }
        score /= lines.size();
        return score;
    }

    private int checkForWinner(GridBoard<Token> board, int width, int height) {
        for (int y = 0; y < height; y++) {
            Token first = board.getElement(0, y);
            if (first != null) {
                boolean isWinningRow = true;
                for (int x = 1; x < width; x++) {
                    if (!first.equals(board.getElement(x, y))) {
                        isWinningRow = false;
                        break;
                    }
                }
                if (isWinningRow) {
                    return first.equals(new Token("x")) ? 0 : 1;
                }
            }
        }
        for (int x = 0; x < width; x++) {
            Token first = board.getElement(x, 0);
            if (first != null) {
                boolean isWinningColumn = true;
                for (int y = 1; y < height; y++) {
                    if (!first.equals(board.getElement(x, y))) {
                        isWinningColumn = false;
                        break;
                    }
                }
                if (isWinningColumn) {
                    return first.equals(new Token("x")) ? 0 : 1;
                }
            }
        }
        Token center = board.getElement(width / 2, height / 2);
        if (center != null) {
            boolean isWinningDiagonal1 = true;
            boolean isWinningDiagonal2 = true;
            for (int i = 1; i < width; i++) {
                if (!center.equals(board.getElement(i, i))) {
                    isWinningDiagonal1 = false;
                }
                if (!center.equals(board.getElement(i, height - 1 - i))) {
                    isWinningDiagonal2 = false;
                }
            }
            if (isWinningDiagonal1 || isWinningDiagonal2) {
                return center.equals(new Token("x")) ? 0 : 1;
            }
        }
        return -1;
    }

    private double evaluateLine(List<Token> line, int playerId, int opponentId) {
        int playerCount = 0;
        int opponentCount = 0;
        boolean playerBlocked = false;
        boolean opponentBlocked = false;
        for (Token token : line) {
            if (token.equals(new Token("x"))) {
                playerCount++;
                if (opponentCount > 0) {
                    opponentBlocked = true;
                }
            } else if (token.equals(new Token("o"))) {
                opponentCount++;
                if (playerCount > 0) {
                    playerBlocked = true;
                }
            }
        }
        if (playerCount > 0 && opponentCount > 0) {
            return 0.0;
        } else if (playerCount > 0) {
            return 0.5;
        } else if (opponentCount > 0) {
            return 0.25;
        } else {
            return 0.75;
        }
    }
}

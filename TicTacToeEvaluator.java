import games.tictactoe.TicTacToeGameState;
import core.components.GridBoard;
import core.components.Token;

public double evaluateState(TicTacToeGameState gs, int playerId) {
    int opponentId = playerId == 0 ? 1 : 0;
    GridBoard<Token> board = gs.getGridBoard();
    int width = board.getWidth();
    int height = board.getHeight();
    
    double playerScore = calculateScore(board, playerId, opponentId, width, height);
    double opponentScore = calculateScore(board, opponentId, playerId, width, height);
    
    return playerScore - opponentScore;
}

private double calculateScore(GridBoard<Token> board, int playerId, int opponentId, int width, int height) {
    double score = 0.0;
    
    if (checkWin(board, playerId, width, height)) {
        score = 1.0;
    } else if (checkWin(board, opponentId, width, height)) {
        score = 0.0;
    } else {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                if (board.getElement(x, y) == null) {
                    board.setElement(x, y, new Token(playerId == 0 ? "x" : "o"));
                    if (checkWin(board, playerId, width, height)) {
                        score += 0.5;
                    }
                    board.setElement(x, y, null);
                }
            }
        }
    }
    
    return score;
}

private boolean checkWin(GridBoard<Token> board, int playerId, int width, int height) {
    // Check rows
    for (int y = 0; y < height; y++) {
        boolean win = true;
        for (int x = 0; x < width; x++) {
            if (board.getElement(x, y) == null || !board.getElement(x, y).equals(new Token(playerId == 0 ? "x" : "o"))) {
                win = false;
                break;
            }
        }
        if (win) {
            return true;
        }
    }

    // Check columns
    for (int x = 0; x < width; x++) {
        boolean win = true;
        for (int y = 0; y < height; y++) {
            if (board.getElement(x, y) == null || !board.getElement(x, y).equals(new Token(playerId == 0 ? "x" : "o"))) {
                win = false;
                break;
            }
        }
        if (win) {
            return true;
        }
    }

    // Check diagonals
    boolean winDiagonal1 = true;
    boolean winDiagonal2 = true;
    for (int i = 0; i < width; i++) {
        if (board.getElement(i, i) == null || !board.getElement(i, i).equals(new Token(playerId == 0 ? "x" : "o"))) {
            winDiagonal1 = false;
        }
        if (board.getElement(i, width - 1 - i) == null || !board.getElement(i, width - 1 - i).equals(new Token(playerId == 0 ? "x" : "o"))) {
            winDiagonal2 = false;
        }
    }
    if (winDiagonal1 || winDiagonal2) {
        return true;
    }

    return false;
}
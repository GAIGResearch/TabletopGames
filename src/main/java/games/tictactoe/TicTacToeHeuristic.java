package games.tictactoe;
import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import utilities.Pair;
import utilities.Utils;

public class TicTacToeHeuristic implements IStateHeuristic {

    double FACTOR_PLAYER = 0.8;
    double FACTOR_OPPONENT = 0.5;

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        TicTacToeGameState ttgs = (TicTacToeGameState) gs;
        Utils.GameResult playerResult = gs.getPlayerResults()[playerId];

        if(playerResult == Utils.GameResult.LOSE) {
            return -1;
        }
        if(playerResult == Utils.GameResult.WIN) {
            return 1;
        }

        // Count how many lines of player characters + rest empty, the more player characters the better
        int[] nPlayer = new int[ttgs.gridBoard.getWidth()];
        int[] nOpponent = new int[ttgs.gridBoard.getWidth()];

        double nTotalCount = nPlayer.length * 2 + 2;  // N rows + N columns + 2 diagonals

        Character playerChar = ttgs.playerMapping.get(playerId);

        // Check columns
        for (int x = 0; x < ttgs.gridBoard.getWidth(); x++){
            addCounts(countColumns(ttgs, x, playerChar), nPlayer, nOpponent);
        }
        // Check rows
        for (int y = 0; y < ttgs.gridBoard.getHeight(); y++){
            addCounts(countRows(ttgs, y, playerChar), nPlayer, nOpponent);
        }
        // Check diagonals
        // Primary
        addCounts(countPrimaryDiagonal(ttgs, playerChar), nPlayer, nOpponent);
        // Secondary
        addCounts(countSecondaryDiagonal(ttgs, playerChar), nPlayer, nOpponent);

        // Calculate scores, the more characters for player, the higher the weight
        double pScore = 0, oppScore = 0;
        for (int i = 0; i < nPlayer.length; i++) {
            pScore += nPlayer[i]/nTotalCount * Math.pow(0.5, nPlayer.length-i);
            oppScore += nOpponent[i]/nTotalCount * Math.pow(0.3, nOpponent.length-i);
        }

        return pScore * FACTOR_PLAYER + oppScore * FACTOR_OPPONENT;
    }

    private Pair<Integer, Integer> countColumns(TicTacToeGameState ttgs, int column, Character playerChar) {
        Pair<Integer, Integer> count = new Pair<>(0, 0);
        for (int y = 0; y < ttgs.gridBoard.getHeight(); y++) {
            checkChar(count, playerChar, ttgs.gridBoard.getElement(column, y));
        }
        return count;
    }

    private Pair<Integer, Integer> countRows(TicTacToeGameState ttgs, int row, Character playerChar) {
        Pair<Integer, Integer> count = new Pair<>(0, 0);
        for (int x = 0; x < ttgs.gridBoard.getWidth(); x++) {
            checkChar(count, playerChar, ttgs.gridBoard.getElement(x, row));
        }
        return count;
    }

    private Pair<Integer, Integer> countPrimaryDiagonal(TicTacToeGameState ttgs, Character playerChar) {
        Pair<Integer, Integer> count = new Pair<>(0, 0);
        for (int x = 0; x < ttgs.gridBoard.getWidth(); x++) {
            checkChar(count, playerChar, ttgs.gridBoard.getElement(x, x));
        }
        return count;
    }

    private Pair<Integer, Integer> countSecondaryDiagonal(TicTacToeGameState ttgs, Character playerChar) {
        Pair<Integer, Integer> count = new Pair<>(0, 0);
        for (int x = 0; x < ttgs.gridBoard.getWidth(); x++) {
            checkChar(count, playerChar, ttgs.gridBoard.getElement(ttgs.gridBoard.getWidth()-1-x, x));
        }
        return count;
    }

    private void checkChar(Pair<Integer, Integer> count, Character playerChar, Character c) {
        if (c == playerChar) {
            count.a ++;
        } else if (c != ' ') {
            count.b ++;
        }
    }

    private void addCounts(Pair<Integer, Integer> count, int[] nPlayer, int[] nOpponent) {
        if (count.b == 0 && count.a > 0) {
            // Player could have this line
            nPlayer[count.a-1] ++;
        } else if (count.a == 0 && count.b > 0) {
            // Opponent could have this line
            nOpponent[count.b-1] ++;
        }
    }
}
package games.catan;

import games.catan.components.CatanTile;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class HexSeedTests {

    CatanForwardModel fm = new CatanForwardModel();

    @Test
    public void hexSeedFixesBoardAndHarbours() {
        CatanParameters params = new CatanParameters();
        params.setParameterValue("hexShuffleSeed", 39392);
        CatanGameState state = new CatanGameState(params, 4);
        fm.setup(state);
        CatanTile[][] board = state.getBoard();
        CatanTile[][] boardCopy = new CatanTile[board.length][board[0].length];

        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                boardCopy[i][j] = board[i][j].copy();
            }
        }
        for (int loop = 0; loop < 9; loop++) {
            params.setRandomSeed(loop);
            fm.setup(state);
            board = state.getBoard();
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
     //               System.out.println("Checking " + board[i][j].toString() + " vs " + boardCopy[i][j].toString());
                    assertEquals(boardCopy[i][j].x, board[i][j].x);
                    assertEquals(boardCopy[i][j].y, board[i][j].y);
                    assertEquals(boardCopy[i][j].getTileType(), board[i][j].getTileType());
                    assertEquals(boardCopy[i][j].getNumber(), board[i][j].getNumber());
                }
            }
        }
    }

    @Test
    public void noSeedRandomizesBoardAndHarbours() {
        CatanParameters params = new CatanParameters();
        CatanGameState state = new CatanGameState(params, 4);
        fm.setup(state);
        CatanTile[][] board = state.getBoard();
        CatanTile[][] boardCopy = new CatanTile[board.length][board[0].length];
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                boardCopy[i][j] = board[i][j].copy();
            }
        }
        for (int loop = 0; loop < 9; loop++) {
            params.setRandomSeed(loop);
            fm.setup(state);
            board = state.getBoard();
            for (int i = 0; i < board.length; i++) {
                for (int j = 0; j < board[i].length; j++) {
                    assertEquals(boardCopy[i][j].x, board[i][j].x);
                    assertEquals(boardCopy[i][j].y, board[i][j].y);
                    int numberMatches = 0;
                    int typeMatches = 0;
                    int bothMatch = 0;
                    if (board[i][j].getTileType() != CatanTile.TileType.SEA) {
                        System.out.println("Checking " + board[i][j].toString() + " vs " + boardCopy[i][j].toString());
                        if (boardCopy[i][j].getNumber() == board[i][j].getNumber()) {
                            numberMatches++;
                        }
                        if (boardCopy[i][j].getTileType() == board[i][j].getTileType()) {
                            typeMatches++;
                        }
                        if (boardCopy[i][j].getNumber() == board[i][j].getNumber() && boardCopy[i][j].getTileType() == board[i][j].getTileType()) {
                            bothMatch++;
                        }
                    }
                    // we allow for some small error margin, as with a random shuffle some things will match
                    assertEquals(numberMatches, 2, 2);
                    assertEquals(typeMatches, 3, 3);
                    assertEquals(bothMatch, 1, 1);
                }
            }
        }
    }
}

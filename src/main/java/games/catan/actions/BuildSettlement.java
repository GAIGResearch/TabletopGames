package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

public class BuildSettlement extends AbstractAction {
    int row;
    int col;
    int vertex;
    int playerID;

    public BuildSettlement(int row, int col, int vertex, int playerID){
        this.row = row;
        this.col = col;
        this.vertex = vertex;
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();

        if (board[row][col].getSettlements()[vertex] == null) {
            board[row][col].addSettlement(vertex, playerID);
            return true;
        }

//        if (vertex == 0){
//            board[row][col].addSettlement(vertex, cgs.getCurrentPlayer());
//            if (col-1 >= 0)
//                    board[row][col-1].addSettlement(2, cgs.getCurrentPlayer());
//            if (row+1 < board.length)
//                board[row+1][col].addSettlement(4, cgs.getCurrentPlayer());
//        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof BuildSettlement){
            BuildSettlement otherAction = (BuildSettlement)other;
            return row == otherAction.row && col == otherAction.col && vertex == otherAction.vertex && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return null;
    }
}

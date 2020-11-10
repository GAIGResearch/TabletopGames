package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

public class BuildRoad extends AbstractAction {
    int row;
    int col;
    int edge;
    int playerID;

    public BuildRoad(int row, int col, int edge, int playerID){
        this.row = row;
        this.col = col;
        this.edge = edge;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();
        if (board[row][col].getRoads()[edge].getOwner() == -1) {
            return board[this.row][this.col].addRoad(edge, playerID);
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        BuildRoad copy = new BuildRoad(row, col, edge, playerID);
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof BuildRoad){
            BuildRoad otherAction = (BuildRoad)other;
            return row == otherAction.row && col == otherAction.col && edge == otherAction.edge && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        // todo put random roads on the board
        return "Buildroad in row=" + row + " col=" + col + " edge=" + edge;
    }
}

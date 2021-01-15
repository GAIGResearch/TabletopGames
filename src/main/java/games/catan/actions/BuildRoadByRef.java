package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

// Builds the Road by reference
// todo: not fully implemented: have a look at "BuildSettlementByRef"
public class BuildRoadByRef extends AbstractAction {
    int x;
    int y;
    int edge;
    int playerID;

    public BuildRoadByRef(int x, int y, int edge, int playerID){
        this.x = x;
        this.y = y;
        this.edge = edge;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();
        if (board[x][y].getRoads()[edge].getOwner() == -1) {
            return board[this.x][this.y].addRoad(edge, playerID);
        }

        return false;
    }

    @Override
    public AbstractAction copy() {
        BuildRoadByRef copy = new BuildRoadByRef(x, y, edge, playerID);
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof BuildRoadByRef){
            BuildRoadByRef otherAction = (BuildRoadByRef)other;
            return x == otherAction.x && y == otherAction.y && edge == otherAction.edge && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Buildroad in x=" + x + " y=" + y + " edge=" + edge;
    }
}

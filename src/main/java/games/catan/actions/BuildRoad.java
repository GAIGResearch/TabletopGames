package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

public class BuildRoad extends AbstractAction {
    int x;
    int y;
    int edge;
    int playerID;

    public BuildRoad(int x, int y, int edge, int playerID){
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
        BuildRoad copy = new BuildRoad(x, y, edge, playerID);
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof BuildRoad){
            BuildRoad otherAction = (BuildRoad)other;
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

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getEdge() {
        return edge;
    }

    public int getPlayerID() {
        return playerID;
    }

}

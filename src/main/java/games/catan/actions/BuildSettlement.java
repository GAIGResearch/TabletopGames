package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;

import java.util.Arrays;

public class BuildSettlement extends AbstractAction {
    int x;
    int y;
    int vertex;
    int playerID;

    public BuildSettlement(int x, int y, int vertex, int playerID){
        this.x = x;
        this.y = y;
        this.vertex = vertex;
        this.playerID = playerID;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();

        if (board[x][y].getSettlements()[vertex].getOwner() == -1) {
            board[x][y].addSettlement(vertex, playerID);
            cgs.addScore(playerID, 1);
            return true;
        }

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
            return x == otherAction.x && y == otherAction.y && vertex == otherAction.vertex && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "BuildSettlement x= " + x + " y= " + y + " vertex=" + vertex;
    }
}

package games.catan.actions.build;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.CatanTile;
import games.catan.components.Building;

import java.util.Objects;

public class BuildCity extends AbstractAction {
    public final int row;
    public final int col;
    public final int vertex;
    public final int playerID;

    public BuildCity(int row, int col, int vertex, int playerID) {
        this.row = row;
        this.col = col;
        this.vertex = vertex;
        this.playerID = playerID;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();
        CatanParameters cp = (CatanParameters) gs.getGameParameters();

        Building settlement = cgs.getBuilding(board[row][col], vertex);
        if (settlement != null && settlement.getOwnerId() == playerID) {
            if (!cgs.spendResourcesIfPossible(cp.costMapping.get(BuyAction.BuyType.City), playerID)) {
                throw new AssertionError("Player cannot afford city");
            }

            Counter cityTokens = cgs.getPlayerTokens().get(playerID).get(BuyAction.BuyType.City);
            if (cityTokens.isMaximum()){
                throw new AssertionError("Player cannot build anymore cities");
            }
            cityTokens.increment();

            // if player builds a city it gets back the settlement token
            Counter settleTokens = cgs.getPlayerTokens().get(playerID).get(BuyAction.BuyType.Settlement);
            settleTokens.decrement();
            settlement.upgrade();
            cgs.addScore(playerID, cp.buildingValue.get(Building.Type.City));
            cgs.addScore(playerID, -cp.buildingValue.get(Building.Type.Settlement));

            return true;
        } else {
            throw new AssertionError("No settlement here");
        }
    }

    @Override
    public BuildCity copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BuildCity){
            BuildCity otherAction = (BuildCity)other;
            return row == otherAction.row && col == otherAction.col && vertex == otherAction.vertex && playerID == otherAction.playerID;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(row,col,vertex,playerID);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("p%d Buy:City (row=%d col=%d vertex=%d)",playerID,row,col,vertex);
    }
}

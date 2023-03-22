package games.catan.actions;

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
        CatanParameters cp = (CatanParameters) gs.getGameParameters();
        CatanTile[][] board = cgs.getBoard();

        Building settlement = board[row][col].getSettlements()[vertex];
        if (settlement != null) {
            if (settlement.getOwnerId() == playerID) {
                Counter cityTokens = cgs.getPlayerTokens().get(playerID).get(CatanParameters.ActionType.City);
                Counter settleTokens = cgs.getPlayerTokens().get(playerID).get(CatanParameters.ActionType.Settlement);
                if (cityTokens.isMaximum()){
                    throw new AssertionError("Player cannot build anymore cities");
                }
                cityTokens.increment();
                // if player builds a city it gets back the settlement token
                settleTokens.decrement();
                if (!cgs.spendResourcesIfPossible(cp.costMapping.get(CatanParameters.ActionType.City), playerID)) {
                    throw new AssertionError("Player cannot afford city");
                }
                settlement.upgrade();

                cgs.addScore(playerID, -cp.settlement_value);
                cgs.addScore(playerID, cp.city_value);

                return true;
            } else {
                throw new AssertionError("Player does not own this settlement");
            }
        } else {
            throw new AssertionError("No settlement here");
        }
    }

    @Override
    public AbstractAction copy() {
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
        return String.format("Build City: row=%d col=%d vertex=%d player=%d",row,col,vertex,playerID);
    }
}

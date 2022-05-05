package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;
import games.catan.components.Settlement;

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

        Settlement settlement = board[row][col].getSettlements()[vertex];
        if (settlement != null) {
            if (settlement.getOwner() == playerID) {
                if (((Counter)cgs.getComponentActingPlayer(CatanConstants.cityCounterHash)).isMaximum()){
                    throw new AssertionError("Player cannot build anymore cities");
                }
                ((Counter)cgs.getComponentActingPlayer(CatanConstants.cityCounterHash)).increment(1);
                // if player builds a city it gets back the settlement token
                ((Counter)cgs.getComponentActingPlayer(CatanConstants.settlementCounterHash)).decrement(1);
                if (!CatanGameState.spendResources(cgs, CatanParameters.costMapping.get("city"))) {
                    throw new AssertionError("Player cannot afford city");
                }
                settlement.upgrade();
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
        return String.format("BuildCity: row=%d col=%d vertex=%d player=%d",row,col,vertex,playerID);
    }
}

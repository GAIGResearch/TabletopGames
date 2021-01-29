package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;
import games.catan.components.Settlement;

import static core.CoreConstants.VERBOSE;

public class BuildCity extends AbstractAction {
    int row;
    int col;
    int vertex;
    int playerID;

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
                    if (VERBOSE)
                        System.out.println("No more cities to build for player " + gs.getCurrentPlayer());
                    return false;
                }
                ((Counter)cgs.getComponentActingPlayer(CatanConstants.cityCounterHash)).increment(1);
                // if player builds a city it gets back the settlement token
                ((Counter)cgs.getComponentActingPlayer(CatanConstants.settlementCounterHash)).decrement(1);
                if (!CatanGameState.spendResources(cgs, CatanParameters.costMapping.get("city"))) return false;
                settlement.upgrade();
                return true;
            }
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new BuildCity(row, col, vertex, playerID);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other instanceof BuildCity){
            BuildCity otherAction = (BuildCity)other;
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

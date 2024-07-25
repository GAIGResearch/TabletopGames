package games.catan.actions.build;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Counter;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.Building;
import games.catan.components.CatanTile;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static games.catan.stats.CatanMetrics.CatanEvent.PortSettle;

public class BuildSettlement extends AbstractAction {
    public final int x;
    public final int y;
    public final int vertex;
    public final int playerID;
    public final boolean free;

    public BuildSettlement(int x, int y, int vertex, int playerID, boolean free){
        this.x = x;
        this.y = y;
        this.vertex = vertex;
        this.playerID = playerID;
        this.free = free;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();
        CatanParameters cp = (CatanParameters) gs.getGameParameters();

        Building settlement = cgs.getBuilding(board[x][y], vertex);
        if (settlement != null && settlement.getOwnerId() == -1) {
            if (!free) {
                if (!cgs.spendResourcesIfPossible(cp.costMapping.get(BuyAction.BuyType.Settlement), playerID)) {
                    throw new AssertionError("Player " + gs.getCurrentPlayer() + " cannot afford this settlement");
                }
            }

            Counter settleTokens = cgs.getPlayerTokens().get(playerID).get(BuyAction.BuyType.Settlement);
            if (settleTokens.isMaximum()){
                throw new AssertionError("No more settlements to build for player " + gs.getCurrentPlayer());
            }
            settleTokens.increment();

            settlement.setOwnerId(playerID);
            if(settlement.getHarbour() != null){
                gs.logEvent(PortSettle, String.valueOf(playerID));

                Map<CatanParameters.Resource, Counter> exchangeRates = cgs.getExchangeRates(playerID);
                CatanParameters.Resource harbour = settlement.getHarbour();
                int newRate = cp.harbour_exchange_rate;
                if (harbour == CatanParameters.Resource.WILD) newRate = cp.harbour_wild_exchange_rate;
                exchangeRates.get(harbour).setValue(Math.min(exchangeRates.get(harbour).getValue(), newRate));
            }
            cgs.addScore(playerID, cp.buildingValue.get(Building.Type.Settlement));

            return true;
        } else {
            throw new AssertionError("Settlement already owned: " + this);
        }
    }

    @Override
    public BuildSettlement copy() {
        return this;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof BuildSettlement){
            BuildSettlement otherAction = (BuildSettlement)other;
            return x == otherAction.x && y == otherAction.y && vertex == otherAction.vertex && playerID == otherAction.playerID && free == otherAction.free;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, vertex, playerID, free);
    }

    @Override
    public String toString() {
        return "p" + playerID + " Buy:Settlement (x= " + x + " y= " + y + " vertex= " + vertex + ")";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

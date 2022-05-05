package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import games.catan.CatanConstants;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.CatanTile;

import java.util.Arrays;
import java.util.Objects;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.resourceDeckHash;

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

        if (board[x][y].getSettlements()[vertex].getOwner() == -1) {
            if (((Counter)cgs.getComponentActingPlayer(CatanConstants.settlementCounterHash)).isMaximum()){
                throw new AssertionError("No more settlements to build for player " + gs.getCurrentPlayer());
            }
            ((Counter)cgs.getComponentActingPlayer(CatanConstants.settlementCounterHash)).increment(1);
            // take resources after set up
            if (!free){
                if (!CatanGameState.spendResources(cgs, CatanParameters.costMapping.get("settlement"))) {
                    throw new AssertionError("Player " + gs.getCurrentPlayer() + " cannot afford this settlement");
                }
            }
            board[x][y].addSettlement(vertex, playerID);
            if(board[x][y].getSettlements()[vertex].getHarbour()!=null){
                int defaultExchangeRate = ((CatanParameters)cgs.getGameParameters()).default_exchange_rate;
                int[] exchangeRates = cgs.getExchangeRates(playerID);
                switch (board[x][y].getSettlements()[vertex].getHarbour()){
                    case BRICK:
                        exchangeRates[0] = defaultExchangeRate - 2;
                        break;
                    case LUMBER:
                        exchangeRates[1] = defaultExchangeRate - 2;
                        break;
                    case ORE:
                        exchangeRates[2] = defaultExchangeRate - 2;
                        break;
                    case GRAIN:
                        exchangeRates[3] = defaultExchangeRate - 2;
                        break;
                    case WOOL:
                        exchangeRates[4] = defaultExchangeRate - 2;
                        break;
                    case GENERIC:
                        for (int i = 0; i < exchangeRates.length; i++){
                            if(exchangeRates[i] > defaultExchangeRate - 1){
                                exchangeRates[i] = defaultExchangeRate - 1;
                            }
                        }
                }
                cgs.updateExchangeRates(playerID,exchangeRates);
            }
            return true;
        } else {
            throw new AssertionError("Settlement already owned: " + this.toString());
        }
    }

    @Override
    public AbstractAction copy() {
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
        return "BuildSettlement x= " + x + " y= " + y + " vertex= " + vertex + " free= " + free;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

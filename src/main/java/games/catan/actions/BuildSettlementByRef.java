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

// Builds a Settlement by reference
public class BuildSettlementByRef extends AbstractAction {
    Settlement settlement;
    int owner;

    public BuildSettlementByRef(Settlement settlement, int owner){
        this.settlement = settlement;
        this.owner = owner;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        CatanGameState cgs = (CatanGameState)gs;
        CatanTile[][] board = cgs.getBoard();
        for (Settlement settl: cgs.getSettlements()){
            if (settl.equals(settlement)){
                if (settl.getOwner() == -1){
                    if (((Counter)cgs.getComponentActingPlayer(CatanConstants.settlementCounterHash)).isMaximum()){
                        if (VERBOSE)
                            System.out.println("No more settlements to build for player " + gs.getCurrentPlayer());
                        return false;
                    }
                    ((Counter)cgs.getComponentActingPlayer(CatanConstants.settlementCounterHash)).increment(1);
                    // take resources after second round
                    if (cgs.getTurnOrder().getRoundCounter() >= 2) {
                        if (!CatanGameState.spendResources(cgs, CatanParameters.costMapping.get("settlement")))
                            return false;
                    }
                    settl.setOwner(owner);
                    return true;
                }
            }
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
        if (other instanceof BuildSettlementByRef){
            BuildSettlementByRef otherAction = (BuildSettlementByRef)other;
            return owner == otherAction.owner && settlement.equals(otherAction.settlement);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "BuildSettlement Settlement= " + settlement.toString() + " owner = " + owner;
    }
}

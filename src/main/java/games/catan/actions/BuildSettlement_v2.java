package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.CatanGameState;
import games.catan.CatanTile;
import games.catan.components.Settlement;

public class BuildSettlement_v2 extends AbstractAction {
    Settlement settlement;
    int owner;

    public BuildSettlement_v2(Settlement settlement, int owner){
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
        if (other instanceof BuildSettlement_v2){
            BuildSettlement_v2 otherAction = (BuildSettlement_v2)other;
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

package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.catan.components.Road;
import games.catan.components.Settlement;

/*
* Class to execute both placing a settlement and a road at the same time instead of doing it as a 2 step process
*  */
public class PlaceSettlementWithRoad extends AbstractAction {
    BuildSettlement bs;
    BuildRoad br;

    public PlaceSettlementWithRoad(BuildSettlement bs, BuildRoad br){
        this.bs = bs;
        this.br = br;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        if (this.bs.execute(gs) && this.br.execute(gs)){
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
        if (other instanceof PlaceSettlementWithRoad){
            PlaceSettlementWithRoad otherAction = (PlaceSettlementWithRoad)other;
            return bs.equals(otherAction.bs) && br.equals(otherAction.br);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "PlaceSettlementWithRoad settlement = " + bs.toString() + " and road = " + br.toString();
    }
}

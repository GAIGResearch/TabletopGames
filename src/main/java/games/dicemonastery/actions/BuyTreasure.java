package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.components.Treasure;

public class BuyTreasure extends UseMonk{

    public final Treasure treasure;

    public BuyTreasure(Treasure treasure) {
        super(2);
        this.treasure = treasure;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BuyTreasure) {
            return ((BuyTreasure)obj).treasure == treasure;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return treasure.hashCode() - 29291430;
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        int player = state.getCurrentPlayer();
        state.acquireTreasure(treasure, player);
        state.addResource(player, DiceMonasteryConstants.Resource.SHILLINGS, -treasure.cost);
        return true;
    }

    @Override
    public String toString() {
        return "Buy " + treasure.getComponentName();
    }
}

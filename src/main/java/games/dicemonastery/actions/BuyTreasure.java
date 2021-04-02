package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.TREASURE;

public class BuyTreasure extends UseMonk{

    public final TREASURE treasure;

    public BuyTreasure(TREASURE treasure) {
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
        return treasure.hashCode() * 7;
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        int player = state.getCurrentPlayer();
        state.acquireTreasure(treasure, player);
        state.addResource(player, DiceMonasteryConstants.Resource.SHILLINGS, -treasure.cost);
        return true;
    }
}

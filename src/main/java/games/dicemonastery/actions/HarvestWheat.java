package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class HarvestWheat extends UseMonk {

    public HarvestWheat() {
        super(1);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), Resource.GRAIN, MEADOW, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), Resource.GRAIN, SUPPLY, STOREROOM);
        return true;
    }

    @Override
    public String toString() {
        return "Harvest Wheat";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HarvestWheat;
    }

    @Override
    public int hashCode() {
        return 398321;
    }

}

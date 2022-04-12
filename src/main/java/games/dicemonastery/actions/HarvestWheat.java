package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class HarvestWheat extends UseMonk {

    public HarvestWheat(int count) {
        super(count);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        for (int i = 0; i < actionPoints; i++) {
            state.moveCube(state.getCurrentPlayer(), Resource.GRAIN, MEADOW, STOREROOM);
            state.moveCube(state.getCurrentPlayer(), Resource.GRAIN, SUPPLY, STOREROOM);
        }
        return true;
    }

    @Override
    public String toString() {
        return "Harvest Wheat " + actionPoints + " times";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof HarvestWheat && ((HarvestWheat) obj).actionPoints == actionPoints;
    }

    @Override
    public int hashCode() {
        return 398321 + actionPoints;
    }

}

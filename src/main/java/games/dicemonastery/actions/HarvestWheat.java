package games.dicemonastery.actions;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class HarvestWheat extends MoveCube {

    public HarvestWheat() {
        super(1, Resource.GRAIN, MEADOW, STOREROOM);
    }

    @Override
    public String toString() {
        return "Harvest Wheat";
    }
}

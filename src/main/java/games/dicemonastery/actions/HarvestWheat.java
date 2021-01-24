package games.dicemonastery.actions;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.MEADOW;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.SUPPLY;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class HarvestWheat extends MoveCube {

    public HarvestWheat() {
        super(1, Resource.GRAIN, MEADOW, SUPPLY);
    }

    @Override
    public String toString() {
        return "Harvest Wheat";
    }
}

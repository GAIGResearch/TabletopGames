package games.dicemonastery.actions;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public class SowWheat extends MoveCubes {

    public SowWheat() {
        super(1, Resource.GRAIN, SUPPLY, MEADOW);
    }

    @Override
    public String toString() {
        return "Sow Wheat";
    }
}

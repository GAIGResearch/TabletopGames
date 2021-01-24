package games.dicemonastery.actions;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public class PlaceSkep extends MoveCube {

    public PlaceSkep() {
        super(1, Resource.SKEP, SUPPLY, MEADOW);
    }

    @Override
    public String toString() {
        return "Place Skep";
    }
}

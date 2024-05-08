package games.dicemonastery.actions;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public class PlaceSkep extends MoveCubes {

    public PlaceSkep() {
        super(1, Resource.SKEP, STOREROOM, MEADOW);
    }

    @Override
    public String toString() {
        return "Place Skep";
    }
}

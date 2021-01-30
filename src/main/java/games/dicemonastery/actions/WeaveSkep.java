package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryConstants;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public class WeaveSkep extends MoveCube{

    public WeaveSkep(){
        super(1, Resource.SKEP, SUPPLY, STOREROOM);
    }

    @Override
    public String toString() {
        return "Weave Skep";
    }
}

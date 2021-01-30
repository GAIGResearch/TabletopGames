package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryConstants;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class BegForAlms extends MoveCube {

    public BegForAlms() {
        super(1, SHILLINGS, SUPPLY, STOREROOM);
    }

    @Override
    public String toString() {
        return "Beg for Alms";
    }
}

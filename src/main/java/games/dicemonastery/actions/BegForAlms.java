package games.dicemonastery.actions;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class BegForAlms extends MoveCubes {

    public BegForAlms(int times) {
        super(times, SHILLINGS, SUPPLY, STOREROOM);
    }

    @Override
    public String toString() {
        return "Beg for Alms " + actionPoints ;
    }
}
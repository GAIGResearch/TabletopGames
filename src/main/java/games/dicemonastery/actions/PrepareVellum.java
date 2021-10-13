package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.SUPPLY;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class PrepareVellum extends UseMonk {

    public PrepareVellum(int actionPoints) {
        super(actionPoints);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), Resource.CALF_SKIN, STOREROOM, SUPPLY);
        state.moveCube(state.getCurrentPlayer(), Resource.VELLUM, SUPPLY, STOREROOM);
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof PrepareVellum;
    }

    @Override
    public int hashCode() {
        return 3094;
    }

    @Override
    public String toString() {
        return "Make Vellum";
    }
}


package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public class BakeBread extends UseMonk {


    public BakeBread(int actionPoints) {
        super(actionPoints);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), Resource.GRAIN, STOREROOM, SUPPLY);
        state.moveCube(state.getCurrentPlayer(), Resource.BREAD, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), Resource.BREAD, SUPPLY, STOREROOM);
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof BakeBread;
    }

    @Override
    public int hashCode() {
        return 30913;
    }

    @Override
    public String toString() {
        return "Bake Bread";
    }
}

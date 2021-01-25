package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public class PrepareInk extends UseMonk {

    public PrepareInk() {
        super(2);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), Resource.PIGMENT, STOREROOM, SUPPLY);
        state.moveCube(state.getCurrentPlayer(), Resource.INK, SUPPLY, STOREROOM);
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof PrepareInk;
    }

    @Override
    public int hashCode() {
        return 1913;
    }

    @Override
    public String toString() {
        return "Prepare Ink";
    }
}


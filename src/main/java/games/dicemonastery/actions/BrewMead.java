package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;

public class BrewMead extends UseMonk{
    public BrewMead() {
        super(2);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), Resource.HONEY, STOREROOM, SUPPLY);
        state.moveCube(state.getCurrentPlayer(), Resource.MEAD, SUPPLY, STOREROOM);
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof BrewMead;
    }

    @Override
    public int hashCode() {
        return 13919;
    }

    @Override
    public String toString() {
        return "Brew Mead";
    }
}

package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class CollectSkep extends UseMonk {

    public CollectSkep() {
        super(1);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), Resource.SKEP, MEADOW, SUPPLY);
        state.moveCube(state.getCurrentPlayer(), Resource.WAX, SUPPLY, STOREROOM);
        state.moveCube(state.getCurrentPlayer(), Resource.HONEY, SUPPLY, STOREROOM);
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof CollectSkep;
    }

    @Override
    public int hashCode() {
        return 30243;
    }

    @Override
    public String toString() {
        return "Collect Skep";
    }
}

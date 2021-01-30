package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.SUPPLY;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class MakeCandle extends UseMonk {

    public MakeCandle() {
        super(2);
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(state.getCurrentPlayer(), Resource.WAX, STOREROOM, SUPPLY);
        state.moveCube(state.getCurrentPlayer(), Resource.CANDLE, SUPPLY, STOREROOM);
        return true;
    }


    @Override
    public boolean equals(Object obj) {
        return obj instanceof MakeCandle;
    }

    @Override
    public int hashCode() {
        return 18213;
    }

    @Override
    public String toString() {
        return "Make Candle";
    }
}


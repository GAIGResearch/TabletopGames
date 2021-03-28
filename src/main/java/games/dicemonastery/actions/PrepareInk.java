package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.SUPPLY;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class PrepareInk extends UseMonk {

    public final Resource pigment;

    public PrepareInk(Resource pigment) {
        super(2);
        this.pigment = pigment;
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        int player = state.getCurrentPlayer();
        Resource ink = pigment.processedTo;
        state.moveCube(player, pigment, STOREROOM, SUPPLY);
        state.moveCube(player, ink, SUPPLY, STOREROOM);
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


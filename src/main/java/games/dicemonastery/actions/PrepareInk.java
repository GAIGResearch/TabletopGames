package games.dicemonastery.actions;

import core.AbstractGameState;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.SUPPLY;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class PrepareInk extends UseMonk {

    public final Resource pigment;

    public PrepareInk(Resource pigment, int actionPoints) {
        super(actionPoints);
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
        if (obj instanceof PrepareInk) {
            PrepareInk other = (PrepareInk) obj;
            return other.pigment == pigment;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return pigment.ordinal() - 2957;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Prepare Ink using " + pigment;
    }
}


package games.dicemonastery.actions;

import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.MEADOW;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.SUPPLY;
import static games.dicemonastery.DiceMonasteryConstants.Resource;

public class TakePigment extends UseMonk {

    public final Resource pigment;

    public TakePigment(Resource pigment, int actionPoints) {
        super(actionPoints);
        this.pigment = pigment;
    }

    @Override
    public boolean _execute(DiceMonasteryGameState state) {
        state.moveCube(-1, pigment, MEADOW, SUPPLY);
        state.addResource(state.getCurrentPlayer(), pigment, 1);
        return true;
    }

    @Override
    public String toString() {
        return "Take " + pigment;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof TakePigment && ((TakePigment) obj).pigment == pigment;
    }

    @Override
    public int hashCode() {
        return 38321 + pigment.ordinal();
    }

}

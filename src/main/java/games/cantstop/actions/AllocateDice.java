package games.cantstop.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.cantstop.CantStopGameState;

public class AllocateDice extends AbstractAction {

    final int[] numberSplit;

    public AllocateDice(int valueOne, int valueTwo) {
        numberSplit = new int[]{valueOne, valueTwo};
    }

    public boolean isLegal(CantStopGameState state) {
        // this is legal as long as at least one of the tracks is not yet completed
        return !(state.trackComplete(numberSplit[0]) && state.trackComplete(numberSplit[1]));
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        CantStopGameState state = (CantStopGameState) gs;
        if (state.trackComplete(numberSplit[0]) && state.trackComplete(numberSplit[1]))
            throw new AssertionError("At least one of the two moves needs to be valid for " + this);
        state.moveMarker(numberSplit[0]);
        state.moveMarker(numberSplit[1]);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof AllocateDice) {
            AllocateDice other = (AllocateDice) obj;
            return (numberSplit[0] == other.numberSplit[0] && numberSplit[1] == other.numberSplit[1]) ||
                    (numberSplit[0] == other.numberSplit[1] && numberSplit[1] == other.numberSplit[0]);
        }
        // for equals and hashcode, we only are about the two numbers - not the precise permutation
        return false;
    }

    @Override
    public int hashCode() {
        if (numberSplit[0] < numberSplit[1])
            return -31 * numberSplit[0] - 255 * numberSplit[1];
        return -31 * numberSplit[1] - 255 * numberSplit[0];
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Use pairs of dice : " + numberSplit[0] + " - " + numberSplit[1];
    }
}

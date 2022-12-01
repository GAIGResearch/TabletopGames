package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sirius.SiriusGameState;

public class MoveToMoon extends AbstractAction {

    public final int destinationMoon;

    public MoveToMoon(int to) {
        destinationMoon = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        int player = state.getCurrentPlayer();
        if (state.getLocationIndex(player) == destinationMoon) {
            throw new AssertionError("Cannot move to current location : " + state.getMoon(destinationMoon));
        }
        state.chooseMoveCard(destinationMoon);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof MoveToMoon) {
            MoveToMoon other = (MoveToMoon) obj;
            return other.destinationMoon == destinationMoon;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return destinationMoon * 3872;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        SiriusGameState state = (SiriusGameState) gameState;
        return "Move to " + state.getMoon(destinationMoon);
    }

    @Override
    public String toString() {
        return "Move to " + destinationMoon;
    }
}

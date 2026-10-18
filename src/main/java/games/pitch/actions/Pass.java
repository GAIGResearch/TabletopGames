package games.pitch.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.pitch.PitchGameState;

public class Pass extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        PitchGameState state = (PitchGameState) gs;
        state.setPlayerBid(state.getCurrentPlayer(), 0);
        return true;
    }

    @Override
    public Pass copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Pass;
    }

    @Override
    public int hashCode() {
        return 830211;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Pass";
    }
}

package games.hareandtortoise.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.hareandtortoise.HareAndTortoiseGameState;
import games.hareandtortoise.HareAndTortoiseParameters;

/**
 * The current player chews a carrot: draws HareAndTortoiseParameters.carrotsPerChew carrots, or pays them in.
 */
public class ChewCarrot extends AbstractAction {

    public final boolean draw;

    public ChewCarrot(boolean draw) {
        this.draw = draw;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) gs;
        int amount = ((HareAndTortoiseParameters) state.getGameParameters()).carrotsPerChew;
        state.addCarrots(state.getCurrentPlayer(), draw ? amount : -amount);
        return true;
    }

    @Override
    public ChewCarrot copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChewCarrot other && other.draw == draw;
    }

    @Override
    public int hashCode() {
        return draw ? 480131 : 480137;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        int amount = ((HareAndTortoiseParameters) gameState.getGameParameters()).carrotsPerChew;
        return draw ? "Chew a carrot: draw " + amount : "Chew a carrot: pay " + amount;
    }

    @Override
    public String toString() {
        return draw ? "Chew a carrot: draw" : "Chew a carrot: pay";
    }
}

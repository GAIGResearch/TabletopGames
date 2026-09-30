package games.hareandtortoise.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.hareandtortoise.HareAndTortoiseGameState;

/**
 * The current player, whose runner has just reached a lettuce square, discards a lettuce and draws carrots for
 * their race position. The runner stays where it is.
 */
public class ChewLettuce extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        HareAndTortoiseGameState state = (HareAndTortoiseGameState) gs;
        int player = state.getCurrentPlayer();
        state.chewLettuce(player);
        state.setLettuceToChew(player, false);
        return true;
    }

    @Override
    public ChewLettuce copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChewLettuce;
    }

    @Override
    public int hashCode() {
        return 480127;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Chew a lettuce";
    }
}

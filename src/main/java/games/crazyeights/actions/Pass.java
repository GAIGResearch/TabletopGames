package games.crazyeights.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.Objects;

/**
 * Pass the turn. Only available when no card can be played and none can be drawn.
 */
public class Pass extends AbstractAction {

    public final int player;

    public Pass(int player) {
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Nothing changes: the forward model ends the turn
        return true;
    }

    @Override
    public Pass copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pass that && player == that.player;
    }

    @Override
    public int hashCode() {
        return Objects.hash(player, "Pass");
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

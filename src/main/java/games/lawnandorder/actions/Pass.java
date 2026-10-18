package games.lawnandorder.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.lawnandorder.LawnAndOrderGameState;

/**
 * The player passes (Step 6). They draw nothing, and are immune to the rest of the round's reveals.
 */
public class Pass extends AbstractAction {
    // All active players choose at once, so the action carries the player
    public final int player;

    public Pass(int player) {
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((LawnAndOrderGameState) gs).setDecision(player, LawnAndOrderGameState.Decision.PASS);
        return true;
    }

    @Override
    public Pass copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Pass other && other.player == player;
    }

    @Override
    public int hashCode() {
        return player + 618227;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pass";
    }

    @Override
    public String toString() {
        return "P" + player + " passes";
    }
}

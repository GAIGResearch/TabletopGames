package games.lawnandorder.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.lawnandorder.LawnAndOrderGameState;

/**
 * The player continues (Step 6). They will draw a card and play again next turn.
 */
public class Continue extends AbstractAction {
    // All active players choose at once, so the action carries the player
    public final int player;

    public Continue(int player) {
        this.player = player;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        ((LawnAndOrderGameState) gs).setDecision(player, LawnAndOrderGameState.Decision.CONTINUE);
        return true;
    }

    @Override
    public Continue copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Continue other && other.player == player;
    }

    @Override
    public int hashCode() {
        return player + 618211;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Continue";
    }

    @Override
    public String toString() {
        return "P" + player + " continues";
    }
}

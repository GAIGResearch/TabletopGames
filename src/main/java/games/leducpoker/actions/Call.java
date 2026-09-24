package games.leducpoker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.leducpoker.LeducPokerGameState;

/**
 * The current player matches the other player's contribution to the pot - a check when nothing is owed.
 */
public class Call extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        LeducPokerGameState state = (LeducPokerGameState) gs;
        int player = state.getCurrentPlayer();
        state.addToPot(player, state.amountToCall(player));
        return true;
    }

    @Override
    public Call copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Call;
    }

    @Override
    public int hashCode() {
        return 604213;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        LeducPokerGameState state = (LeducPokerGameState) gameState;
        int owed = state.amountToCall(state.getCurrentPlayer());
        return owed == 0 ? "Check" : "Call " + owed;
    }

    @Override
    public String toString() {
        return "Call";
    }
}

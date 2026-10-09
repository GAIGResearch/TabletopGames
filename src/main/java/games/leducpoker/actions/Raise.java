package games.leducpoker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.leducpoker.LeducPokerGameState;
import games.leducpoker.LeducPokerParameters;

/**
 * The current player matches the other player's contribution and adds the betting round's raise amount.
 */
public class Raise extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        LeducPokerGameState state = (LeducPokerGameState) gs;
        int player = state.getCurrentPlayer();
        state.addToPot(player, state.amountToCall(player) + raiseAmount(state));
        state.recordRaise();
        return true;
    }

    private static int raiseAmount(LeducPokerGameState state) {
        LeducPokerParameters params = (LeducPokerParameters) state.getGameParameters();
        return params.raiseAmount(state.getBettingRound());
    }

    @Override
    public Raise copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Raise;
    }

    @Override
    public int hashCode() {
        return 604217;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        LeducPokerGameState state = (LeducPokerGameState) gameState;
        int owed = state.amountToCall(state.getCurrentPlayer());
        return (owed == 0 ? "Bet " : "Raise ") + (owed + raiseAmount(state));
    }

    @Override
    public String toString() {
        return "Raise";
    }
}

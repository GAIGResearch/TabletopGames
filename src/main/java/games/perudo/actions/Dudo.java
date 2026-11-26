package games.perudo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import games.perudo.PerudoGameState;

/**
 * Call Dudo: doubt the current bid.
 */
public class Dudo extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        PerudoGameState state = (PerudoGameState) gs;
        int caller = state.getCurrentPlayer();
        int bidder = state.getBidder();
        int count = state.countMatching(state.getBidFace());
        // an exact count makes the bid right, so the caller loses
        int loser = count < state.getBidQuantity() ? bidder : caller;
        state.logEvent(Event.GameEvent.GAME_EVENT, () -> "Dudo on "
                + Bid.describe(state.getBidQuantity(), state.getBidFace()) + ": " + state.diceOnTable()
                + "; " + count + " counted, player " + loser + " loses a die");
        state.removeDie(loser);
        state.clearBid();
        state.setNextRoundStarter(loser);  // the player who lost the die starts the next round
        return true;
    }

    @Override
    public Dudo copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Dudo;
    }

    @Override
    public int hashCode() {
        return 581201;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Dudo";
    }
}

package games.perudo.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import games.perudo.PerudoGameState;

/**
 * Call Calza: claim the current bid is exactly right. Only the caller gains or loses a die.
 */
public class Calza extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        PerudoGameState state = (PerudoGameState) gs;
        int caller = state.getCurrentPlayer();
        int count = state.countMatching(state.getBidFace());
        boolean exact = count == state.getBidQuantity();
        state.logEvent(Event.GameEvent.GAME_EVENT, () -> "Calza on "
                + Bid.describe(state.getBidQuantity(), state.getBidFace()) + ": " + state.diceOnTable()
                + "; " + count + " counted, player " + caller + (exact ? " is right" : " loses a die"));
        if (!exact)
            state.removeDie(caller);
        else if (state.getNDice(caller) < state.getParams().nDicePerPlayer)  // no gain above the starting dice
            state.addDie(caller);
        state.clearBid();
        state.setNextRoundStarter(caller);  // right or wrong, the caller starts the next round
        return true;
    }

    @Override
    public Calza copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Calza;
    }

    @Override
    public int hashCode() {
        return 581203;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Calza";
    }
}

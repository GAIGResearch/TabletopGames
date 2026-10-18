package games.pitch.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.pitch.PitchGameState;

/**
 * The current player bids the number of points their team will take this deal.
 */
public class Bid extends AbstractAction {

    public final int amount;

    public Bid(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PitchGameState state = (PitchGameState) gs;
        int player = state.getCurrentPlayer();
        state.setPlayerBid(player, amount);
        // a legal bid is always the highest so far (the dealer's steal equals it, and the dealer bids last)
        state.setPitcher(player);
        return true;
    }

    @Override
    public Bid copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Bid other && other.amount == amount;
    }

    @Override
    public int hashCode() {
        return 830213 + 31 * amount;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Bid " + amount;
    }
}

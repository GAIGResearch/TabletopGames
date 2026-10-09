package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * The current bidder in an Auction passes, and is out of that auction (the auction records it).
 */
public class PassBid extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public PassBid copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PassBid;
    }

    @Override
    public int hashCode() {
        return 640239;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pass";
    }

    @Override
    public String toString() {
        return "PassBid";
    }
}

package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * The current bidder in an Auction bids the amount (the auction records it).
 */
public class Bid extends AbstractAction {

    public final int amount;

    public Bid(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public Bid copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Bid b && b.amount == amount;
    }

    @Override
    public int hashCode() {
        return 31 * amount + 640237;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Bid " + amount;
    }

    @Override
    public String toString() {
        return "Bid " + amount;
    }
}

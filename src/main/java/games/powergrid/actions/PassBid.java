package games.powergrid.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

import java.util.Objects;

/**
 * Action indicating that a player passes on making a higher bid in the current auction.
 * <p>
 * When executed, this marks the player as having passed for the ongoing auction round,
 * so they will no longer be considered when selecting the next bidder unless the
 * auction state resets.
 *
 * <p><b>Side effects:</b> Mutates {@link PowerGridGameState} by updating the auction's
 * internal bid order (e.g., setting the player's entry to {@code -1}).
 *
 * @see PowerGridGameState#passBid(int)
 * @see games.powergrid.actions.AuctionPowerPlant
 */

public class PassBid extends AbstractAction {
    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState s = (PowerGridGameState) gs;
        if (!s.isAuctionLive()) return false;   // optional guard
        int me = s.getCurrentPlayer();
        s.passBid(me);
        return true;
    }

    @Override
    public AbstractAction copy() { return new PassBid(); }

    @Override public boolean equals(Object o) { return o instanceof PassBid; }
    @Override public int hashCode() { return  0xA55B1D; }

    @Override
    public String getString(AbstractGameState gameState) { return "Pass Bid"; }
}



package games.powergrid.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

import java.util.Objects;

/**
 * Action that raises the current auction bid by one unit for a specific player.
 * <p>
 * When executed during an active auction, this action computes {@code newBid = currentBid + 1}
 * and, if the acting player can afford it, updates the game state's current bid and bidder.
 * If the player cannot afford the increment, the bid is not changed and the action returns {@code false}.
 *
 * <p><b>Side effects:</b> Mutates {@link PowerGridGameState} by updating the highest bid and
 * the current highest bidder.
 *
 * @see games.powergrid.actions.AuctionPowerPlant
 * @see PowerGridGameState
 */

public class IncreaseBid extends AbstractAction {
    private int playerId = -1; //TODO get rid of this if able

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState s = (PowerGridGameState) gs;
        if (!s.isAuctionLive()) return false;
        if (playerId < 0) playerId = s.getCurrentPlayer();

        int newBid = s.getCurrentBid() + 1;
        if (s.getPlayersMoney(playerId) >= newBid) {
            s.setCurrentBid(newBid, playerId);
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        IncreaseBid c = new IncreaseBid();
        c.playerId = this.playerId;
        return c;
    }

    // Option A: treat all IncreaseBid as equal
    @Override public boolean equals(Object o) { return o instanceof IncreaseBid; }
    @Override public int hashCode() { return 0x1A2B3C; }

    // Null-safe label
    @Override
    public String getString(AbstractGameState gameState) {
        if (gameState instanceof PowerGridGameState s) {
            return "Increase bid to " + (s.getCurrentBid() + 1);
        }
        return "Increase bid by 1";
    }

    public String getName(AbstractGameState gameState) { return "Increase Bid"; }

    public int getPlayerId() { return playerId; }
}


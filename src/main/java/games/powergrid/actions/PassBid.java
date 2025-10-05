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
    private final int playerId;

    public PassBid(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;
        pggs.passBid(playerId); //marks player as passed 
        return true;  
    }

    @Override
    public AbstractAction copy() {
        return new PassBid(playerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PassBid)) return false;
        PassBid other = (PassBid) obj;
        return this.playerId == other.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return String.format("Has elected to not bid");
    }

    public int getPlayerId() {
    	return this.playerId;
    }
}

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
	
    private final int playerId;

    public IncreaseBid(int playerId) {
        this.playerId = playerId;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PowerGridGameState pggs = (PowerGridGameState) gs;

        int currentBid = pggs.getCurrentBid();
        int newBid = currentBid + 1;

        // Check if the player can afford the new bid
        if (pggs.getPlayersMoney(playerId) >= newBid) {
            pggs.setCurrentBid(newBid, playerId);  
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return new IncreaseBid(playerId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof IncreaseBid)) return false;
        IncreaseBid other = (IncreaseBid) obj;
        return this.playerId == other.playerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        PowerGridGameState pggs = (PowerGridGameState) gameState;
        return String.format("Increases bid to " +  (pggs.getCurrentBid() + 1));
    }
    
    public int getPlayerId() {
    	return this.playerId;
    }


}

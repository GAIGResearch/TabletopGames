package games.powergrid.actions;

import java.util.Objects;

import core.AbstractGameState;
import java.util.List;
import java.util.ArrayList;

import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

public class BidPowerPlant extends AbstractAction {
    private final int playerId;
    private final int plantNumber;  // unique number on the card
    private final int bidAmount;

    public BidPowerPlant(int playerId, int plantNumber, int bidAmount) {
        this.playerId = playerId;
        this.plantNumber = plantNumber;
        this.bidAmount = bidAmount;
    }
	@Override
	public boolean execute(AbstractGameState gs) {
	    PowerGridGameState pggs = (PowerGridGameState) gs;

	    // Sanity checks
	    if (bidAmount > pggs.getPlayersMoney(playerId)) {
	        return false;
	    }

	    // Case 1: Opening bid (auction not live yet)
	    if (!pggs.isAuctionLive()) {
	        pggs.setAuctionPlantNumber(plantNumber);
	        pggs.setCurrentBid(bidAmount, playerId);

	        // seed active bidders in turn-order starting after opener, then (optionally) add opener at end
	        List<Integer> order = new ArrayList<>(pggs.getTurnOrder());
	        int idx = order.indexOf(playerId);
	        List<Integer> cycle = new ArrayList<>();
	        int minRaise = Math.max(bidAmount + 1, plantNumber);
	        for (int k = 1; k <= order.size(); k++) {
	            int pid = order.get((idx + k) % order.size()); // starts with next clockwise
	            if (pggs.getPlayersMoney(pid) >= minRaise) cycle.add(pid);
	        }
	        // include opener at the end so they can re-enter later if they can afford a future raise
	        cycle.add(playerId);
	        pggs.startAuction(cycle);

	        System.out.printf("Player %d opens auction on plant %d for %d%n", playerId, plantNumber, bidAmount);
	        return true;
	    }

	    // Case 2: Raise bid in a live auction
	    if (plantNumber == pggs.getAuctionPlantNumber()
	            && bidAmount > pggs.getCurrentBid()) {
	        pggs.setCurrentBid(bidAmount, playerId);
	        System.out.printf("Player %d raises bid to %d on plant %d%n",
	                          playerId, bidAmount, plantNumber);
	        return true;
	    }

	    return false; // illegal bid
	}

	@Override
	public AbstractAction copy() {
	    return new BidPowerPlant(playerId, plantNumber, bidAmount);
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj == null || getClass() != obj.getClass()) return false;
	    BidPowerPlant other = (BidPowerPlant) obj;
	    return playerId == other.playerId &&
	           plantNumber == other.plantNumber &&
	           bidAmount == other.bidAmount;
	}

	@Override
	public int hashCode() {
	    return Objects.hash(playerId, plantNumber, bidAmount);
	}

	@Override
	public String getString(AbstractGameState gameState) {
	    return String.format("P%d bids %d on plant %d", playerId, bidAmount, plantNumber);
	}


}

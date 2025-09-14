package games.powergrid.actions;

import java.util.Objects;

import core.AbstractGameState;
import java.util.List;
import java.util.ArrayList;

import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;

public class AuctionPowerPlant extends AbstractAction {
    private final int playerId;
    private final int plantNumber;  // unique number on the card


    public AuctionPowerPlant(int playerId, int plantNumber) {
        this.playerId = playerId;
        this.plantNumber = plantNumber;
    }
	@Override
	public boolean execute(AbstractGameState gs) {
	    PowerGridGameState pggs = (PowerGridGameState) gs;
	    //when the step is not 1 the first card initial auction value is 1 this eventually needs to be figured out 
	  
	    // If the plant has a higher number than the amount of money you have you can't bid 
	    if (plantNumber > pggs.getPlayersMoney(playerId)) {
	        return false;
	    }
        pggs.setAuctionPlantNumber(plantNumber);

        System.out.printf("Player %d opens auction on plant %d%n", playerId, plantNumber);

        return true;

	}

	@Override
	public AbstractAction copy() {
	    return new AuctionPowerPlant(playerId, plantNumber);
	}

	@Override
	public boolean equals(Object obj) {
	    if (this == obj) return true;
	    if (obj == null || getClass() != obj.getClass()) return false;
	    AuctionPowerPlant other = (AuctionPowerPlant) obj;
	    return playerId == other.playerId &&
	           plantNumber == other.plantNumber;
	}

	@Override
	public int hashCode() {
	    return Objects.hash(playerId, plantNumber);
	}

	@Override
	public String getString(AbstractGameState gameState) {
	    return String.format("P%d opens auction on plant %d", playerId, plantNumber);
	}



}

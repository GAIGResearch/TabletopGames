package games.powergrid.actions;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridResourceMarket;
import core.AbstractGameState;
import core.actions.AbstractAction;

public class BuyResource extends AbstractAction {
	private final Resource resource; 
	private final int amount; 
	
    public BuyResource(Resource resource, int amount) {
        this.resource = resource;
        this.amount = amount;
    }
	@Override
	public boolean execute(AbstractGameState gs) {
		PowerGridGameState pggs = (PowerGridGameState) gs;
		PowerGridResourceMarket resourceMarket = pggs.getResourceMarket(); 
	    try {
	        resourceMarket.buy(this.resource, this.amount);
	        return true; 
	    } catch (IllegalArgumentException e) {
	        // if the buy action is illegal it catches the failure
	        System.err.println("BuyResource failed: " + e.getMessage());
	        return false; 
	    }
	}

	@Override
	public AbstractAction copy() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean equals(Object obj) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int hashCode() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getString(AbstractGameState gameState) {
		// TODO Auto-generated method stub
		return null;
	}

}

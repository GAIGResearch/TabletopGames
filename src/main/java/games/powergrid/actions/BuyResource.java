package games.powergrid.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridResourceMarket;

import java.util.Objects;

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
        	int playerId = pggs.getCurrentPlayer();
            int purchase_price = resourceMarket.costToBuy(this.resource, this.amount);
            resourceMarket.buy(this.resource, this.amount);
            pggs.decreasePlayerMoney(playerId, purchase_price);
            System.out.println("Player " + playerId + " bought " + this.amount + " " + this.resource + " for "  + purchase_price);
            pggs.addFuel(playerId, this.resource,this.amount);
            return true;
        } catch (IllegalArgumentException e) {
            System.err.println("BuyResource failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public AbstractAction copy() {
        // Immutable fields, so a shallow copy is fine.
        return new BuyResource(resource, amount);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof BuyResource)) return false;
        BuyResource other = (BuyResource) obj;
        return this.amount == other.amount && this.resource == other.resource;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, amount);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Buys " + amount  + " " + resource;
    }

    @Override
    public String toString() {
        return getString(null);
    }
}

package games.powergrid.actions;

import core.AbstractGameState;

import core.actions.AbstractAction;
import games.powergrid.PowerGridGameState;
import games.powergrid.PowerGridParameters.Resource;
import games.powergrid.components.PowerGridResourceMarket;

import java.util.Objects;

/**
 * Represents an action where the current player purchases one or more fuel resources
 * from the shared {@link PowerGridResourceMarket}.
 * <p>
 * When executed, this action:
 * <ul>
 *   <li>Determines the total purchase cost based on the resource type and quantity.</li>
 *   <li>Removes the specified amount of that resource from the market.</li>
 *   <li>Deducts the purchase cost from the player's money.</li>
 *   <li>Adds the purchased resources to the player's personal fuel storage.</li>
 * </ul>
 *
 * <p><b>Side effects:</b> Modifies both the {@link PowerGridResourceMarket} and the
 * executing player's money and fuel inventory in the {@link PowerGridGameState}.
 * <br>If the market does not have enough available resources or the player cannot afford
 * the cost, an {@link IllegalArgumentException} will be thrown by
 * {@link PowerGridResourceMarket#buy(PowerGridParameters.Resource, int)} or related methods.
 *
 * @see PowerGridResourceMarket
 * @see PowerGridParameters.Resource
 * @see PowerGridGameState
 */

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
    	int playerId = pggs.getCurrentPlayer();
        int purchase_price = resourceMarket.costToBuy(this.resource, this.amount);
        resourceMarket.buy(this.resource, this.amount);
        pggs.decreasePlayerMoney(playerId, purchase_price);
        pggs.addFuel(playerId, this.resource,this.amount);
        return true;

    }

    @Override
    public AbstractAction copy() {
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
        return "Buys " + amount  + " " + resource ;
    }

    @Override
    public String toString() {
        return getString(null);
    }
}

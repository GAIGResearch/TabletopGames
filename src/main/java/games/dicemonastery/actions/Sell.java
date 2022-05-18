package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryConstants.Resource;
import games.dicemonastery.DiceMonasteryGameState;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.SHILLINGS;

public class Sell extends AbstractAction {

    public final Resource resource;
    public final int price;

    public Sell(Resource resource, int price) {
        this.resource = resource;
        this.price = price;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        int player = state.getCurrentPlayer();
        if (state.getResource(player, resource, STOREROOM) >= 1) {
            state.addResource(player, SHILLINGS, price);
            state.addResource(player, resource, -1);
            return true;
        }
        return false;
    }

    @Override
    public Sell copy() {
        return new Sell(resource, price);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sell) {
            Sell other = (Sell) obj;
            return other.price == price && other.resource == resource;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return resource.ordinal() * 7369 + price * 83 + 3479823;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Sell %s for %d shillings", resource, price);
    }
}

package games.dicemonastery.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.dicemonastery.DiceMonasteryConstants.*;
import games.dicemonastery.DiceMonasteryGameState;

import java.util.Objects;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;

public class Buy extends AbstractAction {

    public final Resource resource;
    public final int cost;

    public Buy(Resource resource, int cost) {
        this.resource = resource;
        this.cost = cost;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        DiceMonasteryGameState state = (DiceMonasteryGameState) gs;
        int player = state.getCurrentPlayer();
        if (state.getResource(player, SHILLINGS, STOREROOM) >= cost) {
            state.addResource(player, SHILLINGS, -cost);
            state.addResource(player, resource, 1);
            return true;
        }
        return false;
    }

    @Override
    public Buy copy() {
        return new Buy(resource, cost);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Buy) {
            Buy other = (Buy) obj;
            return other.cost == cost && other.resource == resource;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(resource, cost);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return String.format("Buy %s for %d shillings", resource, cost);
    }
}

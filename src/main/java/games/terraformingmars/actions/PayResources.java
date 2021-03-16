package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.util.Objects;

public class PayResources extends TMAction {
    final TMTypes.Resource res;
    final int amount;
    public PayResources(TMTypes.Resource res, int amount) {
        this.res = res;
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        // Remove resource amount
        TMGameState gs = (TMGameState) gameState;
        boolean success = gs.getPlayerResources()[gs.getCurrentPlayer()].get(res).decrement(amount);
        if (success) {
            if (res == TMTypes.Resource.Card) {
                // TODO: choose card to discard, extended sequence
            }
            return true;
        }
        return false;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PayResources)) return false;
        if (!super.equals(o)) return false;
        PayResources payAction = (PayResources) o;
        return amount == payAction.amount &&
                res == payAction.res;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), res, amount);
    }

    @Override
    public String toString() {
        return "Pay " + amount + " " + res;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay " + amount + " " + res;
    }
}

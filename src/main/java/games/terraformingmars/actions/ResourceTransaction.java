package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.Requirement;

import java.util.Objects;

public class ResourceTransaction extends TMAction {
    public final TMTypes.Resource res;
    public final int amount;
    public ResourceTransaction(TMTypes.Resource res, int amount) {
        super(true);
        this.res = res;
        this.amount = amount;
    }
    public ResourceTransaction(TMTypes.Resource res, int amount, boolean free) {
        super(free);
        this.res = res;
        this.amount = amount;
    }
    public ResourceTransaction(TMTypes.Resource res, int amount, boolean free, Requirement requirement) {
        super(free, requirement);
        this.res = res;
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        // Remove resource amount
        TMGameState gs = (TMGameState) gameState;
        boolean success = gs.getPlayerResources()[gs.getCurrentPlayer()].get(res).increment(amount);  // Amount can be negative
        if (success) {
            if (res == TMTypes.Resource.Card) {
                // TODO: choose card to discard, extended sequence
            }
            return super.execute(gameState);
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
        if (!(o instanceof ResourceTransaction)) return false;
        if (!super.equals(o)) return false;
        ResourceTransaction payAction = (ResourceTransaction) o;
        return amount == payAction.amount &&
                res == payAction.res;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), res, amount);
    }

    @Override
    public String toString() {
        return amount + " " + res;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return amount + " " + res;
    }
}

package games.terraformingmars.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.rules.requirements.Requirement;

import java.util.Objects;

public class ResourceTransaction extends TMAction {
    public final TMTypes.Resource res;
    public final int amount;
    public ResourceTransaction(int player, TMTypes.Resource res, int amount) {
        super(player, true);
        this.res = res;
        this.amount = amount;
    }
    public ResourceTransaction(int player, TMTypes.Resource res, int amount, boolean free) {
        super(player, free);
        this.res = res;
        this.amount = amount;
    }
    public ResourceTransaction(int player, TMTypes.Resource res, int amount, boolean free, Requirement requirement) {
        super(player, free, requirement);
        this.res = res;
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gameState) {
        // Remove resource amount
        TMGameState gs = (TMGameState) gameState;
        int player = this.player;
        if (player == -1) player = gs.getCurrentPlayer();
        boolean success = gs.getPlayerResources()[player].get(res).increment(amount);  // Amount can be negative
        if (amount > 0) {
            gs.getPlayerResourceIncreaseGen()[player].put(res, true);
        }
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

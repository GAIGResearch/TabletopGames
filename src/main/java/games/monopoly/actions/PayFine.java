package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;

/**
 * The current player pays the fine to the Bank, rather than taking a Chance (PayOrChance).
 */
public class PayFine extends AbstractAction {

    public final int amount;

    public PayFine(int amount) {
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        state.pay(state.getCurrentPlayer(), -1, amount);
        return true;
    }

    @Override
    public PayFine copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PayFine p && p.amount == amount;
    }

    @Override
    public int hashCode() {
        return amount + 640229;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay the fine of " + amount;
    }

    @Override
    public String toString() {
        return "PayFine " + amount;
    }
}

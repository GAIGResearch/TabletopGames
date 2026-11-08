package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;

/**
 * The current player pays MonopolyParameters.incomeTaxPercent of their total worth (MonopolyGameState.getNetWorth),
 * rounded down, to the Bank (IncomeTaxChoice).
 */
public class PayPercentTax extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        int player = state.getCurrentPlayer();
        int percent = ((MonopolyParameters) state.getGameParameters()).incomeTaxPercent;
        state.pay(player, -1, state.getNetWorth(player) * percent / 100);
        return true;
    }

    @Override
    public PayPercentTax copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PayPercentTax;
    }

    @Override
    public int hashCode() {
        return 640257;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay a percentage of total worth";
    }

    @Override
    public String toString() {
        return "PayPercentTax";
    }
}

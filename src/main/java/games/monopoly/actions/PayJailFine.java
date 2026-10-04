package games.monopoly.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.monopoly.MonopolyGameState;
import games.monopoly.MonopolyParameters;

/**
 * The current player pays MonopolyParameters.jailFine to the Bank and leaves Jail. They then roll as normal.
 */
public class PayJailFine extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        MonopolyGameState state = (MonopolyGameState) gs;
        int player = state.getCurrentPlayer();
        state.pay(player, -1, ((MonopolyParameters) state.getGameParameters()).jailFine);
        state.leaveJail(player);
        return true;
    }

    @Override
    public PayJailFine copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PayJailFine;
    }

    @Override
    public int hashCode() {
        return 640217;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Pay the fine to leave Jail";
    }

    @Override
    public String toString() {
        return "PayJailFine";
    }
}

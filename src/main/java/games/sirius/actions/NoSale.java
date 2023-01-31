package games.sirius.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.sirius.SiriusGameState;

public class NoSale extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        SiriusGameState state = (SiriusGameState) gs;
        state.setActionTaken("Betrayed", state.getCurrentPlayer());
        state.setActionTaken("Sold", state.getCurrentPlayer());
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof NoSale;
    }

    @Override
    public int hashCode() {
        return -25304;
    }

    @Override
    public String toString() {
        return "No Sale this round";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

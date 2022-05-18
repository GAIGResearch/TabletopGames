package games.catan.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.catan.CatanGameState;

public class EndNegotiation extends AbstractAction {
    // This is used as an indication that we are stopping this round of negotiation

    @Override
    public String toString() {
        return "Ends Negotiation";
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof EndNegotiation;
    }

    @Override
    public int hashCode() {
        return -322042;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

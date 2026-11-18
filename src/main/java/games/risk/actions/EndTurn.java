package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

/**
 * The current player ends their turn without fortifying.
 */
public class EndTurn extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        // the forward model passes the turn
        return true;
    }

    @Override
    public EndTurn copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof EndTurn;
    }

    @Override
    public int hashCode() {
        return 731217;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "EndTurn";
    }
}

package games.leducpoker.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;

public class Fold extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        // The forward model settles the hand in favour of the other player
        return true;
    }

    @Override
    public Fold copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fold;
    }

    @Override
    public int hashCode() {
        return 604211;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return "Fold";
    }

    @Override
    public String toString() {
        return "Fold";
    }
}

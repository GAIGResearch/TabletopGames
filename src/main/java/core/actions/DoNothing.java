package core.actions;

import core.AbstractGameState;

/**
 * This action executes successfully automatically, it makes not changes to the game state.
 */
public class DoNothing extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
       return true;
   }

    @Override
    public AbstractAction copy() {
        return new DoNothing();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof DoNothing;
    }

    @Override
    public int hashCode() {
        return 0;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "DoNothing";
    }
}

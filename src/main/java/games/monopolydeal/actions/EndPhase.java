package games.monopolydeal.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;


/**
 * This is simply a new name for DoNothing when used to prematurely end a game phase
 */
public class EndPhase extends AbstractAction {

    @Override
    public boolean execute(AbstractGameState gs) {
        return true;
    }

    @Override
    public EndPhase copy() {
        return new EndPhase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof EndPhase;
    }

    @Override
    public int hashCode() {
        return 1728310;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "End Current Phase";
    }
}

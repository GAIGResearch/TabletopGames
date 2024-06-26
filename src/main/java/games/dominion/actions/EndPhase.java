package games.dominion.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dominion.DominionConstants;
import games.dominion.DominionGameState;


/**
 * This is simply a new name for DoNothing when used to prematurely end a game phase
 */
public class EndPhase extends AbstractAction {

    public final DominionGameState.DominionGamePhase phase;

    public EndPhase(DominionGameState.DominionGamePhase phase) {
        this.phase = phase;
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
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof EndPhase && phase == ((EndPhase) o).phase;
    }

    @Override
    public int hashCode() {
        return 1 + phase.ordinal() * 310;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "End Phase: " + phase;
    }
}

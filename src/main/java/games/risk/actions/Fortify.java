package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.Objects;

/**
 * Starts the current player's fortifying move from one territory to another. The number of armies is a follow-on
 * choice.
 */
public class Fortify extends AbstractAction {

    public final RiskTerritory from;
    public final RiskTerritory to;

    public Fortify(RiskTerritory from, RiskTerritory to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        state.setActionInProgress(new MoveArmiesChoice(state.getCurrentPlayer(), from, to, 1));
        return true;
    }

    @Override
    public Fortify copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Fortify other && other.from.equals(from) && other.to.equals(to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to) + 731211;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Fortify(" + from + ", " + to + ")";
    }
}

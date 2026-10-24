package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.Objects;

/**
 * Moves n armies from one of the current player's territories to another.
 */
public class MoveArmies extends AbstractAction {

    public final RiskTerritory from;
    public final RiskTerritory to;
    public final int n;

    public MoveArmies(RiskTerritory from, RiskTerritory to, int n) {
        this.from = from;
        this.to = to;
        this.n = n;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        state.addArmies(from, -n);
        state.addArmies(to, n);
        return true;
    }

    @Override
    public MoveArmies copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MoveArmies other && other.from.equals(from) && other.to.equals(to) && other.n == n;
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to, n) + 731209;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "MoveArmies(" + from + ", " + to + ", " + n + ")";
    }
}

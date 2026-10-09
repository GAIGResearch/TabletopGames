package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.Objects;

/**
 * Starts the current player's attack from one territory on another. The number of dice (or a Blitz) is a follow-on
 * choice.
 */
public class ChooseAttack extends AbstractAction {

    public final RiskTerritory from;
    public final RiskTerritory to;

    public ChooseAttack(RiskTerritory from, RiskTerritory to) {
        this.from = from;
        this.to = to;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        state.setActionInProgress(new AttackDiceChoice(state.getCurrentPlayer(), from, to));
        return true;
    }

    @Override
    public ChooseAttack copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ChooseAttack other && other.from.equals(from) && other.to.equals(to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to) + 731229;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "ChooseAttack(" + from + ", " + to + ")";
    }
}

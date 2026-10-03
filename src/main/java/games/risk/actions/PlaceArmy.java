package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskTerritory;

import java.util.Objects;

/**
 * Puts one of the current player's armies to place on a territory they hold.
 */
public class PlaceArmy extends AbstractAction {

    public final RiskTerritory territory;

    public PlaceArmy(RiskTerritory territory) {
        this.territory = territory;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        int player = state.getCurrentPlayer();
        state.addArmies(territory, 1);
        state.setArmiesToPlace(player, state.getArmiesToPlace(player) - 1);
        return true;
    }

    @Override
    public PlaceArmy copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaceArmy other && other.territory.equals(territory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(territory) + 731203;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "PlaceArmy(" + territory + ")";
    }
}

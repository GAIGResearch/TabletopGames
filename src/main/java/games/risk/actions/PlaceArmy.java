package games.risk.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.risk.RiskGameState;
import games.risk.RiskParameters;
import games.risk.RiskTerritory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Puts n of the current player's armies to place on a territory they hold.
 */
public class PlaceArmy extends AbstractAction {

    public final RiskTerritory territory;
    public final int n;

    public PlaceArmy(RiskTerritory territory) {
        this(territory, 1);
    }

    public PlaceArmy(RiskTerritory territory, int n) {
        this.territory = territory;
        this.n = n;
    }

    /**
     * The placements open to the player: placementBatch armies at a time while they have more than that left to
     * place, then one at a time, and no more than the territory has room for.
     */
    public static List<AbstractAction> options(RiskGameState state, int player) {
        RiskParameters params = (RiskParameters) state.getGameParameters();
        int batch = state.getArmiesToPlace(player) > params.placementBatch ? params.placementBatch : 1;
        List<AbstractAction> actions = new ArrayList<>();
        for (RiskTerritory t : state.getPlaceableTerritories(player))
            actions.add(new PlaceArmy(t, Math.min(batch, state.getRoom(t))));
        return actions;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        RiskGameState state = (RiskGameState) gs;
        int player = state.getCurrentPlayer();
        state.addArmies(territory, n);
        state.setArmiesToPlace(player, state.getArmiesToPlace(player) - n);
        return true;
    }

    @Override
    public PlaceArmy copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PlaceArmy other && other.territory.equals(territory) && other.n == n;
    }

    @Override
    public int hashCode() {
        return Objects.hash(territory, n) + 731203;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return n == 1 ? "PlaceArmy(" + territory + ")" : "PlaceArmy(" + territory + ", " + n + ")";
    }
}

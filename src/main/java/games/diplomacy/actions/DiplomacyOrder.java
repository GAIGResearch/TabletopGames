package games.diplomacy.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyProvince;

/**
 * An order given by the current power: to one of its units (hold, move, support, convoy, retreat, disband) or, in
 * the adjustment phase, a build. Executing it only records it in the state; the forward model resolves the orders
 * of all the powers together once every one has been given.
 */
public abstract class DiplomacyOrder extends AbstractAction {

    /**
     * The province of the unit ordered (or for a build, where the unit is built).
     */
    public abstract DiplomacyProvince province();

    @Override
    public boolean execute(AbstractGameState gs) {
        DiplomacyGameState state = (DiplomacyGameState) gs;
        state.addOrder(state.getCurrentPlayer(), this);
        return true;
    }

    @Override
    public DiplomacyOrder copy() {
        return this;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

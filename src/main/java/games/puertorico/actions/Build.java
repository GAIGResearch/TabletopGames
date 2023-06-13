package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.components.Building;

public class Build extends AbstractAction {

    public final PuertoRicoConstants.BuildingType type;
    public final int cost;

    public Build(PuertoRicoConstants.BuildingType type, int cost) {
        this.type = type;
        this.cost = cost;
    }

    public Build(PuertoRicoConstants.BuildingType type) {
        this(type, type.cost);
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        if (state.getDoubloons(state.getCurrentPlayer()) < cost)
            throw new AssertionError("Player does not have enough doubloons to build " + type);
        Building building = state.build(state.getCurrentPlayer(), type);
        state.changeDoubloons(state.getCurrentPlayer(), -cost);
        if (state.hasActiveBuilding(state.getCurrentPlayer(), PuertoRicoConstants.BuildingType.UNIVERSITY)) {
            // we also add a free colonist from the supply if there is one
            if (state.getColonistsInSupply() > 0) {
                state.changeColonistsInSupply(-1);
                building.setOccupation(1);
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Build) {
            Build other = (Build) obj;
            return type == other.type && cost == other.cost;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return type.ordinal() + cost * 31 - 30923;
    }

    @Override
    public String toString() {
        return "Build " + type + " at cost of " + cost;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

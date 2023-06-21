package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.components.Plantation;

public class BuildQuarry extends AbstractAction {
    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        if (state.getQuarriesLeft() <= 0)
            throw new AssertionError("No quarries left to build");
        Plantation quarry = state.removeQuarry();
        state.addPlantation(state.getCurrentPlayer(), quarry);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this; // immutable
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BuildQuarry;
    }

    @Override
    public int hashCode() {
        return 202384;
    }

    @Override
    public String toString() {
        return "Build Quarry";
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

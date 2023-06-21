package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants.*;
import games.puertorico.PuertoRicoGameState;
import games.puertorico.components.Plantation;

public class DrawPlantation extends AbstractAction {

    public final Crop crop;

    public DrawPlantation() {
        this.crop = null;
    }

    public DrawPlantation(Crop crop) {
        this.crop = crop;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // null indicates we draw from stack; otherwise from the face-up plantations
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        Plantation tile = crop == null ? state.drawPlantationFromStack() : state.drawPlantation(crop);
        state.addPlantation(state.getCurrentPlayer(), tile);

        // then we see if the player has an occupied Hospice
        if (crop != null && state.hasActiveBuilding(state.getCurrentPlayer(), BuildingType.HOSPICE)) {
            // if so, we add a colonist to the plantation from supply
            if (state.getColonistsInSupply() > 0) {
                tile.setOccupied();
                state.changeColonistsInSupply(-1);
            }
        }
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DrawPlantation && ((DrawPlantation) obj).crop == this.crop;
    }

    @Override
    public int hashCode() {
        return 67 * (this.crop == null ? 0 : this.crop.ordinal());
    }

    @Override
    public String toString() {
        return "DrawPlantation: " + (this.crop == null ? "Stack" : this.crop.toString());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

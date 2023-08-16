package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

public class WarehouseStorage extends AbstractAction {

    public final PuertoRicoConstants.Crop storedCrop;

    public WarehouseStorage(PuertoRicoConstants.Crop storedCrop) {
        this.storedCrop = storedCrop;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        state.getPlayerBoard(state.getCurrentPlayer()).store(storedCrop);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof WarehouseStorage) {
            WarehouseStorage other = (WarehouseStorage) obj;
            return this.storedCrop == other.storedCrop;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 3942 - storedCrop.ordinal();
    }


    @Override
    public String toString() {
        return String.format("Store %s in warehouse", storedCrop.name());
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

public class GainCrop extends AbstractAction {
    public final PuertoRicoConstants.Crop crop;
    public final int amount;

    public GainCrop(PuertoRicoConstants.Crop crop, int amount) {
        this.crop = crop;
        this.amount = amount;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        if (amount > state.getSupplyOf(crop))
            throw new IllegalArgumentException("Not enough " + crop + " in supply");
        state.getPlayerBoard(state.getCurrentPlayer()).harvest(crop, amount);
        state.changeSupplyOf(crop, -amount);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GainCrop && ((GainCrop) obj).crop == this.crop && ((GainCrop) obj).amount == this.amount;
    }

    @Override
    public int hashCode() {
        return 67 * this.crop.ordinal() + 31 * this.amount;
    }

    @Override
    public String toString() {
        return "GainCrop: " + this.crop + " x" + this.amount;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

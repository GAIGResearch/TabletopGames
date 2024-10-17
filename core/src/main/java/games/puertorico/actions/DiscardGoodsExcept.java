package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

import java.util.Set;

public class DiscardGoodsExcept extends AbstractAction {

    public final PuertoRicoConstants.Crop crop;

    public DiscardGoodsExcept(PuertoRicoConstants.Crop crop) {
        this.crop = crop;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        Set<PuertoRicoConstants.Crop> inWarehouse = state.getPlayerBoard(state.getCurrentPlayer()).getCropsInWarehouses();
        for (PuertoRicoConstants.Crop c : PuertoRicoConstants.Crop.values()) {
            if (inWarehouse.contains(c)) continue;
            int amount = state.getStoresOf(state.getCurrentPlayer(), c);
            if (c == crop) amount--;
            if (amount > 0) {
                state.getPlayerBoard(state.getCurrentPlayer()).harvest(c, -amount);
                state.changeSupplyOf(c, amount);
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
        if (obj instanceof DiscardGoodsExcept) {
            DiscardGoodsExcept other = (DiscardGoodsExcept) obj;
            return crop == other.crop;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return crop.ordinal() + 30294;
    }

    @Override
    public String toString() {
        return "Discard everything except 1 " + crop;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

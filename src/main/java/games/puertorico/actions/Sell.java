package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.PuertoRicoConstants;
import games.puertorico.PuertoRicoGameState;

public class Sell extends AbstractAction {

    public final PuertoRicoConstants.Crop goodSold;
    public final int salesPrice;

    public Sell(PuertoRicoConstants.Crop goodSold, int price) {
        this.goodSold = goodSold;
        this.salesPrice = price;
    }
    @Override
    public boolean execute(AbstractGameState gs) {
        if (goodSold == null) {
            return true;
        }
        PuertoRicoGameState state = (PuertoRicoGameState) gs;
        state.sellGood(state.getCurrentPlayer(), goodSold);
        state.changeDoubloons(state.getCurrentPlayer(), salesPrice);
        return true;
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Sell) {
            Sell other = (Sell) obj;
            return other.goodSold == goodSold && other.salesPrice == salesPrice;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (goodSold == null ? -1 : goodSold.ordinal()) + 31 * salesPrice + 3801203;
    }


    @Override
    public String toString() {
        if (goodSold == null)
            return "Sell Nothing";
        return "Sell " + goodSold + " for " + salesPrice + " doubloons";
    }
    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

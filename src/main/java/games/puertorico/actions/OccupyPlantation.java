package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.*;
import games.puertorico.components.Plantation;

public class OccupyPlantation extends AbstractAction {

    public final PuertoRicoConstants.Crop crop;

    public OccupyPlantation(PuertoRicoConstants.Crop crop) {
        this.crop = crop;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState prgs = (PuertoRicoGameState) gs;
        PRPlayerBoard pb = prgs.getPlayerBoard(prgs.getCurrentPlayer());
        // it is more efficient to mark what we leave fallow; but it is easier to allocate colonists
        // as that is marked on the player board
        for (int i = 0; i < pb.getPlantations().size(); i++) {
            Plantation p = pb.getPlantations().get(i);
            if (p.crop == this.crop && !p.isOccupied()) {
                p.setOccupied();
                pb.addColonists(-1);
                return true;
            }
        }
        throw new AssertionError("No unoccupied plantations of type " + this.crop + " found");
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OccupyPlantation && ((OccupyPlantation) obj).crop == this.crop;
    }

    @Override
    public int hashCode() {
        return 67 * this.crop.ordinal();
    }

    @Override
    public String toString() {
        return "Occupy Plantation: " + this.crop;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

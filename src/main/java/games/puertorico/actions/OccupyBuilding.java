package games.puertorico.actions;

import core.AbstractGameState;
import core.actions.AbstractAction;
import games.puertorico.*;
import games.puertorico.components.Building;
import games.puertorico.components.Plantation;

public class OccupyBuilding extends AbstractAction {

    public final PuertoRicoConstants.BuildingType building;

    public OccupyBuilding(PuertoRicoConstants.BuildingType building) {
        this.building = building;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        PuertoRicoGameState prgs = (PuertoRicoGameState) gs;
        PRPlayerBoard pb = prgs.getPlayerBoard(prgs.getCurrentPlayer());
        for (Building b : pb.getBuildings()) {
            if (b.buildingType == this.building && b.getOccupation() < b.buildingType.capacity) {
                b.setOccupation(b.getOccupation() + 1);
                pb.addColonists(-1);
                return true;
            }
        }
        throw new AssertionError("No unoccupied buildings of type " + this.building + " found");
    }

    @Override
    public AbstractAction copy() {
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof OccupyBuilding && ((OccupyBuilding) obj).building == this.building;
    }

    @Override
    public int hashCode() {
        return 27 * this.building.ordinal();
    }

    @Override
    public String toString() {
        return "Occupy Building: " + this.building;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }
}

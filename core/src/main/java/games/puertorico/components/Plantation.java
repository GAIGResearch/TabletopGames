package games.puertorico.components;

import core.CoreConstants;
import core.components.Component;
import games.puertorico.PuertoRicoConstants;

public class Plantation extends Component {

    public final PuertoRicoConstants.Crop crop;
    boolean occupied;

    public Plantation(PuertoRicoConstants.Crop crop) {
        super(CoreConstants.ComponentType.TOKEN, crop + " Plantation");
        this.crop = crop;
        this.occupied = false;
    }
    private Plantation(Plantation copyFrom) {
        super(CoreConstants.ComponentType.TOKEN, copyFrom.componentName, copyFrom.componentID);
        this.crop = copyFrom.crop;
        this.occupied = copyFrom.occupied;
    }

    public void setOccupied() {
        this.occupied = true;
    }
    public void unsetOccupied() {
        this.occupied = false;
    }

    public boolean isOccupied() {
        return this.occupied;
    }
    public Plantation copy() {
        return new Plantation(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Plantation) {
            Plantation p = (Plantation) obj;
            return this.crop == p.crop && this.occupied == p.occupied && super.equals(obj);
        }
        return false;
    }

    public boolean isEquivalent(Plantation p) {
        return this.crop == p.crop && this.occupied == p.occupied;
    }

    @Override
    public int hashCode() {
        return this.crop.ordinal() + 7 * (this.occupied ? 1 : 0) - 39539 + 31* super.hashCode();
    }
}


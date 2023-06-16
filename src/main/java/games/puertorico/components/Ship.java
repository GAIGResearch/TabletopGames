package games.puertorico.components;

import core.CoreConstants;
import core.components.Component;
import games.puertorico.PuertoRicoConstants;

import java.util.Optional;

public class Ship extends Component {

    public final int capacity;
    int spacesFilled;
    Optional<PuertoRicoConstants.Crop> cargo;

    public Ship(int capacity) {
        super(CoreConstants.ComponentType.TOKEN, "Ship with capacity " + capacity);
        this.capacity = capacity;
        this.spacesFilled = 0;
        this.cargo = Optional.empty();
    }
    // private constructor for copy
    private Ship(int capacity, int componentID) {
        super(CoreConstants.ComponentType.TOKEN, "Ship with capacity " + capacity, componentID);
        this.capacity = capacity;
    }

    public void load(PuertoRicoConstants.Crop cargo, int amount) {
        this.cargo = Optional.of(cargo);
        this.spacesFilled += amount;
        if (this.spacesFilled > this.capacity) {
            throw new IllegalArgumentException("Ship is over capacity");
        }
    }

    public void unload() {
        this.cargo = Optional.empty();
        this.spacesFilled = 0;
    }

    public int getAvailableCapacity() {
        return this.capacity - this.spacesFilled;
    }

    public int getSpacesFilled() {
        return spacesFilled;
    }

    public PuertoRicoConstants.Crop getCurrentCargo() {
        return cargo.orElse(null);
    }
    public Ship copy() {
        Ship copy = new Ship(this.capacity, componentID);
        copy.spacesFilled = this.spacesFilled;
        copy.cargo = this.cargo;
        return copy;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Ship) {
            Ship s = (Ship) obj;
            return this.capacity == s.capacity && this.spacesFilled == s.spacesFilled && this.cargo.equals(s.cargo);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.capacity + 7 * this.spacesFilled + 31 * this.cargo.map(Enum::ordinal).orElse(0) - 3939;
    }

    @Override
    public String toString() {
        return (getCurrentCargo() != null? getCurrentCargo().name() + ": " + spacesFilled + "/" + capacity : "Empty");
    }
}

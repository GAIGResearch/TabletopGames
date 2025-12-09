package games.powergrid.components;

import java.util.Objects;

// PowerGridCity.java

import core.CoreConstants;
import core.components.Component;


/**
 * Immutable representation of a city on the Power Grid map.
 * <p>
 * Each city has a name, belongs to a numbered region, and may be a
 * <em>double city</em> (a metropolis in the Deluxe rules). A double city
 * provides two additional generator slots per step (i.e., two in Step 1, two in Step 2,
 * and two in Step 3) this is currently not implemented in the current version of the game.
 *
 * <p><b>Immutability:</b> All fields are {@code final}; {@link #copy()} returns
 * {@code this}.
 */
public final class PowerGridCity extends Component {

    private final String name;
    private final int region;
    private final boolean doubleCity;  // TODO true = metropolis (double city) currently does nothing 

    public PowerGridCity(int id, String name, int region, boolean doubleCity) {
        super(CoreConstants.ComponentType.TOKEN, name, id);
        this.name = name;
        this.region = region;
        this.doubleCity = doubleCity;
    }

    public String name()      { return name; }
    public int getRegion()       { return region; }
    public boolean isDouble() { return doubleCity; }

    @Override
    public PowerGridCity copy() { return this; }

    @Override
    public String toString() {
        return String.format("%s (id=%d, region=%d%s)",
                name, getComponentID(), region,
                doubleCity ? ", metropolis" : "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PowerGridCity c)) return false;
        return region == c.region
                && doubleCity == c.doubleCity
                && Objects.equals(name, c.name); 
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, region, doubleCity);
    }
}
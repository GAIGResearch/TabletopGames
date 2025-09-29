package games.powergrid.components;

import java.util.Objects;

// PowerGridCity.java

import core.CoreConstants;
import core.components.Component;


/*
 * Immuatable POwerGridCity class this is responsible for tracking the name, region, coordiantes for UI, and double city. In 
 * the version of powergrid this is based on powergrid delux several cities have double slots so 2 phase1, 2 phase2, and 2 phase 3. If 
 * the double_city value is true then it means this is a city that can hold two seperate players power plants at a given phase
 */
public final class PowerGridCity extends Component {
    private final String name;
    private final int region;
    private final boolean doubleCity; // true = 2 slots per step (Deluxe metropolises)

    public PowerGridCity(int id, String name, int region, boolean doubleCity) {
        super(CoreConstants.ComponentType.TOKEN, name, id);
        this.name = name;
        this.region = region;
        this.doubleCity = doubleCity;
    }


	public String name()      { return name; }
    public int region()       { return region; }

    public boolean isDouble() { return doubleCity; }

    @Override public PowerGridCity copy() { return this; } // immutable
    public int getRegion() {
    	return this.region();
    }
    @Override
    public String toString() {
        return String.format("%s (id=%d, region=%d%s)", 
                             name, getComponentID(), region, 
                             doubleCity ? ", metropolis" : "");
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PowerGridCity c)) return false;
        return this.getComponentID() == c.getComponentID()
            && this.region == c.region
            && this.name.equals(c.name)
            && this.doubleCity == c.doubleCity;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getComponentID(), name, region, doubleCity);
    }

}
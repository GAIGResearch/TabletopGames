package games.diplomacy.actions;

import games.diplomacy.DiplomacyLocation;
import games.diplomacy.DiplomacyProvince;
import games.diplomacy.DiplomacyUnit;

import java.util.Objects;

/**
 * In the adjustment phase, a new unit of the given type is built at the location (a home supply centre); a fleet
 * built in a province with separate coasts is built on the coast named.
 */
public class Build extends DiplomacyOrder {

    public final DiplomacyLocation location;
    public final DiplomacyUnit.Type type;

    public Build(DiplomacyLocation location, DiplomacyUnit.Type type) {
        this.location = location;
        this.type = type;
    }

    @Override
    public DiplomacyProvince province() {
        return location.province();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Build other && location.equals(other.location) && type == other.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(location, type.ordinal()) + 640129;
    }

    @Override
    public String toString() {
        return "Build " + type.letter + " " + location;
    }
}

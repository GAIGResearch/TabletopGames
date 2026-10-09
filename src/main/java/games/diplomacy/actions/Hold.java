package games.diplomacy.actions;

import core.AbstractGameState;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyProvince;
import games.diplomacy.DiplomacyUnit;

import java.util.Objects;

/**
 * The unit in the province stays where it is.
 */
public class Hold extends DiplomacyOrder {

    public final DiplomacyProvince unit;

    public Hold(DiplomacyProvince unit) {
        this.unit = unit;
    }

    @Override
    public DiplomacyProvince province() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Hold other && unit.equals(other.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit) + 640117;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DiplomacyUnit u = ((DiplomacyGameState) gameState).getUnit(unit);
        return (u == null ? "" : u.type().letter + " ") + this;
    }

    @Override
    public String toString() {
        return unit.name() + " Holds";
    }
}

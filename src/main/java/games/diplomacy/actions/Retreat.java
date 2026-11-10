package games.diplomacy.actions;

import core.AbstractGameState;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyLocation;
import games.diplomacy.DiplomacyProvince;
import games.diplomacy.DiplomacyUnit;

import java.util.Objects;

/**
 * In a retreat phase, the unit dislodged from the province retreats to the location.
 */
public class Retreat extends DiplomacyOrder {

    public final DiplomacyProvince unit;
    public final DiplomacyLocation to;

    public Retreat(DiplomacyProvince unit, DiplomacyLocation to) {
        this.unit = unit;
        this.to = to;
    }

    @Override
    public DiplomacyProvince province() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Retreat other && unit.equals(other.unit) && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, to) + 640151;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DiplomacyUnit u = ((DiplomacyGameState) gameState).getDislodged(unit);
        return (u == null ? "" : u.type().letter + " ") + this;
    }

    @Override
    public String toString() {
        return unit.name() + " retreats to " + to;
    }
}

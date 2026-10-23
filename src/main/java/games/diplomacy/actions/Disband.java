package games.diplomacy.actions;

import core.AbstractGameState;
import games.diplomacy.DiplomacyProvince;

import java.util.Objects;

/**
 * The unit in the province is removed from the board: in the adjustment phase, by a power with more units than
 * supply centres.
 */
public class Disband extends DiplomacyOrder {

    public final DiplomacyProvince unit;

    public Disband(DiplomacyProvince unit) {
        this.unit = unit;
    }

    @Override
    public DiplomacyProvince province() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Disband other && unit.equals(other.unit);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit) + 640127;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return "Disband " + unit.name();
    }
}

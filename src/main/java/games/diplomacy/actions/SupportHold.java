package games.diplomacy.actions;

import core.AbstractGameState;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyProvince;
import games.diplomacy.DiplomacyUnit;

import java.util.Objects;

/**
 * The unit in the province supports the unit in the supported province in staying where it is (it helps only if
 * that unit does not move).
 */
public class SupportHold extends DiplomacyOrder {

    public final DiplomacyProvince unit;
    public final DiplomacyProvince supported;

    public SupportHold(DiplomacyProvince unit, DiplomacyProvince supported) {
        this.unit = unit;
        this.supported = supported;
    }

    @Override
    public DiplomacyProvince province() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SupportHold other && unit.equals(other.unit) && supported.equals(other.supported);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, supported) + 640139;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DiplomacyGameState state = (DiplomacyGameState) gameState;
        DiplomacyUnit u = state.getUnit(unit);
        DiplomacyUnit s = state.getUnit(supported);
        return (u == null ? "" : u.type().letter + " ") + unit.name() + " S "
                + (s == null ? "" : s.type().letter + " ") + supported.name();
    }

    @Override
    public String toString() {
        return unit.name() + " S " + supported.name();
    }
}

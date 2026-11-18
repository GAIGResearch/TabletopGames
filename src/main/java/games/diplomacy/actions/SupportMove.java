package games.diplomacy.actions;

import core.AbstractGameState;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyProvince;
import games.diplomacy.DiplomacyUnit;

import java.util.Objects;

/**
 * The unit in the province supports the unit in from in moving to the province to (whatever the coast). It helps
 * only if that unit is ordered to make exactly that move.
 */
public class SupportMove extends DiplomacyOrder {

    public final DiplomacyProvince unit;
    public final DiplomacyProvince from;
    public final DiplomacyProvince to;

    public SupportMove(DiplomacyProvince unit, DiplomacyProvince from, DiplomacyProvince to) {
        this.unit = unit;
        this.from = from;
        this.to = to;
    }

    @Override
    public DiplomacyProvince province() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SupportMove other && unit.equals(other.unit) && from.equals(other.from)
                && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, from, to) + 640141;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DiplomacyGameState state = (DiplomacyGameState) gameState;
        DiplomacyUnit u = state.getUnit(unit);
        DiplomacyUnit s = state.getUnit(from);
        return (u == null ? "" : u.type().letter + " ") + unit.name() + " S "
                + (s == null ? "" : s.type().letter + " ") + from.name() + "-" + to.name();
    }

    @Override
    public String toString() {
        return unit.name() + " S " + from.name() + "-" + to.name();
    }
}

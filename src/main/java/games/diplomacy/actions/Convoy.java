package games.diplomacy.actions;

import core.AbstractGameState;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyProvince;

import java.util.Objects;

/**
 * The fleet in the (sea) province convoys the army in from to the province to. It helps only if that army is
 * ordered to make exactly that move.
 */
public class Convoy extends DiplomacyOrder {

    public final DiplomacyProvince unit;
    public final DiplomacyProvince from;
    public final DiplomacyProvince to;

    public Convoy(DiplomacyProvince unit, DiplomacyProvince from, DiplomacyProvince to) {
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
        return o instanceof Convoy other && unit.equals(other.unit) && from.equals(other.from) && to.equals(other.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, from, to) + 640153;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return (((DiplomacyGameState) gameState).getUnit(unit) == null ? "" : "F ") + this;
    }

    @Override
    public String toString() {
        return unit.name() + " C " + from.name() + "-" + to.name();
    }
}

package games.diplomacy.actions;

import core.AbstractGameState;
import games.diplomacy.DiplomacyGameState;
import games.diplomacy.DiplomacyLocation;
import games.diplomacy.DiplomacyProvince;
import games.diplomacy.DiplomacyUnit;

import java.util.Objects;

/**
 * The unit in the province moves to (attacks) the location; for a fleet entering a province with separate coasts,
 * the location names the coast. An army's move marked viaConvoy is to be carried by convoying fleets, even where
 * it could go by land.
 */
public class Move extends DiplomacyOrder {

    public final DiplomacyProvince unit;
    public final DiplomacyLocation to;
    public final boolean viaConvoy;

    public Move(DiplomacyProvince unit, DiplomacyLocation to, boolean viaConvoy) {
        this.unit = unit;
        this.to = to;
        this.viaConvoy = viaConvoy;
    }

    /**
     * A move not marked "via convoy".
     */
    public Move(DiplomacyProvince unit, DiplomacyLocation to) {
        this(unit, to, false);
    }

    @Override
    public DiplomacyProvince province() {
        return unit;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Move other && unit.equals(other.unit) && to.equals(other.to) && viaConvoy == other.viaConvoy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(unit, to, viaConvoy) + 640121;
    }

    @Override
    public String getString(AbstractGameState gameState) {
        DiplomacyUnit u = ((DiplomacyGameState) gameState).getUnit(unit);
        return (u == null ? "" : u.type().letter + " ") + this;
    }

    @Override
    public String toString() {
        return unit.name() + "-" + to + (viaConvoy ? " via convoy" : "");
    }
}

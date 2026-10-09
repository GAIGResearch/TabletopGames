package games.diplomacy.actions;

import games.diplomacy.DiplomacyProvince;

/**
 * In the adjustment phase, the power gives up the rest of the builds it is entitled to.
 */
public class WaiveBuilds extends DiplomacyOrder {

    /**
     * None: a waiver concerns no province.
     */
    @Override
    public DiplomacyProvince province() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof WaiveBuilds;
    }

    @Override
    public int hashCode() {
        return 640133;
    }

    @Override
    public String toString() {
        return "Waive builds";
    }
}

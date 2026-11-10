package games.diplomacy;

import core.interfaces.IGamePhase;

/**
 * The phases of a game year.
 */
public enum DiplomacyPhase implements IGamePhase {
    /** each unit is ordered to hold, move, support or convoy */
    SPRING_ORDERS(true),
    /** each dislodged unit retreats or disbands */
    SPRING_RETREATS(true),
    FALL_ORDERS(false),
    FALL_RETREATS(false),
    /** after the Fall turn: supply centres change hands, and powers build or disband to match them */
    ADJUSTMENTS(false);

    public final boolean spring;

    DiplomacyPhase(boolean spring) {
        this.spring = spring;
    }

    public boolean isOrders() {
        return this == SPRING_ORDERS || this == FALL_ORDERS;
    }

    public boolean isRetreats() {
        return this == SPRING_RETREATS || this == FALL_RETREATS;
    }
}

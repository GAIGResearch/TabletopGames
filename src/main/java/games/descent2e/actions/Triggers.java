package games.descent2e.actions;

/**
 * These represent points at which a player's action may be interrupted by another player,
 * or possibly by the same player using an ability or item.
 *
 * Each item or ability or overlord card should indicate the point at which it can be used using
 * this enum.
 */
public enum Triggers {
    ACTION_POINT_SPEND,
    HEROIC_FEAT,
    MOVE_INTO_SPACE,
    FATIGUE_INTO_SPACE,
    START_ATTACK,
    END_ATTACK,
    SURGE_DECISION,
    ROLL_OWN_DICE,
    ROLL_OTHER_DICE,
    END_TURN,
    FORCED,
    ANYTIME,
    ROLL_DEFENCE_DICE,
    TAKE_DAMAGE
}

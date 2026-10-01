package games.toads.abilities;

/**
 * The priorities of the four Rulebook 3 Tactic stages. CardModifiers are applied at priority 0, after the
 * Block, Start and During stages and before After.
 */
public class AbilityConstants {

    public static final int BLOCK = -30;
    public static final int START = -20;
    public static final int DURING = -10;
    public static final int AFTER = 20;
}

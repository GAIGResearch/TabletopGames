package games.ventlife.components;

/**
 * The creature tokens on one hex. Only Tube Worms ever stack (the Low-Vent Bonus); otherwise count is 1.
 */
public record Creature(Species species, int owner, int count) {
}

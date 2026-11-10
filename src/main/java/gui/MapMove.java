package gui;

/**
 * The map regions an action involves, so that a player can choose it by pointing at one region and then the other:
 * the region of the piece that acts, and the region it acts on (the same region for an action on the spot, such as a
 * hold). See {@link AbstractGUIManager#getMapMove}.
 *
 * @param from the id of the region of the piece that acts
 * @param to   the id of the region it acts on
 */
public record MapMove(String from, String to) {
}

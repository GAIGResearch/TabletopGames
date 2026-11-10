package games.diplomacy;

import games.diplomacy.actions.DiplomacyOrder;

/**
 * An order of a resolved phase, with the power that gave it and whether it succeeded (a move that was carried out,
 * a support that was not cut, a convoying fleet not dislodged, a build that was made).
 */
public record DiplomacyResult(int power, DiplomacyOrder order, boolean success) {
}

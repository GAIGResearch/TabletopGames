package games.monopoly;

import core.interfaces.IGamePhase;

/**
 * The steps of a Monopoly turn. A turn starts with ROLL; after each roll has been resolved comes MANAGE, which ends
 * with another roll (after doubles) or the end of the turn.
 */
public enum MonopolyGamePhase implements IGamePhase {
    /** the current player rolls; a player in Jail may instead pay the fine or use a Get Out of Jail Free card first */
    ROLL,
    /** the current player builds, sells buildings, mortgages and unmortgages, then rolls again or ends the turn */
    MANAGE
}

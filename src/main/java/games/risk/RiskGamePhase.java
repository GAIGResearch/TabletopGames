package games.risk;

import core.interfaces.IGamePhase;

/**
 * The steps of a game of Risk. CLAIM and PLACE_INITIAL are the initial army placement; each turn then runs
 * REINFORCE, ATTACK and FORTIFY.
 */
public enum RiskGamePhase implements IGamePhase {
    /** players in turn place one army on an unclaimed territory */
    CLAIM,
    /** players in turn place one of their remaining starting armies on a territory they hold */
    PLACE_INITIAL,
    /** the current player trades in RISK cards and places their new armies */
    REINFORCE,
    /** the current player attacks, or ends their attack */
    ATTACK,
    /** the current player makes their one fortifying move, or ends their turn */
    FORTIFY
}

package games.diplomacy;

/**
 * How convoy paradoxes are resolved (DATC issue 4.A.2).
 */
public enum DiplomacyParadoxRule {
    /**
     * The 2000 rulebook's rules 21 and 22: a convoyed army does not cut the support of an attack on one of its
     * convoying fleets (unless it has another successful route). A paradox these leave unresolved falls back on the
     * Szykman rule.
     */
    RULEBOOK_2000,
    /**
     * The Szykman rule alone (DATC's preference): a convoyed army cuts support as any other attacker, and in a
     * paradox the convoying fleets involved hold, so the army does not move.
     */
    SZYKMAN
}

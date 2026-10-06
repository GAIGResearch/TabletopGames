package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Resolving orders with support (rulebook p.7-11, Diagrams 8-18): strengths, dislodgement, standoffs with support,
 * matching of supports to orders, cutting support, and a dislodged unit's effect elsewhere. Each test clears the
 * board, places only the units it needs, and plays a Spring 1901 orders phase. Where a unit is dislodged, the test
 * also checks the retreat phase it leads to (the retreat rules themselves are in DiplomacyRetreatTest).
 * <p>
 * Strengths are written "attack a vs hold h" etc. in the comments. A support "succeeds" in the results when it
 * counted: it matches the order given, and is not cut (nor its unit dislodged).
 */
public class DiplomacySupportTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    @Test
    public void supportedAttackDislodgesAHoldingUnit() {
        // Diagram 8: A Mar-Bur supported by A Gas: attack 2 vs hold 1, so A Bur (Germany) is dislodged
        place(state, FRANCE, "A Mar", "A Gas");
        place(state, GERMANY, "A Bur");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Mar-Bur", "A Gas S A Mar-Bur", "A Bur H");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
        assertNull(state.getUnit(prov(state, "Mar")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Gas")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Bur")));
        assertEquals(prov(state, "Mar"), state.getDislodgedFrom(prov(state, "Bur")));
        assertEquals(Set.of(result(state, FRANCE, "A Mar-Bur", true), result(state, FRANCE, "A Gas S A Mar-Bur", true),
                result(state, GERMANY, "A Bur H", false)), lastResults(state));
        // the Spring retreats follow, with Germany (the only power with a dislodged unit) to order
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(GERMANY, state.getCurrentPlayer());
        assertEquals(1901, state.getYear());
        assertNoOrders(state);
    }

    @Test
    public void supporterNeedNotBeAdjacentToTheUnitItSupports() {
        // Diagram 9: A Sil-Pru, F Bal S A Sil-Pru (Bal and Sil are not adjacent): attack 2 vs hold 1
        place(state, GERMANY, "A Sil", "F Bal");
        place(state, RUSSIA, "A Pru");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Sil-Pru", "F Bal S A Sil-Pru", "A Pru H");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Pru")));
        assertNull(state.getUnit(prov(state, "Sil")));
        assertEquals(army(RUSSIA), state.getDislodged(prov(state, "Pru")));
        assertEquals(prov(state, "Sil"), state.getDislodgedFrom(prov(state, "Pru")));
        assertEquals(Set.of(result(state, GERMANY, "A Sil-Pru", true), result(state, GERMANY, "F Bal S A Sil-Pru", true),
                result(state, RUSSIA, "A Pru H", false)), lastResults(state));
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(RUSSIA, state.getCurrentPlayer());
    }

    @Test
    public void equallySupportedAttacksStandOff() {
        // Diagram 10: F GoL-Tyn (2, with F Wes) and F Nap-Tyn (2, with F Rom): each attack 2 vs prevent 2 - both
        // fail, Tyn stays vacant, nothing is dislodged so the retreat phase is skipped
        place(state, FRANCE, "F GoL", "F Wes");
        place(state, ITALY, "F Nap", "F Rom");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F GoL-Tyn", "F Wes S F GoL-Tyn", "F Nap-Tyn", "F Rom S F Nap-Tyn");
        assertNull(state.getUnit(prov(state, "Tyn")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "GoL")));
        assertEquals(fleet(ITALY), state.getUnit(prov(state, "Nap")));
        assertEquals(Set.of(result(state, FRANCE, "F GoL-Tyn", false), result(state, FRANCE, "F Wes S F GoL-Tyn", true),
                result(state, ITALY, "F Nap-Tyn", false), result(state, ITALY, "F Rom S F Nap-Tyn", true)),
                lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void standoffDoesNotDislodgeAUnitAlreadyThere() {
        // Diagram 10 with an Austrian fleet holding in Tyn: each attack 2 beats hold 1 but not the other's
        // prevent 2, so both fail and F Tyn is not dislodged
        place(state, FRANCE, "F GoL", "F Wes");
        place(state, ITALY, "F Nap", "F Rom");
        place(state, AUSTRIA, "F Tyn");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Tyn H", "F GoL-Tyn", "F Wes S F GoL-Tyn", "F Nap-Tyn", "F Rom S F Nap-Tyn");
        assertEquals(fleet(AUSTRIA), state.getUnit(prov(state, "Tyn")));
        assertNull(state.getDislodged(prov(state, "Tyn")));
        assertTrue(lastResults(state).contains(result(state, AUSTRIA, "F Tyn H", true)));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void supportedHoldResistsAnEquallySupportedAttack() {
        // Diagram 11: F GoL-Tyn with F Wes: attack 2 vs hold 1 + F Rom's hold support = 2 - fails
        place(state, FRANCE, "F GoL", "F Wes");
        place(state, ITALY, "F Tyn", "F Rom");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F GoL-Tyn", "F Wes S F GoL-Tyn", "F Tyn H", "F Rom S F Tyn");
        assertEquals(fleet(ITALY), state.getUnit(prov(state, "Tyn")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "GoL")));
        assertEquals(Set.of(result(state, FRANCE, "F GoL-Tyn", false), result(state, FRANCE, "F Wes S F GoL-Tyn", true),
                result(state, ITALY, "F Tyn H", true), result(state, ITALY, "F Rom S F Tyn", true)), lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void holdSupportFailsWhenTheSupportedUnitMoves() {
        // p.7 "F Den S F Bal" is invalid if F Bal moves (DATC 6.D.7 with Den in place of Pru).
        // F Bal-Swe: attack 1 vs A Fin-Swe's prevent 1 - fails (and A Fin-Swe fails too: F Bal is dislodged, but
        // not by a unit from Swe, so it still prevents). F Lvn-Bal: attack 2 (F Bot) vs hold of Bal: a unit whose
        // move failed has hold strength 1, with no hold support - so F Bal is dislodged
        place(state, GERMANY, "F Bal", "F Den");
        place(state, RUSSIA, "F Lvn", "F Bot", "A Fin");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Bal-Swe", "F Den S F Bal", "F Lvn-Bal", "F Bot S F Lvn-Bal", "A Fin-Swe");
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Bal")));
        assertNull(state.getUnit(prov(state, "Swe")));
        assertEquals(fleet(GERMANY), state.getDislodged(prov(state, "Bal")));
        assertEquals(prov(state, "Lvn"), state.getDislodgedFrom(prov(state, "Bal")));
        assertEquals(Set.of(result(state, GERMANY, "F Bal-Swe", false), result(state, GERMANY, "F Den S F Bal", false),
                result(state, RUSSIA, "F Lvn-Bal", true), result(state, RUSSIA, "F Bot S F Lvn-Bal", true),
                result(state, RUSSIA, "A Fin-Swe", false)), lastResults(state));
        // F Bal's retreats: fleet moves Ber, Bot (occupied), Den (occupied), Kie, Lvn (attacker's origin), Pru,
        // Swe (left vacant by the standoff)
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertTrue(state.isStandoff(prov(state, "Swe")));
        assertEquals(orderSet(state, "F Bal R Ber", "F Bal R Kie", "F Bal R Pru", "Disband Bal"), legalSet(state, fm));
    }

    @Test
    public void holdSupportCountsWhenTheSupportedUnitHolds() {
        // the same with F Bal holding: hold 1 + F Den 1 = 2 vs attack 2 - F Bal stays
        place(state, GERMANY, "F Bal", "F Den");
        place(state, RUSSIA, "F Lvn", "F Bot");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Bal H", "F Den S F Bal", "F Lvn-Bal", "F Bot S F Lvn-Bal");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Bal")));
        assertEquals(Set.of(result(state, GERMANY, "F Bal H", true), result(state, GERMANY, "F Den S F Bal", true),
                result(state, RUSSIA, "F Lvn-Bal", false), result(state, RUSSIA, "F Bot S F Lvn-Bal", true)),
                lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void moveSupportForADifferentMoveFails() {
        // p.7: A Boh S A Mun-Sil, but A Mun-Tyr is ordered: the support does not count for Mun-Tyr, so attack 1
        // vs the Italian A Tyr's hold 1 - fails
        place(state, AUSTRIA, "A Boh");
        place(state, GERMANY, "A Mun");
        place(state, ITALY, "A Tyr");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Boh S A Mun-Sil", "A Mun-Tyr", "A Tyr H");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Mun")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Tyr")));
        assertEquals(Set.of(result(state, AUSTRIA, "A Boh S A Mun-Sil", false), result(state, GERMANY, "A Mun-Tyr", false),
                result(state, ITALY, "A Tyr H", true)), lastResults(state));
    }

    @Test
    public void moveSupportDoesNotBecomeAHoldSupport() {
        // p.7: A Boh S A Mun-Sil while A Mun holds: it is not a hold support, so the Russian A Sil-Mun with A Tyr's
        // support (attack 2) beats A Mun's hold 1 and dislodges it
        place(state, AUSTRIA, "A Boh");
        place(state, GERMANY, "A Mun");
        place(state, RUSSIA, "A Sil", "A Tyr");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Boh S A Mun-Sil", "A Mun H", "A Sil-Mun", "A Tyr S A Sil-Mun");
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Mun")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Mun")));
        assertEquals(Set.of(result(state, AUSTRIA, "A Boh S A Mun-Sil", false), result(state, GERMANY, "A Mun H", false),
                result(state, RUSSIA, "A Sil-Mun", true), result(state, RUSSIA, "A Tyr S A Sil-Mun", true)),
                lastResults(state));
    }

    @Test
    public void dislodgedUnitStillCausesAStandoffElsewhere() {
        // Diagram 12: A Boh-Mun (2, with A Tyr) dislodges A Mun, whose own move A Mun-Sil (2, with A Ber) stands off
        // with A War-Sil (2, with A Pru): Mun was not dislodged from Sil, so it still prevents with 2
        // (rule 10, DATC 4.A.7 b). A Mun-Sil: 2 vs prevent 2 fails, so Mun's hold is 1 < 2
        place(state, AUSTRIA, "A Boh", "A Tyr");
        place(state, GERMANY, "A Mun", "A Ber");
        place(state, RUSSIA, "A War", "A Pru");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Boh-Mun", "A Tyr S A Boh-Mun", "A Mun-Sil", "A Ber S A Mun-Sil", "A War-Sil",
                "A Pru S A War-Sil");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Mun")));
        assertNull(state.getUnit(prov(state, "Sil")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "War")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Mun")));
        assertEquals(prov(state, "Boh"), state.getDislodgedFrom(prov(state, "Mun")));
        assertEquals(Set.of(result(state, AUSTRIA, "A Boh-Mun", true), result(state, AUSTRIA, "A Tyr S A Boh-Mun", true),
                result(state, GERMANY, "A Mun-Sil", false), result(state, GERMANY, "A Ber S A Mun-Sil", true),
                result(state, RUSSIA, "A War-Sil", false), result(state, RUSSIA, "A Pru S A War-Sil", true)),
                lastResults(state));
        // A Mun's retreats: Ber and Tyr occupied, Boh the attacker's origin, Sil left vacant by the standoff
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertTrue(state.isStandoff(prov(state, "Sil")));
        assertEquals(GERMANY, state.getCurrentPlayer());
        assertEquals(orderSet(state, "A Mun R Bur", "A Mun R Kie", "A Mun R Ruh", "Disband Mun"), legalSet(state, fm));
    }

    @Test
    public void unitDislodgedHeadToHeadHasNoEffectOnTheAttackersProvince() {
        // Diagram 13: A Rum-Bul (2, with A Ser) vs A Bul-Rum head to head: 2 vs defend 1 - Bul is dislodged, and
        // so has no effect on Rum (prevent 0): A Sev-Rum (1) enters Rum, vacated by the successful A Rum-Bul
        place(state, TURKEY, "A Bul");
        place(state, RUSSIA, "A Rum", "A Ser", "A Sev");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Bul-Rum", "A Rum-Bul", "A Ser S A Rum-Bul", "A Sev-Rum");
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Bul")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Rum")));
        assertNull(state.getUnit(prov(state, "Sev")));
        assertEquals(army(TURKEY), state.getDislodged(prov(state, "Bul")));
        assertEquals(prov(state, "Rum"), state.getDislodgedFrom(prov(state, "Bul")));
        assertEquals(Set.of(result(state, TURKEY, "A Bul-Rum", false), result(state, RUSSIA, "A Rum-Bul", true),
                result(state, RUSSIA, "A Ser S A Rum-Bul", true), result(state, RUSSIA, "A Sev-Rum", true)),
                lastResults(state));
        // A Bul's retreats: army moves Con, Gre, Rum (occupied, and the attacker's origin), Ser (occupied)
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(orderSet(state, "A Bul R Con", "A Bul R Gre", "Disband Bul"), legalSet(state, fm));
    }

    @Test
    public void provinceLeftByTheHeadToHeadWinnerIsNotAStandoff() {
        // p.9, after Diagram 14: without A Sev-Rum, Rum is left vacant, "but not as the result of a standoff":
        // the failed A Bul-Rum lost the head-to-head battle and has no effect on Rum
        place(state, TURKEY, "A Bul");
        place(state, RUSSIA, "A Rum", "A Ser");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Bul-Rum", "A Rum-Bul", "A Ser S A Rum-Bul");
        assertNull(state.getUnit(prov(state, "Rum")));
        assertEquals(army(TURKEY), state.getDislodged(prov(state, "Bul")));
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertFalse(state.isStandoff(prov(state, "Rum")));
    }

    @Test
    public void supportedUnitDislodgedHeadToHeadAlsoHasNoEffect() {
        // Diagram 14: A Rum-Bul (3, with A Gre and A Ser) vs A Bul-Rum (defend 2, with F Bla): Bul is dislodged;
        // A Sev-Rum (1) still enters Rum (the loser's prevent is 0). F Bla's support counted, though in vain
        place(state, TURKEY, "A Bul", "F Bla");
        place(state, RUSSIA, "A Rum", "A Gre", "A Ser", "A Sev");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Bul-Rum", "F Bla S A Bul-Rum", "A Rum-Bul", "A Gre S A Rum-Bul", "A Ser S A Rum-Bul",
                "A Sev-Rum");
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Bul")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Rum")));
        assertEquals(army(TURKEY), state.getDislodged(prov(state, "Bul")));
        assertEquals(Set.of(result(state, TURKEY, "A Bul-Rum", false), result(state, TURKEY, "F Bla S A Bul-Rum", true),
                result(state, RUSSIA, "A Rum-Bul", true), result(state, RUSSIA, "A Gre S A Rum-Bul", true),
                result(state, RUSSIA, "A Ser S A Rum-Bul", true), result(state, RUSSIA, "A Sev-Rum", true)),
                lastResults(state));
    }

    @Test
    public void attackOnTheSupporterCutsItsSupport() {
        // Diagram 15: A Boh-Sil cuts A Sil's support (it comes from Boh, not War, where the support is given),
        // though it fails (1 vs hold 1). A Pru-War: 1 vs hold 1 - fails
        place(state, GERMANY, "A Pru", "A Sil");
        place(state, RUSSIA, "A War", "A Boh");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Pru-War", "A Sil S A Pru-War", "A War H", "A Boh-Sil");
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "War")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Sil")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Pru")));
        assertEquals(Set.of(result(state, GERMANY, "A Pru-War", false), result(state, GERMANY, "A Sil S A Pru-War", false),
                result(state, RUSSIA, "A War H", true), result(state, RUSSIA, "A Boh-Sil", false)), lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void attackFromTheProvinceSupportedIntoDoesNotCut() {
        // Diagram 16: A War-Sil comes from War, where A Sil's support is given: not cut. A Pru-War: 2 vs War's hold
        // (A War-Sil fails, 1 vs hold 1, so 1) - A War is dislodged. Not head to head: War moves to Sil, not Pru
        place(state, GERMANY, "A Pru", "A Sil");
        place(state, RUSSIA, "A War");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Pru-War", "A Sil S A Pru-War", "A War-Sil");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "War")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Sil")));
        assertNull(state.getUnit(prov(state, "Pru")));
        assertEquals(army(RUSSIA), state.getDislodged(prov(state, "War")));
        assertEquals(prov(state, "Pru"), state.getDislodgedFrom(prov(state, "War")));
        assertEquals(Set.of(result(state, GERMANY, "A Pru-War", true), result(state, GERMANY, "A Sil S A Pru-War", true),
                result(state, RUSSIA, "A War-Sil", false)), lastResults(state));
        // A War's retreats: Gal, Lvn, Mos, Ukr (Pru is the attacker's origin, Sil occupied)
        assertEquals(orderSet(state, "A War R Gal", "A War R Lvn", "A War R Mos", "A War R Ukr", "Disband War"),
                legalSet(state, fm));
    }

    @Test
    public void dislodgementCutsSupportEvenFromTheProvinceSupportedInto() {
        // Diagram 17: A Pru-Sil (2, with A War) dislodges A Sil, cutting its support for F Ber-Pru (rule: a
        // dislodged supporter's support is cut, whichever province the attack came from). F Ber-Pru (1) then
        // stands off with F Bal-Pru (1); Pru, vacated by A Pru, is left empty by a standoff
        place(state, GERMANY, "F Ber", "A Sil");
        place(state, RUSSIA, "A Pru", "A War", "F Bal");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Ber-Pru", "A Sil S F Ber-Pru", "A Pru-Sil", "A War S A Pru-Sil", "F Bal-Pru");
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Sil")));
        assertNull(state.getUnit(prov(state, "Pru")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Bal")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Sil")));
        assertEquals(prov(state, "Pru"), state.getDislodgedFrom(prov(state, "Sil")));
        assertEquals(Set.of(result(state, GERMANY, "F Ber-Pru", false), result(state, GERMANY, "A Sil S F Ber-Pru", false),
                result(state, RUSSIA, "A Pru-Sil", true), result(state, RUSSIA, "A War S A Pru-Sil", true),
                result(state, RUSSIA, "F Bal-Pru", false)), lastResults(state));
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertTrue(state.isStandoff(prov(state, "Pru")));
        // A Sil's retreats: Ber and War occupied, Pru the attacker's origin (and a standoff)
        assertEquals(orderSet(state, "A Sil R Boh", "A Sil R Gal", "A Sil R Mun", "Disband Sil"), legalSet(state, fm));
    }

    @Test
    public void unitDislodgedFromOneProvinceStillCutsSupportInAnother() {
        // Diagram 18: A Mun-Sil cuts A Sil's support for A Pru-Ber (it does not come from Ber), although A Mun is
        // dislodged by A Boh-Mun (2, with A Tyr; Mun-Sil fails 1 vs hold 1, so Mun's hold is 1).
        // A Pru-Ber is then 1 vs hold 1 - fails
        place(state, GERMANY, "A Ber", "A Mun");
        place(state, RUSSIA, "A Pru", "A Sil", "A Boh", "A Tyr");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Ber H", "A Mun-Sil", "A Pru-Ber", "A Sil S A Pru-Ber", "A Boh-Mun", "A Tyr S A Boh-Mun");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Mun")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Pru")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Mun")));
        assertEquals(prov(state, "Boh"), state.getDislodgedFrom(prov(state, "Mun")));
        assertEquals(Set.of(result(state, GERMANY, "A Ber H", true), result(state, GERMANY, "A Mun-Sil", false),
                result(state, RUSSIA, "A Pru-Ber", false), result(state, RUSSIA, "A Sil S A Pru-Ber", false),
                result(state, RUSSIA, "A Boh-Mun", true), result(state, RUSSIA, "A Tyr S A Boh-Mun", true)),
                lastResults(state));
        // A Mun's retreats: Ber, Sil, Tyr occupied; Boh the attacker's origin
        assertEquals(orderSet(state, "A Mun R Bur", "A Mun R Kie", "A Mun R Ruh", "Disband Mun"), legalSet(state, fm));
    }

    @Test
    public void attackByTheSupportersOwnPowerDoesNotCutSupport() {
        // rule 16 (p.14): France's A Bre-Gas does not cut A Gas's support (and fails: attack on its own unit, 0).
        // A Mar-Bur: 2 vs hold 1 - dislodges A Bur
        place(state, FRANCE, "A Mar", "A Gas", "A Bre");
        place(state, GERMANY, "A Bur");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Mar-Bur", "A Gas S A Mar-Bur", "A Bre-Gas", "A Bur H");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Gas")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bre")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Bur")));
        assertEquals(Set.of(result(state, FRANCE, "A Mar-Bur", true), result(state, FRANCE, "A Gas S A Mar-Bur", true),
                result(state, FRANCE, "A Bre-Gas", false), result(state, GERMANY, "A Bur H", false)), lastResults(state));
    }

    @Test
    public void attackByAnotherPowerCutsSupportThoughItFails() {
        // the same with the army in Brest English: its attack cuts A Gas's support (and fails, 1 vs hold 1), so
        // A Mar-Bur is 1 vs hold 1 - fails
        place(state, FRANCE, "A Mar", "A Gas");
        place(state, ENGLAND, "A Bre");
        place(state, GERMANY, "A Bur");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Mar-Bur", "A Gas S A Mar-Bur", "A Bre-Gas", "A Bur H");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Bur")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Mar")));
        assertEquals(Set.of(result(state, FRANCE, "A Mar-Bur", false), result(state, FRANCE, "A Gas S A Mar-Bur", false),
                result(state, ENGLAND, "A Bre-Gas", false), result(state, GERMANY, "A Bur H", true)), lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }
}

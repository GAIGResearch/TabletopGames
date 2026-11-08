package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The retreat phases (rulebook p.17; DATC 6.H): who orders, the legal retreats, their resolution, automatic
 * disbanding with no legal retreat, phase flow, and centre ownership after the Fall retreats. Arranged retreat
 * positions use the test-only setDislodged / setStandoff hooks (the unit standing in the province is then the
 * attacker); the others come from a real orders phase. DATC 6.H.3 and 6.H.4 (illegal orders) are not here.
 */
public class DiplomacyRetreatTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    // ---- who orders ----

    @Test
    public void onlyPowersWithDislodgedUnitsOrderInARetreatPhase() {
        // Germany has two dislodged units (Kie, Mun: index order); France has a unit on the board but none dislodged
        place(state, ENGLAND, "A Kie");
        place(state, AUSTRIA, "A Mun");
        place(state, FRANCE, "A Par");
        dislodge(state, GERMANY, "A Kie", "Hol");
        dislodge(state, GERMANY, "A Mun", "Boh");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, GERMANY);
        assertEquals(List.of(prov(state, "Kie"), prov(state, "Mun")), state.unitsToOrder(GERMANY));
        assertTrue(state.hasOrdersToGive(GERMANY));
        assertEquals(List.of(), state.unitsToOrder(FRANCE));
        assertFalse(state.hasOrdersToGive(FRANCE));
        assertFalse(state.hasOrdersToGive(ENGLAND));
        // the first unit to order is Kie; once it is ordered, Mun
        fm.next(state, order(state, "Disband Kie"));
        assertEquals(List.of(prov(state, "Mun")), state.unitsToOrder(GERMANY));
        assertEquals(GERMANY, state.getCurrentPlayer());
    }

    @Test
    public void turnPassesBetweenPowersAndThenToTheFallOrders() {
        // Germany then Russia retreat; the Fall orders then start with Austria (the first power with units)
        place(state, AUSTRIA, "A Vie");
        place(state, ENGLAND, "A Kie");
        place(state, TURKEY, "A Sev");
        dislodge(state, GERMANY, "A Kie", "Hol");
        dislodge(state, RUSSIA, "A Sev", "Arm");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, GERMANY);
        fm.next(state, order(state, "A Kie R Den"));
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(RUSSIA, state.getCurrentPlayer());
        fm.next(state, order(state, "A Sev R Ukr"));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(1901, state.getYear());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Den")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Ukr")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void otherPowersRetreatOrdersAreHiddenInACopy() {
        // Germany has ordered its retreat; Russia's copy wipes it, so Germany orders again in the copy, after Russia
        place(state, ENGLAND, "A Kie");
        place(state, TURKEY, "A Sev");
        dislodge(state, GERMANY, "A Kie", "Hol");
        dislodge(state, RUSSIA, "A Sev", "Arm");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, GERMANY);
        fm.next(state, order(state, "A Kie R Den"));
        assertEquals(RUSSIA, state.getCurrentPlayer());
        DiplomacyGameState copy = (DiplomacyGameState) state.copy(RUSSIA);
        assertEquals(List.of(), copy.getOrders(GERMANY));
        assertEquals(List.of(prov(state, "Kie")), copy.unitsToOrder(GERMANY));
        assertEquals(RUSSIA, copy.getCurrentPlayer());
        assertEquals(List.of(order(state, "A Kie R Den")), state.getOrders(GERMANY));
    }

    // ---- legal retreats (arranged) ----

    @Test
    public void armyRetreatsOnlyByAnOrdinaryMoveToAFreeProvince() {
        // A Kie dislodged by a unit from Hol (an English army now in Kie). Army moves from Kie: Ber, Den, Hol, Mun,
        // Ruh (not the seas Bal, Hel). Ber occupied, Hol the attacker's origin (empty), Mun left vacant by a
        // standoff: Den and Ruh remain
        place(state, ENGLAND, "A Kie");
        place(state, GERMANY, "A Ber");
        dislodge(state, GERMANY, "A Kie", "Hol");
        state.setStandoff(prov(state, "Mun"), true);
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, GERMANY);
        assertEquals(orderSet(state, "A Kie R Den", "A Kie R Ruh", "Disband Kie"), legalSet(state, fm));
    }

    @Test
    public void fleetRetreatKeepsToItsCoastsAndNamesTheCoastItReaches() {
        // F Gas (England) dislodged by an army from Bur: fleet moves from Gas are Bre, Mid, Spa/nc. Bre occupied
        place(state, FRANCE, "A Gas", "F Bre");
        dislodge(state, ENGLAND, "F Gas", "Bur");
        startPhase(state, DiplomacyPhase.FALL_RETREATS, ENGLAND);
        assertEquals(orderSet(state, "F Gas R Mid", "F Gas R Spa/nc", "Disband Gas"), legalSet(state, fm));
    }

    @Test
    public void fleetDislodgedFromASplitCoastRetreatsFromThatCoast() {
        // F Spa/sc (Italy) dislodged by a unit from Mar: moves from the south coast are GoL, Mar, Mid, Por, Wes
        // (not Gas, reached only from the north coast). Mar is the attacker's origin, Wes occupied
        place(state, FRANCE, "A Spa", "F Wes");
        dislodge(state, ITALY, "F Spa/sc", "Mar");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, ITALY);
        assertEquals(orderSet(state, "F Spa/sc R GoL", "F Spa/sc R Mid", "F Spa/sc R Por", "Disband Spa"),
                legalSet(state, fm));
    }

    // ---- resolution (arranged) ----

    @Test
    public void retreatingFleetArrivesOnTheCoastNamed() {
        place(state, FRANCE, "A Gas", "F Bre");
        dislodge(state, ENGLAND, "F Gas", "Bur");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, ENGLAND);
        play(state, fm, "F Gas R Spa/nc");
        assertEquals(fleet(ENGLAND, "nc"), state.getUnit(prov(state, "Spa")));
        assertNull(state.getDislodged(prov(state, "Gas")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Gas")));
        assertEquals(Set.of(result(state, ENGLAND, "F Gas R Spa/nc", true)), lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertNoOrders(state);
    }

    @Test
    public void disbandRemovesTheDislodgedUnit() {
        place(state, ENGLAND, "A Kie");
        dislodge(state, GERMANY, "A Kie", "Hol");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, GERMANY);
        play(state, fm, "Disband Kie");
        assertNull(state.getDislodged(prov(state, "Kie")));
        assertEquals(1, totalUnits(state));
        assertEquals(Set.of(result(state, GERMANY, "Disband Kie", true)), lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void retreatsOfDifferentPowersToOneProvinceAreAllDisbanded() {
        // German A Mun (from Boh) and Italian A Pie (from Mar) both retreat to Tyr: both disbanded, Tyr empty
        place(state, AUSTRIA, "A Mun");
        place(state, FRANCE, "A Pie");
        dislodge(state, GERMANY, "A Mun", "Boh");
        dislodge(state, ITALY, "A Pie", "Mar");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, GERMANY);
        play(state, fm, "A Mun R Tyr", "A Pie R Tyr");
        assertNull(state.getUnit(prov(state, "Tyr")));
        assertEquals(0, totalDislodged(state));
        assertEquals(2, totalUnits(state));
        assertEquals(Set.of(result(state, GERMANY, "A Mun R Tyr", false), result(state, ITALY, "A Pie R Tyr", false)),
                lastResults(state));
    }

    // ---- from a real orders phase ----

    @Test
    public void unitWithNoLegalRetreatIsDisbandedAndThePhaseSkipped() {
        // DATC 6.H.15: F Spa/sc-Por (2, with F Mid) dislodges F Por. Its fleet moves: Mid (occupied), Spa/nc and
        // Spa/sc (Spa is the attacker's origin, whichever coast). No retreat: disbanded at once, no decision, and
        // with no other dislodged unit the retreat phase is skipped. The disbanding is not in the results
        place(state, ENGLAND, "F Por");
        place(state, FRANCE, "F Spa/sc", "F Mid");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        playExpecting(state, fm, "F Por H: fails", "F Spa/sc-Por: ok", "F Mid S F Spa/sc-Por: ok");
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Por")));
        assertNull(state.getDislodged(prov(state, "Por")));
        assertEquals(2, totalUnits(state));
        assertEquals(0, state.nUnits(ENGLAND));
    }

    @Test
    public void unitWithNoLegalRetreatIsDisbandedWhileOthersRetreat() {
        // as above, plus Diagram 8 (A Bur dislodged from Mar, which has retreats): only Germany orders
        place(state, ENGLAND, "F Por");
        place(state, FRANCE, "F Spa/sc", "F Mid", "A Mar", "A Gas");
        place(state, GERMANY, "A Bur");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Por H", "F Spa/sc-Por", "F Mid S F Spa/sc-Por", "A Mar-Bur", "A Gas S A Mar-Bur", "A Bur H");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertNull(state.getDislodged(prov(state, "Por")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Bur")));
        assertFalse(state.hasOrdersToGive(ENGLAND));
        assertEquals(GERMANY, state.getCurrentPlayer());
        // A Bur: Bel, Gas (occupied), Mar (origin), Mun, Par, Pic, Ruh
        assertEquals(orderSet(state, "A Bur R Bel", "A Bur R Mun", "A Bur R Par", "A Bur R Pic", "A Bur R Ruh",
                "Disband Bur"), legalSet(state, fm));
    }

    @Test
    public void fallRetreatIntoACentreTakesItAndOwnershipChangesOnlyAfterTheRetreats() {
        // Fall: A Boh-Mun (2, with A Tyr) dislodges the German A Mun. During the Fall retreats Mun is still
        // German. A Mun retreats to Kie (an English-owned centre here); after the retreats Kie becomes German and
        // Mun Austrian
        place(state, AUSTRIA, "A Boh", "A Tyr");
        place(state, GERMANY, "A Mun");
        own(state, ENGLAND, "Kie");
        startOrders(state, DiplomacyPhase.FALL_ORDERS);
        play(state, fm, "A Boh-Mun", "A Tyr S A Boh-Mun", "A Mun H");
        assertEquals(DiplomacyPhase.FALL_RETREATS, state.getPhase());
        assertEquals(GERMANY, state.getOwner(prov(state, "Mun")));
        assertEquals(ENGLAND, state.getOwner(prov(state, "Kie")));
        // A Mun: Ber, Bur, Kie, Ruh, Sil (Boh the origin, Tyr occupied)
        assertEquals(orderSet(state, "A Mun R Ber", "A Mun R Bur", "A Mun R Kie", "A Mun R Ruh", "A Mun R Sil",
                "Disband Mun"), legalSet(state, fm));
        play(state, fm, "A Mun R Kie");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Kie")));
        assertEquals(GERMANY, state.getOwner(prov(state, "Kie")));
        assertEquals(AUSTRIA, state.getOwner(prov(state, "Mun")));
        // the cleared board leaves every power with centres and few units, so there are builds to make
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(1901, state.getYear());
    }

    @Test
    public void d6h01NoSupportsDuringRetreat() {
        // DATC 6.H.1: A Tyr-Tri (2, with A Ven) dislodges F Tri; F Ion-Gre (2, with F Aeg) dislodges F Gre.
        // A Ser (not dislodged) has nothing to order. F Tri: fleet moves Adr, Alb, Ven (occupied).
        // F Gre: Aeg (occupied), Alb, Bul/sc, Ion (origin). Both retreat to Alb: both disbanded
        place(state, AUSTRIA, "F Tri", "A Ser");
        place(state, TURKEY, "F Gre");
        place(state, ITALY, "A Ven", "A Tyr", "F Ion", "F Aeg");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        playExpecting(state, fm, "F Tri H: fails", "A Ser H: ok", "F Gre H: fails", "A Ven S A Tyr-Tri: ok",
                "A Tyr-Tri: ok", "F Ion-Gre: ok", "F Aeg S F Ion-Gre: ok");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(List.of(prov(state, "Tri")), state.unitsToOrder(AUSTRIA));
        assertEquals(orderSet(state, "F Tri R Adr", "F Tri R Alb", "Disband Tri"), legalSet(state, fm));
        fm.next(state, order(state, "F Tri R Alb"));
        assertEquals(TURKEY, state.getCurrentPlayer());
        assertEquals(orderSet(state, "F Gre R Alb", "F Gre R Bul/sc", "Disband Gre"), legalSet(state, fm));
        fm.next(state, order(state, "F Gre R Alb"));
        assertNull(state.getUnit(prov(state, "Alb")));
        assertEquals(5, totalUnits(state));
        assertEquals(Set.of(result(state, AUSTRIA, "F Tri R Alb", false), result(state, TURKEY, "F Gre R Alb", false)),
                lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void d6h05NoRetreatToTheAreaFromWhichAttacked() {
        // F Bla-Ank (2, with F Con) dislodges F Ank: fleet moves Arm, Bla (origin), Con (occupied)
        place(state, RUSSIA, "F Con", "F Bla");
        place(state, TURKEY, "F Ank");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Con S F Bla-Ank", "F Bla-Ank", "F Ank H");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(prov(state, "Bla"), state.getDislodgedFrom(prov(state, "Ank")));
        assertEquals(orderSet(state, "F Ank R Arm", "Disband Ank"), legalSet(state, fm));
    }

    @Test
    public void d6h06NoRetreatToAContestedArea() {
        // A Tri-Vie (2) dislodges A Vie; A Mun-Boh and A Sil-Boh stand off. A Vie: Boh (standoff), Bud
        // (occupied), Gal, Tri (origin), Tyr
        place(state, AUSTRIA, "A Bud", "A Tri");
        place(state, GERMANY, "A Mun", "A Sil");
        place(state, ITALY, "A Vie");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Bud S A Tri-Vie", "A Tri-Vie", "A Mun-Boh", "A Sil-Boh", "A Vie H");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertTrue(state.isStandoff(prov(state, "Boh")));
        assertEquals(prov(state, "Tri"), state.getDislodgedFrom(prov(state, "Vie")));
        assertEquals(orderSet(state, "A Vie R Gal", "A Vie R Tyr", "Disband Vie"), legalSet(state, fm));
    }

    @Test
    public void d6h07MultipleRetreatsToTheSameAreaDisbandBoth() {
        // A Vie (from Tri) and A Boh (from Sil) of Italy are dislodged; both retreat to Tyr: both disbanded
        place(state, AUSTRIA, "A Bud", "A Tri");
        place(state, GERMANY, "A Mun", "A Sil");
        place(state, ITALY, "A Vie", "A Boh");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Bud S A Tri-Vie", "A Tri-Vie", "A Mun S A Sil-Boh", "A Sil-Boh", "A Vie H", "A Boh H");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(List.of(prov(state, "Boh"), prov(state, "Vie")), state.unitsToOrder(ITALY));
        play(state, fm, "A Boh R Tyr", "A Vie R Tyr");
        assertNull(state.getUnit(prov(state, "Tyr")));
        assertEquals(0, state.nUnits(ITALY));
        assertEquals(Set.of(result(state, ITALY, "A Boh R Tyr", false), result(state, ITALY, "A Vie R Tyr", false)),
                lastResults(state));
    }

    @Test
    public void d6h08TripleRetreatToTheSameAreaDisbandsAll() {
        // F Nwy, F Edi, F Hol are dislodged (each by a supported attack, 2 vs 1); all three retreat to Nth
        place(state, ENGLAND, "A Lvp", "F Yor", "F Nwy");
        place(state, GERMANY, "A Kie", "A Ruh");
        place(state, RUSSIA, "F Edi", "A Swe", "A Fin", "F Hol");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        playExpecting(state, fm, "A Lvp-Edi: ok", "F Yor S A Lvp-Edi: ok", "F Nwy H: fails", "A Kie S A Ruh-Hol: ok",
                "A Ruh-Hol: ok", "F Edi H: fails", "A Swe S A Fin-Nwy: ok", "A Fin-Nwy: ok", "F Hol H: fails");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        play(state, fm, "F Nwy R Nth", "F Edi R Nth", "F Hol R Nth");
        assertNull(state.getUnit(prov(state, "Nth")));
        assertEquals(Set.of(result(state, ENGLAND, "F Nwy R Nth", false), result(state, RUSSIA, "F Edi R Nth", false),
                result(state, RUSSIA, "F Hol R Nth", false)), lastResults(state));
        // left: A Edi, F Yor (England), A Kie, A Hol (Germany), A Swe, A Nwy (Russia)
        assertEquals(6, totalUnits(state));
    }

    @Test
    public void d6h09DislodgedUnitDoesNotMakeTheAttackersAreaContested() {
        // A Ber-Pru (2) beats A Pru-Ber head to head; F Hel-Kie (2) dislodges F Kie. Ber is left empty, but the
        // failed A Pru-Ber lost the head-to-head, so Ber is not a standoff: F Kie may retreat there.
        // F Kie: Bal, Ber, Den (occupied), Hel (origin), Hol. A Pru: Ber (its attacker's origin), Lvn, Sil
        // (occupied), War
        place(state, ENGLAND, "F Hel", "F Den");
        place(state, GERMANY, "A Ber", "F Kie", "A Sil");
        place(state, RUSSIA, "A Pru");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        playExpecting(state, fm, "F Hel-Kie: ok", "F Den S F Hel-Kie: ok", "A Ber-Pru: ok", "F Kie H: fails",
                "A Sil S A Ber-Pru: ok", "A Pru-Ber: fails");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertFalse(state.isStandoff(prov(state, "Ber")));
        assertEquals(GERMANY, state.getCurrentPlayer());
        assertEquals(orderSet(state, "F Kie R Bal", "F Kie R Ber", "F Kie R Hol", "Disband Kie"), legalSet(state, fm));
        fm.next(state, order(state, "F Kie R Ber"));
        assertEquals(RUSSIA, state.getCurrentPlayer());
        assertEquals(orderSet(state, "A Pru R Lvn", "A Pru R War", "Disband Pru"), legalSet(state, fm));
    }

    @Test
    public void d6h11RetreatWhenDislodgedByAdjacentConvoy() {
        // A Gas-Mar via convoy (Mid, Wes, GoL) 2 (A Bur) dislodges A Mar. 4.A.5 b: a unit dislodged by a convoyed
        // army may retreat to the army's origin. A Mar: Gas (vacated), Pie, Spa; Bur occupied
        place(state, FRANCE, "A Gas", "A Bur", "F Mid", "F Wes", "F GoL");
        place(state, ITALY, "A Mar");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        playExpecting(state, fm, "A Gas-Mar via convoy: ok", "A Bur S A Gas-Mar: ok", "F Mid C A Gas-Mar: ok",
                "F Wes C A Gas-Mar: ok", "F GoL C A Gas-Mar: ok", "A Mar H: fails");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(ITALY, state.getCurrentPlayer());
        assertEquals(orderSet(state, "A Mar R Gas", "A Mar R Pie", "A Mar R Spa", "Disband Mar"), legalSet(state, fm));
    }

    @Test
    public void d6h12RetreatWhenDislodgedByAdjacentConvoyWhileTryingToDoTheSame() {
        // F Bre-Eng 2 (F Mid) dislodges F Eng: England's only route Lvp-Iri-Eng-Nth-Edi is broken, A Lvp stays
        // (hold 1) and A Edi-Lvp via convoy (Nrg, NAt) 2 (A Cly) dislodges it. The failed convoyed move never
        // reached Edi, so Edi is no standoff; 4.A.5 b opens it to A Lvp
        place(state, ENGLAND, "A Lvp", "F Iri", "F Eng", "F Nth");
        place(state, FRANCE, "F Bre", "F Mid");
        place(state, RUSSIA, "A Edi", "F Nrg", "F NAt", "A Cly");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        playExpecting(state, fm, "A Lvp-Edi via convoy: fails", "F Iri C A Lvp-Edi: ok", "F Eng C A Lvp-Edi: fails",
                "F Nth C A Lvp-Edi: ok", "F Bre-Eng: ok", "F Mid S F Bre-Eng: ok", "A Edi-Lvp via convoy: ok",
                "F Nrg C A Edi-Lvp: ok", "F NAt C A Edi-Lvp: ok", "A Cly S A Edi-Lvp: ok");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertFalse(state.isStandoff(prov(state, "Edi")));
        assertEquals(ENGLAND, state.getCurrentPlayer());
        // F Eng first (province order): Bel, Lon, Pic, Wal (Bre the attacker's origin; Iri, Mid, Nth occupied)
        assertEquals(orderSet(state, "F Eng R Bel", "F Eng R Lon", "F Eng R Pic", "F Eng R Wal", "Disband Eng"),
                legalSet(state, fm));
        fm.next(state, order(state, "Disband Eng"));
        // A Lvp: Edi, Wal, Yor (Cly occupied)
        assertEquals(orderSet(state, "A Lvp R Edi", "A Lvp R Wal", "A Lvp R Yor", "Disband Lvp"), legalSet(state, fm));
    }

    @Test
    public void d6h13NoRetreatWithConvoyInMovementPhase() {
        // A Par-Pic 2 (A Bre) dislodges A Pic; F Eng's convoy from the orders phase gives no retreat to Lon.
        // A Pic: Bel, Bur (Bre occupied, Par the attacker's origin)
        place(state, ENGLAND, "A Pic", "F Eng");
        place(state, FRANCE, "A Par", "A Bre");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        playExpecting(state, fm, "A Pic H: fails", "F Eng C A Pic-Lon: ok", "A Par-Pic: ok", "A Bre S A Par-Pic: ok");
        assertEquals(orderSet(state, "A Pic R Bel", "A Pic R Bur", "Disband Pic"), legalSet(state, fm));
    }

    @Test
    public void d6h10AttackersOriginIsClosedOnlyToTheUnitItDislodged() {
        // A Ber-Kie (2) dislodges the English A Kie; A War-Pru (2) dislodges the German A Pru. Ber is the origin of
        // Kie's attacker (closed to A Kie: Den, Hol, Ruh; Mun occupied) but open to A Pru (Ber, Lvn; Sil occupied,
        // War its own attacker's origin)
        place(state, ENGLAND, "A Kie");
        place(state, GERMANY, "A Ber", "A Mun", "A Pru");
        place(state, RUSSIA, "A War", "A Sil");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Kie H", "A Ber-Kie", "A Mun S A Ber-Kie", "A Pru H", "A War-Pru", "A Sil S A War-Pru");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertEquals(ENGLAND, state.getCurrentPlayer());
        assertEquals(orderSet(state, "A Kie R Den", "A Kie R Hol", "A Kie R Ruh", "Disband Kie"), legalSet(state, fm));
        fm.next(state, order(state, "Disband Kie"));
        assertEquals(GERMANY, state.getCurrentPlayer());
        assertEquals(orderSet(state, "A Pru R Ber", "A Pru R Lvn", "Disband Pru"), legalSet(state, fm));
        fm.next(state, order(state, "A Pru R Ber"));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(Set.of(result(state, ENGLAND, "Disband Kie", true), result(state, GERMANY, "A Pru R Ber", true)),
                lastResults(state));
    }

    @Test
    public void d6h14NoRetreatWithSupportInMovementPhase() {
        // A Par-Pic (2) dislodges A Pic; A Mar-Bur (2) dislodges A Bur. Both retreat to Bel: F Eng's support from
        // the orders phase does not help - both disbanded
        place(state, ENGLAND, "A Pic", "F Eng");
        place(state, FRANCE, "A Par", "A Bre", "A Bur");
        place(state, GERMANY, "A Mun", "A Mar");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "A Pic H", "F Eng S A Pic-Bel", "A Par-Pic", "A Bre S A Par-Pic", "A Bur H",
                "A Mun S A Mar-Bur", "A Mar-Bur");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        // A Pic: Bel (Bre and Bur occupied, Par the origin); A Bur: Bel, Gas, Par (vacated), Ruh
        assertEquals(orderSet(state, "A Pic R Bel", "Disband Pic"), legalSet(state, fm));
        play(state, fm, "A Pic R Bel", "A Bur R Bel");
        assertNull(state.getUnit(prov(state, "Bel")));
        assertEquals(Set.of(result(state, ENGLAND, "A Pic R Bel", false), result(state, FRANCE, "A Bur R Bel", false)),
                lastResults(state));
    }

    @Test
    public void d6h16StandoffOnOneCoastClosesTheWholeProvince() {
        // F Mid-Spa/nc and F Gas-Spa/nc stand off; F Tyn-Wes (2, with F Tun) dislodges F Wes, which may not
        // retreat to Spa/sc. F Wes: GoL, Mid (occupied), NAf, Spa/sc (standoff), Tun (occupied), Tyn (origin)
        place(state, FRANCE, "F Mid", "F Gas", "F Wes");
        place(state, ITALY, "F Tun", "F Tyn");
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
        play(state, fm, "F Mid-Spa/nc", "F Gas-Spa/nc", "F Wes H", "F Tun S F Tyn-Wes", "F Tyn-Wes");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertTrue(state.isStandoff(prov(state, "Spa")));
        assertEquals(orderSet(state, "F Wes R GoL", "F Wes R NAf", "Disband Wes"), legalSet(state, fm));
    }
}

package games.diplomacy;

import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Set;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * DATC v3.0 test cases 6.A (basic checks), 6.B (coastal issues), 6.C (circular movement) and 6.I (building) that
 * can arise from legal orders, with the DATC preferred results (4.B.4: support ignores coasts; 4.D: no build in an
 * occupied centre). Spring 1901 orders on a cleared board.
 * <p>
 * Not here, as they concern illegal or badly written orders that are never offered: 6.A.1-4, 6.A.6, 6.A.7, 6.A.9,
 * 6.A.10 (unreachable moves and supports - see DiplomacySetupAndMapTest and DiplomacySupportOrdersTest), the
 * illegal halves of 6.A.5 and 6.A.8; 6.B.1, 6.B.3 (a fleet move into Spain must name a reachable coast -
 * DiplomacySetupAndMapTest), 6.B.5 (support from a coast that cannot reach - DiplomacySupportOrdersTest), 6.B.9
 * (a support naming a coast: supports carry no coast), 6.B.10-12 (unit or army orders naming a coast), 6.B.14
 * (build F StP without a coast - DiplomacyYearTest's exact build set).
 */
public class DiplomacyDatcBasicTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    private void start() {
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
    }

    // ---------------------------------------------------------------- 6.A

    @Test
    public void a05LegalPartOfMoveToOwnSectorWithConvoy() {
        // England's illegal orders are not offered, so its units hold. F Lon-Yor 2 (A Wal) vs A Yor's hold 1:
        // dislodged. A Yor may retreat to Edi (Lvp and Wal occupied, Lon the attacker's origin)
        place(state, ENGLAND, "F Nth", "A Yor", "A Lvp");
        place(state, GERMANY, "F Lon", "A Wal");
        start();
        playExpecting(state, fm, "F Nth H: ok", "A Yor H: fails", "A Lvp H: ok",
                "F Lon-Yor: ok", "A Wal S F Lon-Yor: ok");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Yor")));
        assertEquals(army(ENGLAND), state.getDislodged(prov(state, "Yor")));
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
    }

    @Test
    public void a08UnitCannotSupportItselfToHold() {
        // F Tri S F Tri is not offered (only Hold): A Ven-Tri 2 (A Tyr) vs hold 1 dislodges F Tri, which may
        // retreat to Alb or Adr (Ven is the attacker's origin)
        place(state, ITALY, "A Ven", "A Tyr");
        place(state, AUSTRIA, "F Tri");
        start();
        assertFalse(fm.computeAvailableActions(state).contains(order(state, "F Tri S F Tri")));
        playExpecting(state, fm, "A Ven-Tri: ok", "A Tyr S A Ven-Tri: ok", "F Tri H: fails");
        assertEquals(army(ITALY), state.getUnit(prov(state, "Tri")));
        assertEquals(fleet(AUSTRIA), state.getDislodged(prov(state, "Tri")));
    }

    @Test
    public void a11SimpleBounce() {
        place(state, AUSTRIA, "A Vie");
        place(state, ITALY, "A Ven");
        start();
        playExpecting(state, fm, "A Vie-Tyr: fails", "A Ven-Tyr: fails");
        assertNull(state.getUnit(prov(state, "Tyr")));
        assertTrue(state.isStandoff(prov(state, "Tyr")));
    }

    @Test
    public void a12BounceOfThreeUnits() {
        // three moves of strength 1 into an empty province: all fail (not two bouncing and the third entering)
        place(state, AUSTRIA, "A Vie");
        place(state, GERMANY, "A Mun");
        place(state, ITALY, "A Ven");
        start();
        playExpecting(state, fm, "A Vie-Tyr: fails", "A Mun-Tyr: fails", "A Ven-Tyr: fails");
        assertNull(state.getUnit(prov(state, "Tyr")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Mun")));
    }

    // ---------------------------------------------------------------- 6.B

    @Test
    public void b02FleetIntoSpainFromGasconyIsOfferedOnlyToTheNorthCoast() {
        place(state, FRANCE, "F Gas");
        start();
        Set<AbstractAction> legal = legalSet(state, fm);
        assertTrue(legal.contains(order(state, "F Gas-Spa/nc")));
        assertFalse(legal.contains(order(state, "F Gas-Spa/sc")));   // 6.B.3
        assertFalse(legal.contains(order(state, "F Gas-Spa")));      // 6.B.2 with no coast
        play(state, fm, "F Gas-Spa/nc");
        assertEquals(fleet(FRANCE, "nc"), state.getUnit(prov(state, "Spa")));
    }

    @Test
    public void b04SupportToUnreachableCoastAllowed() {
        // F Mar cannot reach Spa/nc but supports into Spain (supports ignore coasts): F Gas-Spa/nc 2 vs
        // F Wes-Spa/sc 1 - the French fleet arrives on the north coast
        place(state, FRANCE, "F Gas", "F Mar");
        place(state, ITALY, "F Wes");
        start();
        playExpecting(state, fm, "F Gas-Spa/nc: ok", "F Mar S F Gas-Spa: ok", "F Wes-Spa/sc: fails");
        assertEquals(fleet(FRANCE, "nc"), state.getUnit(prov(state, "Spa")));
    }

    @Test
    public void b06SupportCanBeCutWithOtherCoast() {
        // F GoL-Spa/sc attacks the fleet on the north coast and cuts its support of F Mid: F NAt-Mid 2 (F Iri)
        // vs 1 dislodges F Mid (it may retreat, e.g. to Bre or Gas)
        place(state, ENGLAND, "F Iri", "F NAt");
        place(state, FRANCE, "F Spa/nc", "F Mid");
        place(state, ITALY, "F GoL");
        start();
        playExpecting(state, fm, "F Iri S F NAt-Mid: ok", "F NAt-Mid: ok", "F Spa/nc S F Mid: fails",
                "F Mid H: fails", "F GoL-Spa/sc: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Mid")));
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Mid")));
        assertEquals(fleet(FRANCE, "nc"), state.getUnit(prov(state, "Spa")));
    }

    @Test
    public void b07SupportingOwnUnitWithUnspecifiedCoast() {
        // F Por's support (no coast) counts for F Mid-Spa/nc: 2 vs F Wes-Spa/sc 2 (F GoL) - both fail
        place(state, FRANCE, "F Por", "F Mid");
        place(state, ITALY, "F GoL", "F Wes");
        start();
        playExpecting(state, fm, "F Por S F Mid-Spa: ok", "F Mid-Spa/nc: fails",
                "F GoL S F Wes-Spa: ok", "F Wes-Spa/sc: fails");
        assertNull(state.getUnit(prov(state, "Spa")));
    }

    @Test
    public void b08SupportingWithUnspecifiedCoastWhenOnlyOneCoastIsPossible() {
        // as 6.B.7 with F Gas, which can only reach the north coast
        place(state, FRANCE, "F Por", "F Gas");
        place(state, ITALY, "F GoL", "F Wes");
        start();
        playExpecting(state, fm, "F Por S F Gas-Spa: ok", "F Gas-Spa/nc: fails",
                "F GoL S F Wes-Spa: ok", "F Wes-Spa/sc: fails");
        assertNull(state.getUnit(prov(state, "Spa")));
    }

    @Test
    public void b13CoastalCrawlNotAllowed() {
        // F Bul/sc-Con and F Con-Bul/ec are a head-to-head battle whatever the coasts: both fail
        place(state, TURKEY, "F Bul/sc", "F Con");
        start();
        playExpecting(state, fm, "F Bul/sc-Con: fails", "F Con-Bul/ec: fails");
        assertEquals(fleet(TURKEY, "sc"), state.getUnit(prov(state, "Bul")));
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Con")));
    }

    @Test
    public void b15SupportingForeignUnitWithUnspecifiedCoast() {
        // French F Por's support counts for the English F Mid-Spa/nc: 2 vs 2, both fail
        place(state, FRANCE, "F Por");
        place(state, ENGLAND, "F Mid");
        place(state, ITALY, "F GoL", "F Wes");
        start();
        playExpecting(state, fm, "F Por S F Mid-Spa: ok", "F Mid-Spa/nc: fails",
                "F GoL S F Wes-Spa: ok", "F Wes-Spa/sc: fails");
        assertNull(state.getUnit(prov(state, "Spa")));
    }

    // ---------------------------------------------------------------- 6.C

    @Test
    public void c01ThreeArmyCircularMovement() {
        place(state, TURKEY, "F Ank", "A Con", "A Smy");
        start();
        playExpecting(state, fm, "F Ank-Con: ok", "A Con-Smy: ok", "A Smy-Ank: ok");
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Con")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Smy")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Ank")));
    }

    @Test
    public void c02ThreeArmyCircularMovementWithSupport() {
        place(state, TURKEY, "F Ank", "A Con", "A Smy", "A Bul");
        start();
        playExpecting(state, fm, "F Ank-Con: ok", "A Con-Smy: ok", "A Smy-Ank: ok", "A Bul S F Ank-Con: ok");
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Con")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Ank")));
    }

    @Test
    public void c03DisruptedThreeArmyCircularMovement() {
        // F Ank-Con 1 and A Bul-Con 1 stand off, so the rotation is broken: nothing moves
        place(state, TURKEY, "F Ank", "A Con", "A Smy", "A Bul");
        start();
        playExpecting(state, fm, "F Ank-Con: fails", "A Con-Smy: fails", "A Smy-Ank: fails", "A Bul-Con: fails");
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Ank")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Con")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Bul")));
    }

    @Test
    public void c04CircularMovementWithAttackedConvoy() {
        // F Nap-Ion 1 vs hold 1 fails: the convoy Aeg-Ion-Adr holds, the cycle Tri-Ser-Bul-Tri has only moves in
        // it (the convoys are not part of it) and all three move
        place(state, AUSTRIA, "A Tri", "A Ser");
        place(state, TURKEY, "A Bul", "F Aeg", "F Ion", "F Adr");
        place(state, ITALY, "F Nap");
        start();
        playExpecting(state, fm, "A Tri-Ser: ok", "A Ser-Bul: ok", "A Bul-Tri via convoy: ok",
                "F Aeg C A Bul-Tri: ok", "F Ion C A Bul-Tri: ok", "F Adr C A Bul-Tri: ok", "F Nap-Ion: fails");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Ser")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Bul")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Tri")));
    }

    @Test
    public void c05DisruptedCircularMovementDueToDislodgedConvoy() {
        // F Nap-Ion 2 (F Tun) dislodges F Ion, the only route is broken, A Bul stays and the cycle fails
        place(state, AUSTRIA, "A Tri", "A Ser");
        place(state, TURKEY, "A Bul", "F Aeg", "F Ion", "F Adr");
        place(state, ITALY, "F Nap", "F Tun");
        start();
        playExpecting(state, fm, "A Tri-Ser: fails", "A Ser-Bul: fails", "A Bul-Tri via convoy: fails",
                "F Aeg C A Bul-Tri: ok", "F Ion C A Bul-Tri: fails", "F Adr C A Bul-Tri: ok",
                "F Nap-Ion: ok", "F Tun S F Nap-Ion: ok");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Tri")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Ser")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Bul")));
        assertEquals(fleet(ITALY), state.getUnit(prov(state, "Ion")));
    }

    @Test
    public void c06TwoArmiesWithTwoConvoysSwap() {
        // a cycle of two convoyed moves (no head-to-head between convoyed armies): circular movement, both move
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, FRANCE, "F Eng", "A Bel");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Bel: ok", "A Lon-Bel via convoy: ok",
                "F Eng C A Bel-Lon: ok", "A Bel-Lon via convoy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Lon")));
    }

    @Test
    public void c07DisruptedUnitSwap() {
        // A Lon-Bel 1 and A Bur-Bel 1 stand off, so A Bel-Lon meets A Lon still there (1 vs 1): nothing moves
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, FRANCE, "F Eng", "A Bel", "A Bur");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Bel: ok", "A Lon-Bel via convoy: fails",
                "F Eng C A Bel-Lon: ok", "A Bel-Lon via convoy: fails", "A Bur-Bel: fails");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Lon")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bel")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
    }

    @Test
    public void c08NoSelfDislodgementInDisruptedCircularMovement() {
        // F Bla-Bul/ec 1 and A Ser-Bul 1 stand off; F Bla stays, so F Con-Bla (1 vs 1) fails and F Con stays;
        // A Bul-Con 2 (A Smy) may not dislodge the Turkish F Con: nothing moves
        place(state, TURKEY, "F Con", "A Bul", "A Smy");
        place(state, RUSSIA, "F Bla");
        place(state, AUSTRIA, "A Ser");
        start();
        playExpecting(state, fm, "F Con-Bla: fails", "A Bul-Con: fails", "A Smy S A Bul-Con: ok",
                "F Bla-Bul/ec: fails", "A Ser-Bul: fails");
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Con")));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Bul")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Bla")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void c09NoHelpInDislodgementOfOwnUnitInDisruptedCircularMovement() {
        // Austrian A Bul-Con: 1 (Turkey's support does not count against Turkey's F Con) vs 1 fails; then A Bul
        // stays, F Bla-Bul/ec and A Ser-Bul fail, F Bla stays and F Con-Bla fails: nothing moves
        place(state, TURKEY, "F Con", "A Smy");
        place(state, RUSSIA, "F Bla");
        place(state, AUSTRIA, "A Ser", "A Bul");
        start();
        playExpecting(state, fm, "F Con-Bla: fails", "A Smy S A Bul-Con: ok", "F Bla-Bul/ec: fails",
                "A Ser-Bul: fails", "A Bul-Con: fails");
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Con")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Bul")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Bla")));
        assertEquals(0, totalDislodged(state));
    }

    // ---------------------------------------------------------------- 6.I

    @Test
    public void i01NoMoreBuildsAreOfferedThanThePowerIsEntitledTo() {
        // Germany: Ber, Kie, Mun, Hol, Den = 5 centres, 4 units -> 1 build (free Ber, Kie, Mun); after it the
        // power has nothing more to order
        own(state, GERMANY, "Hol", "Den");
        place(state, GERMANY, "A Hol", "A Den", "A Bel", "A Ruh");
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, GERMANY);
        assertEquals(1, state.adjustment(GERMANY));
        assertEquals(orderSet(state, "Build A Ber", "Build F Ber", "Build A Kie", "Build F Kie", "Build A Mun", "Waive Germany"),
                legalSet(state, fm));
        fm.next(state, order(state, "Build A Kie"));
        assertFalse(state.hasOrdersToGive(GERMANY));
        assertNotEquals(GERMANY, state.getCurrentPlayer());
        assertEquals(List.of(order(state, "Build A Kie")), state.getOrders(GERMANY));
    }

    @Test
    public void i03i05i06BuildsOnlyInEmptyOwnedHomeCentres() {
        // Germany owns Kie, Mun (occupied: 6.I.3), War (not a home centre: 6.I.6) = 3 centres, 1 unit -> 2 builds;
        // Ber is Russia's (6.I.5). Only Kie is offered
        own(state, RUSSIA, "Ber");
        own(state, GERMANY, "War");
        place(state, GERMANY, "A Mun");
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, GERMANY);
        assertEquals(2, state.adjustment(GERMANY));
        assertEquals(orderSet(state, "Build A Kie", "Build F Kie", "Waive Germany"), legalSet(state, fm));
    }

    @Test
    public void i02i04i07NoFleetInlandNoBuildBesideAUnitOnTheOtherCoastOneBuildPerCentre() {
        // Russia: Mos, Sev, StP, War = 4 centres, F StP/sc -> 3 builds. Moscow: army only (6.I.2); StP occupied
        // on its south coast, so no build there on either coast (6.I.4)
        place(state, RUSSIA, "F StP/sc");
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, RUSSIA);
        assertEquals(3, state.adjustment(RUSSIA));
        assertEquals(orderSet(state, "Build A Mos", "Build A Sev", "Build F Sev", "Build A War", "Waive Russia"),
                legalSet(state, fm));
        // after a build in Moscow it is not offered again (6.I.7)
        fm.next(state, order(state, "Build A Mos"));
        assertEquals(RUSSIA, state.getCurrentPlayer());
        assertEquals(orderSet(state, "Build A Sev", "Build F Sev", "Build A War", "Waive Russia"),
                legalSet(state, fm));
    }
}

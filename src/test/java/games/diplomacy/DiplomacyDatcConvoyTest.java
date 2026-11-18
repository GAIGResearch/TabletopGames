package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * DATC v3.0 test cases with convoys: 6.F (convoys), 6.G (convoying to adjacent provinces), and the convoy cases of
 * 6.D/6.E (6.D.6, 6.D.8, 6.D.16, 6.D.27, 6.E.11), with the DATC preferred results under the choices made: 4.A.1 b
 * (a convoy fails only when every route is broken), the 2000 paradox rules 21/22, 4.A.3 (the convoy route only
 * if the move is via convoy or an own fleet convoys it, no fallback to land), 4.A.4 a (a convoyed attack comes
 * from the army's province), 4.A.7 b (a dislodged unit still prevents unless it lost a head-to-head battle).
 * Moves to provinces not adjacent by land are written "via convoy" (the only way they are offered).
 * <p>
 * Not here: 6.F.1 (the convoy order of a coastal fleet is never offered - see DiplomacyConvoyOrdersTest),
 * 6.F.12 (F Iri is on no convoy route, so its order is never offered - also there), 6.G.7 (an impossible convoy
 * order); 6.F.17, 6.F.18, 6.F.22-24 (paradoxes the 2000 rules leave open or where Szykman differs:
 * DiplomacyParadoxTest).
 * Spring 1901 orders on a cleared board; a convoy order "ok" means its fleet was not dislodged.
 */
public class DiplomacyDatcConvoyTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState(helpingAnyUnit());
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    private void start() {
        startOrders(state, DiplomacyPhase.SPRING_ORDERS);
    }

    // ---- 6.D / 6.E convoy cases ----

    @Test
    public void d06SupportToHoldOnConvoyingUnitAllowed() {
        // F Bal's hold 1 + F Pru = 2 vs F Lvn-Bal 2: not dislodged, so A Ber-Swe arrives
        place(state, GERMANY, "A Ber", "F Bal", "F Pru");
        place(state, RUSSIA, "F Lvn", "F Bot");
        start();
        playExpecting(state, fm, "A Ber-Swe via convoy: ok", "F Bal C A Ber-Swe: ok", "F Pru S F Bal: ok",
                "F Lvn-Bal: fails", "F Bot S F Lvn-Bal: ok");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Swe")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Bal")));
    }

    @Test
    public void d08FailedConvoyCannotReceiveHoldSupport() {
        // A Gre-Nap via convoy has no convoy order (F Ion holds): it fails, but it was a move, so A Bul's hold
        // support does not count. A Alb-Gre 2 (A Ser) vs hold 1: A Gre dislodged
        place(state, AUSTRIA, "F Ion", "A Ser", "A Alb");
        place(state, TURKEY, "A Gre", "A Bul");
        start();
        playExpecting(state, fm, "F Ion H: ok", "A Ser S A Alb-Gre: ok", "A Alb-Gre: ok",
                "A Gre-Nap via convoy: fails", "A Bul S A Gre: fails");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Gre")));
        // A Gre cannot retreat (Bul and Ser occupied, Alb its attacker's origin): disbanded at once
        assertNull(state.getDislodged(prov(state, "Gre")));
        assertEquals(1, state.nUnits(TURKEY));
    }

    @Test
    public void d16ConvoyingAUnitDislodgingAUnitOfSamePowerIsAllowed() {
        // England's F Nth convoys France's A Bel-Lon: 2 (F Eng) vs A Lon's hold 1 - the English army is dislodged
        place(state, ENGLAND, "A Lon", "F Nth");
        place(state, FRANCE, "F Eng", "A Bel");
        start();
        playExpecting(state, fm, "A Lon H: fails", "F Nth C A Bel-Lon: ok", "F Eng S A Bel-Lon: ok",
                "A Bel-Lon via convoy: ok");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Lon")));
        assertEquals(army(ENGLAND), state.getDislodged(prov(state, "Lon")));
    }

    @Test
    public void d27FailingConvoyCanBeSupported() {
        // F Bal's convoy matches no move (A Ber holds), but it may be supported to hold: 2 vs F Swe-Bal 2
        place(state, ENGLAND, "F Swe", "F Den");
        place(state, GERMANY, "A Ber");
        place(state, RUSSIA, "F Bal", "F Pru");
        start();
        playExpecting(state, fm, "F Swe-Bal: fails", "F Den S F Swe-Bal: ok", "A Ber H: ok", "F Bal C A Ber-Lvn: ok",
                "F Pru S F Bal: ok");
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Bal")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void e11NoSelfDislodgementWithBeleagueredGarrisonUnitSwapWithAdjacentConvoyingAndTwoCoasts() {
        // A Spa-Por via convoy (F Mid) is convoyed, so F Por-Spa/nc is no head-to-head battle: if A Spa leaves,
        // F Por-Spa has 3 (F GoL, F Wes) vs A Gas-Spa's 2 (A Mar) and wins, and Por is vacated for A Spa. Both
        // resolutions are consistent - circular movement: the swap succeeds
        place(state, FRANCE, "A Spa", "F Mid", "F GoL");
        place(state, GERMANY, "A Mar", "A Gas");
        place(state, ITALY, "F Por", "F Wes");
        start();
        playExpecting(state, fm, "A Spa-Por via convoy: ok", "F Mid C A Spa-Por: ok", "F GoL S F Por-Spa/nc " + NOT_OFFERED + ": ok",
                "A Mar S A Gas-Spa: ok", "A Gas-Spa: fails", "F Por-Spa/nc: ok", "F Wes S F Por-Spa/nc: ok");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Por")));
        assertEquals(fleet(ITALY, "nc"), state.getUnit(prov(state, "Spa")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Gas")));
        assertEquals(0, totalDislodged(state));
    }

    // ---- 6.F convoys ----

    @Test
    public void f02AnArmyBeingConvoyedCanBounceAsNormal() {
        // 1 vs 1 at Bre: a standoff, both stay
        place(state, ENGLAND, "F Eng", "A Lon");
        place(state, FRANCE, "A Par");
        start();
        playExpecting(state, fm, "F Eng C A Lon-Bre: ok", "A Lon-Bre via convoy: fails", "A Par-Bre: fails");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Lon")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Par")));
        assertTrue(state.isStandoff(prov(state, "Bre")));
    }

    @Test
    public void f03AnArmyBeingConvoyedCanReceiveSupport() {
        // A Lon-Bre 2 (F Mid) beats A Par-Bre 1
        place(state, ENGLAND, "F Eng", "A Lon", "F Mid");
        place(state, FRANCE, "A Par");
        start();
        playExpecting(state, fm, "F Eng C A Lon-Bre: ok", "A Lon-Bre via convoy: ok", "F Mid S A Lon-Bre: ok",
                "A Par-Bre: fails");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bre")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Par")));
    }

    @Test
    public void f04AnAttackedConvoyIsNotDisrupted() {
        // F Ska-Nth 1 vs hold 1 fails: F Nth is not dislodged
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, GERMANY, "F Ska");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Hol: ok", "A Lon-Hol via convoy: ok", "F Ska-Nth: fails");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Hol")));
    }

    @Test
    public void f05ABeleagueredConvoyIsNotDisrupted() {
        // F Eng-Nth 2 and F Ska-Nth 2 stand off; F Nth is not dislodged
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, FRANCE, "F Eng", "F Bel");
        place(state, GERMANY, "F Ska", "F Den");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Hol: ok", "A Lon-Hol via convoy: ok", "F Eng-Nth: fails",
                "F Bel S F Eng-Nth: ok", "F Ska-Nth: fails", "F Den S F Ska-Nth: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Hol")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
    }

    @Test
    public void f06DislodgedConvoyDoesNotCutSupport() {
        // F Ska-Nth 2 (F Hel) dislodges F Nth: A Lon-Hol does not happen, so A Hol's support of A Bel stands.
        // A Bel's support of A Hol is cut by A Pic-Bel. A Pic-Bel 2 (A Bur) vs A Bel's hold 1 + A Hol = 2: fails
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, GERMANY, "A Hol", "A Bel", "F Hel", "F Ska");
        place(state, FRANCE, "A Pic", "A Bur");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Hol: fails", "A Lon-Hol via convoy: fails", "A Hol S A Bel: ok",
                "A Bel S A Hol: fails", "F Hel S F Ska-Nth: ok", "F Ska-Nth: ok", "A Pic-Bel: fails",
                "A Bur S A Pic-Bel: ok");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Bel")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Lon")));
    }

    @Test
    public void f07DislodgedConvoyDoesNotCauseContestedArea() {
        // F Nth dislodged by F Ska (2 vs 1); A Lon never reached Hol, so Hol is no standoff and F Nth may retreat
        // there. F Nth: Bel, Den, Edi, Eng, Hol, Nrg, Nwy, Yor (Hel and Lon occupied, Ska the attacker's origin)
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, GERMANY, "F Hel", "F Ska");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Hol: fails", "A Lon-Hol via convoy: fails", "F Hel S F Ska-Nth: ok",
                "F Ska-Nth: ok");
        assertEquals(DiplomacyPhase.SPRING_RETREATS, state.getPhase());
        assertFalse(state.isStandoff(prov(state, "Hol")));
        assertEquals(orderSet(state, "F Nth R Bel", "F Nth R Den", "F Nth R Edi", "F Nth R Eng", "F Nth R Hol",
                "F Nth R Nrg", "F Nth R Nwy", "F Nth R Yor", "Disband Nth"), legalSet(state, fm));
        play(state, fm, "F Nth R Hol");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Hol")));
    }

    @Test
    public void f08DislodgedConvoyDoesNotCauseABounce() {
        // the disrupted A Lon-Hol has no prevent strength: A Bel-Hol enters Hol
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, GERMANY, "F Hel", "F Ska", "A Bel");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Hol: fails", "A Lon-Hol via convoy: fails", "F Hel S F Ska-Nth: ok",
                "F Ska-Nth: ok", "A Bel-Hol: ok");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Hol")));
    }

    @Test
    public void f09DislodgeOfMultiRouteConvoy() {
        // 4.A.1 b: F Eng dislodged (2 vs 1), the route via F Nth remains
        place(state, ENGLAND, "F Eng", "F Nth", "A Lon");
        place(state, FRANCE, "F Bre", "F Mid");
        start();
        playExpecting(state, fm, "F Eng C A Lon-Bel: fails", "F Nth C A Lon-Bel: ok", "A Lon-Bel via convoy: ok",
                "F Bre S F Mid-Eng: ok", "F Mid-Eng: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
    }

    @Test
    public void f10DislodgeOfMultiRouteConvoyWithForeignFleet() {
        place(state, ENGLAND, "F Nth", "A Lon");
        place(state, GERMANY, "F Eng");
        place(state, FRANCE, "F Bre", "F Mid");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Bel: ok", "A Lon-Bel via convoy: ok", "F Eng C A Lon-Bel: fails",
                "F Bre S F Mid-Eng: ok", "F Mid-Eng: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
    }

    @Test
    public void f11DislodgeOfMultiRouteConvoyWithOnlyForeignFleets() {
        place(state, ENGLAND, "A Lon");
        place(state, GERMANY, "F Eng");
        place(state, RUSSIA, "F Nth");
        place(state, FRANCE, "F Bre", "F Mid");
        start();
        playExpecting(state, fm, "A Lon-Bel via convoy: ok", "F Eng C A Lon-Bel: fails", "F Nth C A Lon-Bel: ok",
                "F Bre S F Mid-Eng: ok", "F Mid-Eng: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
    }

    @Test
    public void f13TheUnwantedAlternative() {
        // F Den-Nth 2 (F Hol) dislodges F Nth; France's F Eng still carries A Lon to Bel
        place(state, ENGLAND, "A Lon", "F Nth");
        place(state, FRANCE, "F Eng");
        place(state, GERMANY, "F Hol", "F Den");
        start();
        playExpecting(state, fm, "A Lon-Bel via convoy: ok", "F Nth C A Lon-Bel: fails", "F Eng C A Lon-Bel: ok",
                "F Hol S F Den-Nth: ok", "F Den-Nth: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(fleet(ENGLAND), state.getDislodged(prov(state, "Nth")));
    }

    @Test
    public void f14SimpleConvoyParadox() {
        // rule 21: A Bre-Lon does not cut F Lon's support of the attack on its only convoying fleet; F Wal-Eng
        // 2 vs 1 dislodges F Eng and the convoy fails
        place(state, ENGLAND, "F Lon", "F Wal");
        place(state, FRANCE, "A Bre", "F Eng");
        start();
        playExpecting(state, fm, "F Lon S F Wal-Eng: ok", "F Wal-Eng: ok", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Eng")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bre")));
    }

    @Test
    public void f15SimpleConvoyParadoxWithAdditionalConvoy() {
        // as 6.F.14; F Wal leaves, so Italy's A NAf-Wal (via Mid, Iri) enters the vacated Wal
        place(state, ENGLAND, "F Lon", "F Wal");
        place(state, FRANCE, "A Bre", "F Eng");
        place(state, ITALY, "F Iri", "F Mid", "A NAf");
        start();
        playExpecting(state, fm, "F Lon S F Wal-Eng: ok", "F Wal-Eng: ok", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: fails", "F Iri C A NAf-Wal: ok", "F Mid C A NAf-Wal: ok",
                "A NAf-Wal via convoy: ok");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Eng")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Wal")));
    }

    @Test
    public void f16PandinsParadox() {
        // rule 21: F Lon's support is not cut. F Wal-Eng 2 and F Bel-Eng 2 (F Nth) stand off against each other,
        // F Eng survives, but A Bre-Lon 1 vs F Lon's hold 1 fails: nothing moves
        place(state, ENGLAND, "F Lon", "F Wal");
        place(state, FRANCE, "A Bre", "F Eng");
        place(state, GERMANY, "F Nth", "F Bel");
        start();
        playExpecting(state, fm, "F Lon S F Wal-Eng: ok", "F Wal-Eng: fails", "A Bre-Lon via convoy: fails",
                "F Eng C A Bre-Lon: ok", "F Nth S F Bel-Eng: ok", "F Bel-Eng: fails");
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Eng")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Lon")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void f19MultiRouteConvoyDisruptionParadox() {
        // rule 22: F Tyn is not necessary (route via F Ion), so A Tun-Nap cuts F Nap's support; F Rom-Tyn 1 vs 1
        // fails; A Tun-Nap 1 vs 1 fails
        place(state, FRANCE, "A Tun", "F Tyn", "F Ion");
        place(state, ITALY, "F Nap", "F Rom");
        start();
        playExpecting(state, fm, "A Tun-Nap via convoy: fails", "F Tyn C A Tun-Nap: ok", "F Ion C A Tun-Nap: ok",
                "F Nap S F Rom-Tyn: fails", "F Rom-Tyn: fails");
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Tyn")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void f20UnwantedMultiRouteConvoyParadox() {
        // F Nap supports a hold (no attack on a convoying fleet): rule 21 does not apply, and A Tun-Nap (route via
        // F Tyn intact) cuts it. F Eas-Ion 2 (F Aeg) vs 1 dislodges F Ion. A Tun-Nap 1 vs F Nap's hold 1 fails
        place(state, FRANCE, "A Tun", "F Tyn");
        place(state, ITALY, "F Nap", "F Ion");
        place(state, TURKEY, "F Aeg", "F Eas");
        start();
        playExpecting(state, fm, "A Tun-Nap via convoy: fails", "F Tyn C A Tun-Nap: ok", "F Nap S F Ion: fails",
                "F Ion C A Tun-Nap: fails", "F Aeg S F Eas-Ion: ok", "F Eas-Ion: ok");
        assertEquals(fleet(ITALY), state.getDislodged(prov(state, "Ion")));
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Ion")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Tun")));
    }

    @Test
    public void f21DadsArmyConvoy() {
        // A Nwy-Cly (via F Nrg, not attacked) cuts F Cly's support of F NAt; F Mid-NAt 2 (F Iri) vs 1 dislodges
        // F NAt, so England's A Lvp-Cly convoy fails (attack and prevent 0). A Nwy-Cly 2 (A Edi) vs F Cly's hold 1:
        // F Cly is dislodged too
        place(state, RUSSIA, "A Edi", "F Nrg", "A Nwy");
        place(state, FRANCE, "F Iri", "F Mid");
        place(state, ENGLAND, "A Lvp", "F NAt", "F Cly");
        start();
        playExpecting(state, fm, "A Edi S A Nwy-Cly: ok", "F Nrg C A Nwy-Cly: ok", "A Nwy-Cly via convoy: ok",
                "F Iri S F Mid-NAt: ok", "F Mid-NAt: ok", "A Lvp-Cly via convoy: fails", "F NAt C A Lvp-Cly: fails",
                "F Cly S F NAt: fails");
        // neither can retreat - F NAt: Cly, Iri, Lvp and Nrg occupied, Mid its attacker's origin; F Cly: Edi, Lvp,
        // NAt and Nrg occupied - so both are disbanded at once
        assertNull(state.getDislodged(prov(state, "NAt")));
        assertNull(state.getDislodged(prov(state, "Cly")));
        assertEquals(1, state.nUnits(ENGLAND));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Cly")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Lvp")));
    }

    @Test
    public void f25CutSupportLast() {
        // F Swe-Ska 1 vs 1 fails: A Den-Nwy 2 (A Fin) dislodges F Nwy, cutting its support; F Nrg-Nth 1 vs 1
        // fails: A Yor-Hol 2 (F Hel) dislodges A Hol, cutting its support; A Ruh-Bel 1 vs A Bel's hold 1 fails
        place(state, GERMANY, "A Ruh", "A Hol", "A Den", "F Ska", "A Fin");
        place(state, ENGLAND, "A Yor", "F Nth", "F Hel", "A Bel");
        place(state, RUSSIA, "F Nrg", "F Nwy", "F Swe");
        start();
        playExpecting(state, fm, "A Ruh-Bel: fails", "A Hol S A Ruh-Bel: fails", "A Den-Nwy via convoy: ok",
                "F Ska C A Den-Nwy: ok", "A Fin S A Den-Nwy: ok", "A Yor-Hol via convoy: ok", "F Nth C A Yor-Hol: ok",
                "F Hel S A Yor-Hol: ok", "A Bel H: ok", "F Nrg-Nth: fails", "F Nwy S F Nrg-Nth: fails",
                "F Swe-Ska: fails");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Nwy")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Hol")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(2, totalDislodged(state));
    }

    // ---- 6.G convoying to adjacent provinces ----

    @Test
    public void g01TwoUnitsCanSwapProvincesByConvoy() {
        // England's own F Ska convoys A Nwy-Swe (not marked via convoy): convoyed, so no head-to-head - a swap
        place(state, ENGLAND, "A Nwy", "F Ska");
        place(state, RUSSIA, "A Swe");
        start();
        playExpecting(state, fm, "A Nwy-Swe: ok", "F Ska C A Nwy-Swe: ok", "A Swe-Nwy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Swe")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Nwy")));
    }

    @Test
    public void g02KidnappingAnArmy() {
        // only a German fleet convoys: land route, head-to-head 1 vs 1 - neither moves
        place(state, ENGLAND, "A Nwy");
        place(state, RUSSIA, "F Swe");
        place(state, GERMANY, "F Ska");
        start();
        playExpecting(state, fm, "A Nwy-Swe: fails", "F Swe-Nwy: fails", "F Ska C A Nwy-Swe: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Nwy")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Swe")));
    }

    @Test
    public void g03AnUnwantedDisruptedConvoyToAdjacentProvince() {
        // England's convoy of a French army: land route, A Pic-Bel enters the empty Bel; F Eng is dislodged
        place(state, FRANCE, "F Bre", "A Pic", "A Bur", "F Mid");
        place(state, ENGLAND, "F Eng");
        start();
        playExpecting(state, fm, "F Bre-Eng: ok", "A Pic-Bel: ok", "A Bur S A Pic-Bel: ok", "F Mid S F Bre-Eng: ok",
                "F Eng C A Pic-Bel: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bel")));
    }

    @Test
    public void g04AnUnwantedDisruptedConvoyToAdjacentProvinceAndOppositeMove() {
        // land route: head-to-head A Pic-Bel 2 (A Bur) vs A Bel-Pic 1 - A Bel dislodged
        place(state, FRANCE, "F Bre", "A Pic", "A Bur", "F Mid");
        place(state, ENGLAND, "F Eng", "A Bel");
        start();
        playExpecting(state, fm, "F Bre-Eng: ok", "A Pic-Bel: ok", "A Bur S A Pic-Bel: ok", "F Mid S F Bre-Eng: ok",
                "F Eng C A Pic-Bel: fails", "A Bel-Pic: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bel")));
        assertEquals(army(ENGLAND), state.getDislodged(prov(state, "Bel")));
    }

    @Test
    public void g05SwappingWithMultipleFleetsWithOneOwnFleet() {
        // Turkey's own F Ion convoys A Apu-Rom (route Apu-Ion-Tyn-Rom): convoyed, the armies swap
        place(state, ITALY, "A Rom", "F Tyn");
        place(state, TURKEY, "A Apu", "F Ion");
        start();
        playExpecting(state, fm, "A Rom-Apu: ok", "F Tyn C A Apu-Rom: ok", "A Apu-Rom: ok", "F Ion C A Apu-Rom: ok");
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Rom")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Apu")));
    }

    @Test
    public void g06SwappingWithUnintendedIntent() {
        // England's F Eng convoys A Lvp-Edi, so the move is convoyed; the Russian F NAt, F Nrg give a route
        // (Lvp-NAt-Nrg-Edi): the armies swap
        place(state, ENGLAND, "A Lvp", "F Eng");
        place(state, GERMANY, "A Edi");
        place(state, FRANCE, "F Iri", "F Nth");
        place(state, RUSSIA, "F Nrg", "F NAt");
        start();
        playExpecting(state, fm, "A Lvp-Edi: ok", "F Eng C A Lvp-Edi: ok", "A Edi-Lvp: ok", "F Iri H: ok",
                "F Nth H: ok", "F Nrg C A Lvp-Edi: ok", "F NAt C A Lvp-Edi: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Edi")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Lvp")));
    }

    @Test
    public void g08ExplicitConvoyThatIsNotThere() {
        // via convoy with no convoy order: no fallback to land, A Bel stays
        place(state, FRANCE, "A Bel");
        place(state, ENGLAND, "F Nth", "A Hol");
        start();
        playExpecting(state, fm, "A Bel-Hol via convoy: fails", "F Nth-Hel: ok", "A Hol-Kie: ok");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bel")));
        assertNull(state.getUnit(prov(state, "Hol")));
    }

    @Test
    public void g09SwappedOrDislodged() {
        // own F Ska convoys: A Nwy-Swe is convoyed, so the armies swap rather than A Swe being dislodged
        place(state, ENGLAND, "A Nwy", "F Ska", "F Fin");
        place(state, RUSSIA, "A Swe");
        start();
        playExpecting(state, fm, "A Nwy-Swe: ok", "F Ska C A Nwy-Swe: ok", "F Fin S A Nwy-Swe: ok", "A Swe-Nwy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Swe")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Nwy")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void g10SwappedOrAHeadToHeadBattle() {
        // A Nwy-Swe via convoy (German F Ska) 3 dislodges A Swe (hold 1). A Swe-Nwy 2 (F Bar) vs F Nrg-Nwy 2
        // (F Nth): both fail - 4.A.7 b: A Swe, dislodged but not in a head-to-head battle, still prevents
        place(state, ENGLAND, "A Nwy", "F Den", "F Fin");
        place(state, GERMANY, "F Ska");
        place(state, RUSSIA, "A Swe", "F Bar");
        place(state, FRANCE, "F Nrg", "F Nth");
        start();
        playExpecting(state, fm, "A Nwy-Swe via convoy: ok", "F Den S A Nwy-Swe: ok", "F Fin S A Nwy-Swe: ok",
                "F Ska C A Nwy-Swe: ok", "A Swe-Nwy: fails", "F Bar S A Swe-Nwy: ok", "F Nrg-Nwy: fails",
                "F Nth S F Nrg-Nwy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Swe")));
        // A Swe cannot retreat (Den and Fin occupied, Nwy a standoff): disbanded at once
        assertNull(state.getDislodged(prov(state, "Swe")));
        assertEquals(1, state.nUnits(RUSSIA));
        assertNull(state.getUnit(prov(state, "Nwy")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Nrg")));
    }

    @Test
    public void g11AConvoyToAnAdjacentProvinceWithAParadox() {
        // own F Ska convoys: A Swe-Nwy is convoyed (no fallback). Rule 21: it does not cut F Nwy's support of the
        // attack on F Ska; F Nth-Ska 2 vs 1 dislodges F Ska and the army does not move
        place(state, ENGLAND, "F Nwy", "F Nth");
        place(state, RUSSIA, "A Swe", "F Ska", "F Bar");
        start();
        playExpecting(state, fm, "F Nwy S F Nth-Ska: ok", "F Nth-Ska: ok", "A Swe-Nwy: fails",
                "F Ska C A Swe-Nwy: fails", "F Bar S A Swe-Nwy: ok");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nwy")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Ska")));
        assertEquals(fleet(RUSSIA), state.getDislodged(prov(state, "Ska")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Swe")));
    }

    @Test
    public void g12SwappingTwoUnitsWithTwoConvoys() {
        place(state, ENGLAND, "A Lvp", "F NAt", "F Nrg");
        place(state, GERMANY, "A Edi", "F Nth", "F Eng", "F Iri");
        start();
        playExpecting(state, fm, "A Lvp-Edi via convoy: ok", "F NAt C A Lvp-Edi: ok", "F Nrg C A Lvp-Edi: ok",
                "A Edi-Lvp via convoy: ok", "F Nth C A Edi-Lvp: ok", "F Eng C A Edi-Lvp: ok", "F Iri C A Edi-Lvp: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Edi")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Lvp")));
    }

    @Test
    public void g13SupportCutOnAttackOnItselfViaConvoy() {
        // 4.A.4 a: the convoyed attack comes from Tri, so it does not cut A Ven's support into Tri. F Alb-Tri 2
        // vs A Tri's hold 1 (its move failed: 1 vs A Ven's hold 1)
        place(state, AUSTRIA, "F Adr", "A Tri");
        place(state, ITALY, "A Ven", "F Alb");
        start();
        playExpecting(state, fm, "F Adr C A Tri-Ven: ok", "A Tri-Ven via convoy: fails", "A Ven S F Alb-Tri: ok",
                "F Alb-Tri: ok");
        assertEquals(fleet(ITALY), state.getUnit(prov(state, "Tri")));
        assertEquals(army(AUSTRIA), state.getDislodged(prov(state, "Tri")));
    }

    @Test
    public void g14BounceByConvoyToAdjacentProvince() {
        // A Nwy-Swe by land 3 dislodges A Swe (hold 1: its convoyed move fails, 2 vs F Nrg-Nwy 2). Not a
        // head-to-head battle (A Swe is convoyed), so A Swe still prevents F Nrg-Nwy (4.A.7 b)
        place(state, ENGLAND, "A Nwy", "F Den", "F Fin");
        place(state, FRANCE, "F Nrg", "F Nth");
        place(state, GERMANY, "F Ska");
        place(state, RUSSIA, "A Swe", "F Bar");
        start();
        playExpecting(state, fm, "A Nwy-Swe: ok", "F Den S A Nwy-Swe: ok", "F Fin S A Nwy-Swe: ok", "F Nrg-Nwy: fails",
                "F Nth S F Nrg-Nwy: ok", "F Ska C A Swe-Nwy: ok", "A Swe-Nwy via convoy: fails",
                "F Bar S A Swe-Nwy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Swe")));
        // A Swe cannot retreat (Den and Fin occupied, Nwy a standoff and its attacker's origin): disbanded at once
        assertNull(state.getDislodged(prov(state, "Swe")));
        assertEquals(1, state.nUnits(RUSSIA));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Nrg")));
        assertNull(state.getUnit(prov(state, "Nwy")));
    }

    @Test
    public void g15BounceAndDislodgeWithDoubleConvoy() {
        // A Lon-Bel 2 (A Hol) dislodges A Bel (hold 1); A Bel-Lon 1 and A Yor-Lon 1 stand off (A Bel, convoyed,
        // still prevents - 4.A.7 b); London is left empty
        place(state, ENGLAND, "F Nth", "A Hol", "A Yor", "A Lon");
        place(state, FRANCE, "F Eng", "A Bel");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Bel: ok", "A Hol S A Lon-Bel: ok", "A Yor-Lon: fails",
                "A Lon-Bel via convoy: ok", "F Eng C A Bel-Lon: ok", "A Bel-Lon via convoy: fails");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(army(FRANCE), state.getDislodged(prov(state, "Bel")));
        assertNull(state.getUnit(prov(state, "Lon")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Yor")));
    }

    @Test
    public void g16TheTwoUnitInOneAreaBugMovingByConvoy() {
        // A Nwy-Swe 3 and A Swe-Nwy via convoy 2 swap; F Nth-Nwy 1 loses to A Swe-Nwy's 2
        place(state, ENGLAND, "A Nwy", "A Den", "F Bal", "F Nth");
        place(state, RUSSIA, "A Swe", "F Ska", "F Nrg");
        start();
        playExpecting(state, fm, "A Nwy-Swe: ok", "A Den S A Nwy-Swe: ok", "F Bal S A Nwy-Swe: ok", "F Nth-Nwy: fails",
                "A Swe-Nwy via convoy: ok", "F Ska C A Swe-Nwy: ok", "F Nrg S A Swe-Nwy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Swe")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Nwy")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
    }

    @Test
    public void g17TheTwoUnitInOneAreaBugMovingOverLand() {
        // A Nwy-Swe via convoy 3 and A Swe-Nwy by land 2 swap; F Nth-Nwy 1 bounces
        place(state, ENGLAND, "A Nwy", "A Den", "F Bal", "F Ska", "F Nth");
        place(state, RUSSIA, "A Swe", "F Nrg");
        start();
        playExpecting(state, fm, "A Nwy-Swe via convoy: ok", "A Den S A Nwy-Swe: ok", "F Bal S A Nwy-Swe: ok",
                "F Ska C A Nwy-Swe: ok", "F Nth-Nwy: fails", "A Swe-Nwy: ok", "F Nrg S A Swe-Nwy: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Swe")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Nwy")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
    }

    @Test
    public void g18TheTwoUnitInOneAreaBugWithDoubleConvoy() {
        // A Lon-Bel 3 (A Hol, A Ruh) and A Bel-Lon 2 (A Wal) swap; A Yor-Lon 1 fails
        place(state, ENGLAND, "F Nth", "A Hol", "A Yor", "A Lon", "A Ruh");
        place(state, FRANCE, "F Eng", "A Bel", "A Wal");
        start();
        playExpecting(state, fm, "F Nth C A Lon-Bel: ok", "A Hol S A Lon-Bel: ok", "A Yor-Lon: fails",
                "A Lon-Bel via convoy: ok", "A Ruh S A Lon-Bel: ok", "F Eng C A Bel-Lon: ok",
                "A Bel-Lon via convoy: ok", "A Wal S A Bel-Lon: ok");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Lon")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Yor")));
    }

    @Test
    public void g19SwappingWithIntentOfAnUnnecessaryConvoyWhenThatConvoyIsOffered() {
        // DIFFERS FROM THE DATC: the DATC treats F Wes's convoy as illegal (Wes is not necessary to any route) and
        // has no swap. Here F Wes is on the chain Mar-GoL-Wes-Spa, so its convoy is offered; it is a French fleet
        // convoying exactly A Mar-Spa, so the move is convoyed (p.15), and the route Mar-GoL-Spa (Italy's F GoL)
        // carries it: no head-to-head battle, the armies swap
        place(state, FRANCE, "A Mar", "F Wes");
        place(state, ITALY, "F GoL", "A Spa");
        start();
        playExpecting(state, fm, "A Mar-Spa: ok", "F Wes C A Mar-Spa: ok", "F GoL C A Mar-Spa: ok", "A Spa-Mar: ok");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Spa")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Mar")));
    }

    @Test
    public void g20ExplicitConvoyToAdjacentProvinceDisrupted() {
        // via convoy, F Eng dislodged: no fallback to land, A Pic stays
        place(state, FRANCE, "F Bre", "A Pic", "A Bur", "F Mid");
        place(state, ENGLAND, "F Eng");
        start();
        playExpecting(state, fm, "F Bre-Eng: ok", "A Pic-Bel via convoy: fails", "A Bur S A Pic-Bel: ok",
                "F Mid S F Bre-Eng: ok", "F Eng C A Pic-Bel: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Pic")));
        assertNull(state.getUnit(prov(state, "Bel")));
    }
}

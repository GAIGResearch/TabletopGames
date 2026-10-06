package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * DATC v3.0 test cases 6.D (supports and dislodges) and 6.E (head-to-head battles and beleaguered garrisons) that
 * involve no convoy and only orders this game offers. The convoy cases 6.D.6, 6.D.8, 6.D.16, 6.D.27, 6.E.11 are in
 * DiplomacyDatcConvoyTest. Skipped: 6.D.22-24, 6.D.28-32, 6.D.34, 6.E.14 (illegal orders, never offered). DATC choice 4.A.7 b: a unit
 * dislodged other than head to head still prevents. Spring 1901 orders on a cleared board; "ok" means the order
 * succeeded (a support: it counted - it matched and was not cut).
 */
public class DiplomacyDatcSupportTest {

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

    @Test
    public void d01SupportedHoldCanPreventDislodgement() {
        // A Tri-Ven: 2 (F Adr) vs hold 1 + A Tyr = 2 - fails
        place(state, AUSTRIA, "F Adr", "A Tri");
        place(state, ITALY, "A Ven", "A Tyr");
        start();
        playExpecting(state, fm, "F Adr S A Tri-Ven: ok", "A Tri-Ven: fails", "A Ven H: ok", "A Tyr S A Ven: ok");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Tri")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Ven")));
    }

    @Test
    public void d02MoveCutsSupportOnHold() {
        // A Vie-Tyr cuts A Tyr's hold support (and fails, 1 vs hold 1): A Tri-Ven 2 vs hold 1 - dislodges A Ven
        place(state, AUSTRIA, "F Adr", "A Tri", "A Vie");
        place(state, ITALY, "A Ven", "A Tyr");
        start();
        playExpecting(state, fm, "F Adr S A Tri-Ven: ok", "A Tri-Ven: ok", "A Vie-Tyr: fails", "A Ven H: fails",
                "A Tyr S A Ven: fails");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Ven")));
        assertEquals(army(ITALY), state.getDislodged(prov(state, "Ven")));
    }

    @Test
    public void d03MoveCutsSupportOnMove() {
        // F Ion-Adr cuts F Adr's support (and fails, 1 vs hold 1): A Tri-Ven 1 vs hold 1 - fails
        place(state, AUSTRIA, "F Adr", "A Tri");
        place(state, ITALY, "A Ven", "F Ion");
        start();
        playExpecting(state, fm, "F Adr S A Tri-Ven: fails", "A Tri-Ven: fails", "A Ven H: ok", "F Ion-Adr: fails");
        assertEquals(army(ITALY), state.getUnit(prov(state, "Ven")));
    }

    @Test
    public void d04SupportToHoldOnUnitSupportingAHold() {
        // A Pru-Ber cuts A Ber's support for F Kie (it comes from Pru, not Kie); A Ber's hold 1 + F Kie = 2 vs
        // attack 2 - fails
        place(state, GERMANY, "A Ber", "F Kie");
        place(state, RUSSIA, "F Bal", "A Pru");
        start();
        playExpecting(state, fm, "A Ber S F Kie: fails", "F Kie S A Ber: ok", "F Bal S A Pru-Ber: ok",
                "A Pru-Ber: fails");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
    }

    @Test
    public void d05SupportToHoldOnUnitSupportingAMove() {
        // A Ber is supporting a move, so may receive hold support: hold 2 vs attack 2. Its own support is cut by
        // A Pru-Ber, but A Mun-Sil (1) still enters the empty Sil
        place(state, GERMANY, "A Ber", "F Kie", "A Mun");
        place(state, RUSSIA, "F Bal", "A Pru");
        start();
        playExpecting(state, fm, "A Ber S A Mun-Sil: fails", "F Kie S A Ber: ok", "A Mun-Sil: ok",
                "F Bal S A Pru-Ber: ok", "A Pru-Ber: fails");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Sil")));
    }

    @Test
    public void d07SupportToHoldOnMovingUnitNotAllowed() {
        // F Bal-Swe bounces off A Fin-Swe (1 vs 1); F Pru's hold support does not count for a moving unit, so
        // F Lvn-Bal (2) vs hold 1 dislodges F Bal
        place(state, GERMANY, "F Bal", "F Pru");
        place(state, RUSSIA, "F Lvn", "F Bot", "A Fin");
        start();
        playExpecting(state, fm, "F Bal-Swe: fails", "F Pru S F Bal: fails", "F Lvn-Bal: ok", "F Bot S F Lvn-Bal: ok",
                "A Fin-Swe: fails");
        assertEquals(fleet(GERMANY), state.getDislodged(prov(state, "Bal")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Bal")));
    }

    @Test
    public void d09SupportToMoveOnHoldingUnitNotAllowed() {
        // A Alb S A Tri-Ser does not count as hold support for the holding A Tri: A Ven-Tri 2 vs 1 dislodges it
        place(state, ITALY, "A Ven", "A Tyr");
        place(state, AUSTRIA, "A Alb", "A Tri");
        start();
        playExpecting(state, fm, "A Ven-Tri: ok", "A Tyr S A Ven-Tri: ok", "A Alb S A Tri-Ser: fails", "A Tri H: fails");
        assertEquals(army(AUSTRIA), state.getDislodged(prov(state, "Tri")));
    }

    @Test
    public void d10SelfDislodgmentProhibited() {
        place(state, GERMANY, "A Ber", "F Kie", "A Mun");
        start();
        playExpecting(state, fm, "A Ber H: ok", "F Kie-Ber: fails", "A Mun S F Kie-Ber: ok");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
    }

    @Test
    public void d11NoSelfDislodgmentOfReturningUnit() {
        // A Ber-Pru and A War-Pru bounce (1 vs 1); A Ber stays, and F Kie-Ber attacks Germany's own unit: 0
        place(state, GERMANY, "A Ber", "F Kie", "A Mun");
        place(state, RUSSIA, "A War");
        start();
        playExpecting(state, fm, "A Ber-Pru: fails", "F Kie-Ber: fails", "A Mun S F Kie-Ber: ok", "A War-Pru: fails");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void d12SupportingAForeignUnitToDislodgeOwnUnitProhibited() {
        // A Ven-Tri: 1 + Austrian support not counted against Austria's F Tri = 1 vs hold 1
        place(state, AUSTRIA, "F Tri", "A Vie");
        place(state, ITALY, "A Ven");
        start();
        playExpecting(state, fm, "F Tri H: ok", "A Vie S A Ven-Tri: ok", "A Ven-Tri: fails");
        assertEquals(fleet(AUSTRIA), state.getUnit(prov(state, "Tri")));
    }

    @Test
    public void d13SupportingAForeignUnitToDislodgeAReturningOwnUnitProhibited() {
        // F Tri-Adr and F Apu-Adr bounce; F Tri stays and A Ven-Tri is 1 (Austrian support not counted) vs 1
        place(state, AUSTRIA, "F Tri", "A Vie");
        place(state, ITALY, "A Ven", "F Apu");
        start();
        playExpecting(state, fm, "F Tri-Adr: fails", "A Vie S A Ven-Tri: ok", "A Ven-Tri: fails", "F Apu-Adr: fails");
        assertEquals(fleet(AUSTRIA), state.getUnit(prov(state, "Tri")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void d14SupportingAForeignUnitIsNotEnoughToPreventDislodgement() {
        // A Ven-Tri: 1 + A Tyr + F Adr = 3 (Austria's support not counted) vs hold 1 - dislodges F Tri
        place(state, AUSTRIA, "F Tri", "A Vie");
        place(state, ITALY, "A Ven", "A Tyr", "F Adr");
        start();
        playExpecting(state, fm, "F Tri H: fails", "A Vie S A Ven-Tri: ok", "A Ven-Tri: ok", "A Tyr S A Ven-Tri: ok",
                "F Adr S A Ven-Tri: ok");
        assertEquals(fleet(AUSTRIA), state.getDislodged(prov(state, "Tri")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Tri")));
    }

    @Test
    public void d15DefenderCannotCutSupportForAttackOnItself() {
        // F Ank-Con comes from Ank, where F Con's support is given: not cut. F Ank-Con fails (1 vs hold 1), so
        // F Bla-Ank 2 vs hold 1 dislodges F Ank
        place(state, RUSSIA, "F Con", "F Bla");
        place(state, TURKEY, "F Ank");
        start();
        playExpecting(state, fm, "F Con S F Bla-Ank: ok", "F Bla-Ank: ok", "F Ank-Con: fails");
        assertEquals(fleet(TURKEY), state.getDislodged(prov(state, "Ank")));
        assertEquals(prov(state, "Bla"), state.getDislodgedFrom(prov(state, "Ank")));
    }

    @Test
    public void d17DislodgementCutsSupports() {
        // F Ank-Con (2, with A Smy) vs hold 1 dislodges F Con, cutting its support though the attack came from
        // the province supported into. F Bla-Ank (1) then bounces with A Arm-Ank (1), F Ank having left
        place(state, RUSSIA, "F Con", "F Bla");
        place(state, TURKEY, "F Ank", "A Smy", "A Arm");
        start();
        playExpecting(state, fm, "F Con S F Bla-Ank: fails", "F Bla-Ank: fails", "F Ank-Con: ok",
                "A Smy S F Ank-Con: ok", "A Arm-Ank: fails");
        assertEquals(fleet(TURKEY), state.getUnit(prov(state, "Con")));
        assertEquals(fleet(RUSSIA), state.getDislodged(prov(state, "Con")));
        assertNull(state.getUnit(prov(state, "Ank")));
    }

    @Test
    public void d18SurvivingUnitWillSustainSupport() {
        // A Bul's hold support: F Con hold 2 vs F Ank-Con 2 - not dislodged, support (attacked only from Ank)
        // stands: F Bla-Ank 2 vs hold 1 (Ank's move failed) and A Arm's prevent 1 - dislodges F Ank
        place(state, RUSSIA, "F Con", "F Bla", "A Bul");
        place(state, TURKEY, "F Ank", "A Smy", "A Arm");
        start();
        playExpecting(state, fm, "F Con S F Bla-Ank: ok", "F Bla-Ank: ok", "A Bul S F Con: ok", "F Ank-Con: fails",
                "A Smy S F Ank-Con: ok", "A Arm-Ank: fails");
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Ank")));
        // the dislodged F Ank cannot retreat (Arm, Con and Smy occupied, Bla its attacker's origin), so it is
        // disbanded at once and no retreat phase is played
        assertNull(state.getDislodged(prov(state, "Ank")));
        assertEquals(2, state.nUnits(TURKEY)); // A Smy, A Arm
        assertFalse(state.getPhase().isRetreats());
    }

    @Test
    public void d19EvenWhenSurvivingIsInAlternativeWay() {
        // the Russian A Smy's support does not help dislodge the Russian F Con: F Ank-Con 1 vs hold 1 - fails;
        // F Bla-Ank 2 vs hold 1 dislodges F Ank
        place(state, RUSSIA, "F Con", "F Bla", "A Smy");
        place(state, TURKEY, "F Ank");
        start();
        playExpecting(state, fm, "F Con S F Bla-Ank: ok", "F Bla-Ank: ok", "A Smy S F Ank-Con: ok", "F Ank-Con: fails");
        assertEquals(fleet(TURKEY), state.getDislodged(prov(state, "Ank")));
    }

    @Test
    public void d20UnitCannotCutSupportOfItsOwnCountry() {
        // A Yor-Lon (own unit: attack 0) does not cut F Lon's support: F Nth-Eng 2 vs hold 1 dislodges F Eng
        place(state, ENGLAND, "F Lon", "F Nth", "A Yor");
        place(state, FRANCE, "F Eng");
        start();
        playExpecting(state, fm, "F Lon S F Nth-Eng: ok", "F Nth-Eng: ok", "A Yor-Lon: fails", "F Eng H: fails");
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Eng")));
    }

    @Test
    public void d21DislodgingDoesNotCancelASupportCut() {
        // A Mun-Tyr cuts A Tyr's support though A Mun is dislodged (A Sil-Mun 2 vs hold 1): A Ven-Tri 1 vs 1 fails
        place(state, AUSTRIA, "F Tri");
        place(state, ITALY, "A Ven", "A Tyr");
        place(state, GERMANY, "A Mun");
        place(state, RUSSIA, "A Sil", "A Ber");
        start();
        playExpecting(state, fm, "F Tri H: ok", "A Ven-Tri: fails", "A Tyr S A Ven-Tri: fails", "A Mun-Tyr: fails",
                "A Sil-Mun: ok", "A Ber S A Sil-Mun: ok");
        assertEquals(fleet(AUSTRIA), state.getUnit(prov(state, "Tri")));
        assertEquals(army(GERMANY), state.getDislodged(prov(state, "Mun")));
    }

    @Test
    public void d25FailingHoldSupportCanBeSupported() {
        // A Ber's support for the moving A Pru fails, but A Ber may still be supported: hold 2 vs attack 2
        place(state, GERMANY, "A Ber", "F Kie");
        place(state, RUSSIA, "F Bal", "A Pru");
        start();
        playExpecting(state, fm, "A Ber S A Pru: fails", "F Kie S A Ber: ok", "F Bal S A Pru-Ber: ok", "A Pru-Ber: fails");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
    }

    @Test
    public void d26FailingMoveSupportCanBeSupported() {
        place(state, GERMANY, "A Ber", "F Kie");
        place(state, RUSSIA, "F Bal", "A Pru");
        start();
        playExpecting(state, fm, "A Ber S A Pru-Sil: fails", "F Kie S A Ber: ok", "F Bal S A Pru-Ber: ok",
                "A Pru-Ber: fails");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
    }

    @Test
    public void d33UnwantedSupportAllowed() {
        // A Ser-Bud 2 (Russian support) vs A Vie-Bud's prevent 1 - succeeds, so A Bul-Ser enters the vacated Ser
        place(state, AUSTRIA, "A Ser", "A Vie");
        place(state, RUSSIA, "A Gal");
        place(state, TURKEY, "A Bul");
        start();
        playExpecting(state, fm, "A Ser-Bud: ok", "A Vie-Bud: fails", "A Gal S A Ser-Bud: ok", "A Bul-Ser: ok");
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Ser")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Bud")));
    }

    @Test
    public void e01DislodgedUnitHasNoEffectOnAttackersArea() {
        // A Ber-Pru (2) beats A Pru-Ber (defend 1) head to head; the loser's prevent on Ber is 0, so F Kie-Ber
        // enters the vacated Ber
        place(state, GERMANY, "A Ber", "F Kie", "A Sil");
        place(state, RUSSIA, "A Pru");
        start();
        playExpecting(state, fm, "A Ber-Pru: ok", "F Kie-Ber: ok", "A Sil S A Ber-Pru: ok", "A Pru-Ber: fails");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Pru")));
        assertEquals(army(RUSSIA), state.getDislodged(prov(state, "Pru")));
        assertEquals(prov(state, "Ber"), state.getDislodgedFrom(prov(state, "Pru")));
    }

    @Test
    public void e02NoSelfDislodgementInHeadToHeadBattle() {
        // A Ber-Kie attacks its own fleet: 0; F Kie-Ber 1 vs defend 2 (A Ber + A Mun) - nothing moves
        place(state, GERMANY, "A Ber", "F Kie", "A Mun");
        start();
        playExpecting(state, fm, "A Ber-Kie: fails", "F Kie-Ber: fails", "A Mun S A Ber-Kie: ok");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Kie")));
    }

    @Test
    public void e03NoHelpInDislodgingOwnUnit() {
        // English F Kie-Ber: 1 (German support not counted against A Ber) vs defend 1; A Ber-Kie: 1 vs defend 2
        // (F Kie + A Mun's support, counted for defence)
        place(state, GERMANY, "A Ber", "A Mun");
        place(state, ENGLAND, "F Kie");
        start();
        playExpecting(state, fm, "A Ber-Kie: fails", "A Mun S F Kie-Ber: ok", "F Kie-Ber: fails");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Kie")));
    }

    @Test
    public void e04NonDislodgedLoserStillHasEffect() {
        // F Hol-Nth 3 vs defend 2 but vs F Nrg-Nth's prevent 3 - fails; F Nrg-Nth 3 vs prevent 3 - fails; so
        // F Nth-Hol (2 vs defend 3) fails but F Nth is not dislodged, and its prevent 2 stops A Ruh-Hol (2)
        place(state, GERMANY, "F Hol", "F Hel", "F Ska");
        place(state, FRANCE, "F Nth", "F Bel");
        place(state, ENGLAND, "F Edi", "F Yor", "F Nrg");
        place(state, AUSTRIA, "A Kie", "A Ruh");
        start();
        playExpecting(state, fm, "F Hol-Nth: fails", "F Hel S F Hol-Nth: ok", "F Ska S F Hol-Nth: ok",
                "F Nth-Hol: fails", "F Bel S F Nth-Hol: ok", "F Edi S F Nrg-Nth: ok", "F Yor S F Nrg-Nth: ok",
                "F Nrg-Nth: fails", "A Kie S A Ruh-Hol: ok", "A Ruh-Hol: fails");
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Hol")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void e05LoserDislodgedByAnotherArmyStillHasEffect() {
        // with F Lon's support F Nrg-Nth is 4: beats hold 1 and F Hol-Nth's prevent 3 - dislodges F Nth. F Nth was
        // not dislodged by F Hol, so its prevent 2 on Hol still stops A Ruh-Hol (2); F Hol (move failed) stays
        place(state, GERMANY, "F Hol", "F Hel", "F Ska");
        place(state, FRANCE, "F Nth", "F Bel");
        place(state, ENGLAND, "F Edi", "F Yor", "F Nrg", "F Lon");
        place(state, AUSTRIA, "A Kie", "A Ruh");
        start();
        playExpecting(state, fm, "F Hol-Nth: fails", "F Hel S F Hol-Nth: ok", "F Ska S F Hol-Nth: ok",
                "F Nth-Hol: fails", "F Bel S F Nth-Hol: ok", "F Edi S F Nrg-Nth: ok", "F Yor S F Nrg-Nth: ok",
                "F Nrg-Nth: ok", "F Lon S F Nrg-Nth: ok", "A Kie S A Ruh-Hol: ok", "A Ruh-Hol: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(FRANCE), state.getDislodged(prov(state, "Nth")));
        assertEquals(prov(state, "Nrg"), state.getDislodgedFrom(prov(state, "Nth")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Hol")));
    }

    @Test
    public void e06NotDislodgedBecauseOfOwnSupportStillHasEffect() {
        // F Hol-Nth: 1 + F Hel (F Eng's French support not counted against the French F Nth) = 2 vs defend 2 -
        // fails; F Nth not dislodged, so its prevent 2 stops A Ruh-Hol (2)
        place(state, GERMANY, "F Hol", "F Hel");
        place(state, FRANCE, "F Nth", "F Bel", "F Eng");
        place(state, AUSTRIA, "A Kie", "A Ruh");
        start();
        playExpecting(state, fm, "F Hol-Nth: fails", "F Hel S F Hol-Nth: ok", "F Nth-Hol: fails",
                "F Bel S F Nth-Hol: ok", "F Eng S F Hol-Nth: ok", "A Kie S A Ruh-Hol: ok", "A Ruh-Hol: fails");
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void e07NoSelfDislodgementWithBeleagueredGarrison() {
        // F Nwy-Nth: 1 + F Ska = 2 (England's F Yor not counted against England's F Nth) vs F Hel-Nth's prevent 2;
        // F Hel-Nth 2 vs F Nwy-Nth's prevent 3 (all supports count to prevent) - nothing moves
        place(state, ENGLAND, "F Nth", "F Yor");
        place(state, GERMANY, "F Hol", "F Hel");
        place(state, RUSSIA, "F Ska", "F Nwy");
        start();
        playExpecting(state, fm, "F Nth H: ok", "F Yor S F Nwy-Nth: ok", "F Hol S F Hel-Nth: ok", "F Hel-Nth: fails",
                "F Ska S F Nwy-Nth: ok", "F Nwy-Nth: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
    }

    @Test
    public void e08NoSelfDislodgementWithBeleagueredGarrisonAndHeadToHead() {
        // F Nth-Nwy 1 vs defend 3 fails; F Nwy-Nth 2 (F Ska) vs defend 1 but prevent 2 fails; F Hel-Nth 2 vs
        // F Nwy-Nth's prevent 3 (its opponent did not win) fails
        place(state, ENGLAND, "F Nth", "F Yor");
        place(state, GERMANY, "F Hol", "F Hel");
        place(state, RUSSIA, "F Ska", "F Nwy");
        start();
        playExpecting(state, fm, "F Nth-Nwy: fails", "F Yor S F Nwy-Nth: ok", "F Hol S F Hel-Nth: ok",
                "F Hel-Nth: fails", "F Ska S F Nwy-Nth: ok", "F Nwy-Nth: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Nwy")));
    }

    @Test
    public void e09AlmostSelfDislodgementWithBeleagueredGarrison() {
        // F Nth leaves for Nrg: F Nwy-Nth attacks an empty province with 3 (all supports) vs F Hel-Nth's prevent 2
        place(state, ENGLAND, "F Nth", "F Yor");
        place(state, GERMANY, "F Hol", "F Hel");
        place(state, RUSSIA, "F Ska", "F Nwy");
        start();
        playExpecting(state, fm, "F Nth-Nrg: ok", "F Yor S F Nwy-Nth: ok", "F Hol S F Hel-Nth: ok",
                "F Hel-Nth: fails", "F Ska S F Nwy-Nth: ok", "F Nwy-Nth: ok");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nrg")));
        assertEquals(fleet(RUSSIA), state.getUnit(prov(state, "Nth")));
        assertNull(state.getUnit(prov(state, "Nwy")));
    }

    @Test
    public void e10AlmostCircularMovementWithNoSelfDislodgement() {
        // F Hel-Nth 2 vs F Nwy-Nth's prevent 3 fails; so F Den-Hel meets its own fleet (0) and F Nth-Den meets
        // F Den holding (1 vs 1); F Nwy-Nth 2 (F Yor not counted) vs hold 1 but prevent 2 fails. Nothing moves
        place(state, ENGLAND, "F Nth", "F Yor");
        place(state, GERMANY, "F Hol", "F Hel", "F Den");
        place(state, RUSSIA, "F Ska", "F Nwy");
        start();
        playExpecting(state, fm, "F Nth-Den: fails", "F Yor S F Nwy-Nth: ok", "F Hol S F Hel-Nth: ok",
                "F Hel-Nth: fails", "F Den-Hel: fails", "F Ska S F Nwy-Nth: ok", "F Nwy-Nth: fails");
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Den")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Hel")));
    }

    @Test
    public void e12SupportOnAttackOnOwnUnitCanBeUsedForOtherMeans() {
        // A Bud-Rum 1 vs hold 1 fails (A Rum supports); Italian A Vie-Bud 1 (Austrian support not counted against
        // the Austrian A Bud) vs hold 1 fails, but prevents with 2; A Gal-Bud 2 vs prevent 2 fails.
        // A Rum's support is not cut: A Bud-Rum comes from Bud, where it supports
        place(state, AUSTRIA, "A Bud", "A Ser");
        place(state, ITALY, "A Vie");
        place(state, RUSSIA, "A Gal", "A Rum");
        start();
        playExpecting(state, fm, "A Bud-Rum: fails", "A Ser S A Vie-Bud: ok", "A Vie-Bud: fails", "A Gal-Bud: fails",
                "A Rum S A Gal-Bud: ok");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Bud")));
        assertEquals(0, totalDislodged(state));
    }

    @Test
    public void e13ThreeWayBeleagueredGarrison() {
        // three attacks of 2 on F Nth: each beats hold 1 but not the others' prevent 2
        place(state, ENGLAND, "F Edi", "F Yor");
        place(state, FRANCE, "F Bel", "F Eng");
        place(state, GERMANY, "F Nth");
        place(state, RUSSIA, "F Nrg", "F Nwy");
        start();
        playExpecting(state, fm, "F Edi S F Yor-Nth: ok", "F Yor-Nth: fails", "F Bel-Nth: fails",
                "F Eng S F Bel-Nth: ok", "F Nth H: ok", "F Nrg-Nth: fails", "F Nwy S F Nrg-Nth: ok");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Nth")));
    }

    @Test
    public void e15FriendlyHeadToHeadBattle() {
        // A Kie-Ber (3) vs A Ber-Kie (3) head to head: both fail, and neither lost to the other, so each still
        // prevents with 3: A Ruh-Kie (2) and A Pru-Ber (2) fail too
        place(state, ENGLAND, "F Hol", "A Ruh");
        place(state, FRANCE, "A Kie", "A Mun", "A Sil");
        place(state, GERMANY, "A Ber", "F Den", "F Hel");
        place(state, RUSSIA, "F Bal", "A Pru");
        start();
        playExpecting(state, fm, "F Hol S A Ruh-Kie: ok", "A Ruh-Kie: fails", "A Kie-Ber: fails",
                "A Mun S A Kie-Ber: ok", "A Sil S A Kie-Ber: ok", "A Ber-Kie: fails", "F Den S A Ber-Kie: ok",
                "F Hel S A Ber-Kie: ok", "F Bal S A Pru-Ber: ok", "A Pru-Ber: fails");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Kie")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(0, totalDislodged(state));
    }
}

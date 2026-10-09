package games.diplomacy;

import core.Game;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: the whole sample game of the rulebook (p.19-22), Spring 1901 to the 1902 adjustments, played through
 * actions in a real game. Every outcome is worked out from the rules; where the rulebook's text and the rules
 * disagree the rules are followed (see the comments marked "Rulebook text").
 */
public class DiplomacySampleGameTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    private void assertUnits(Map<String, DiplomacyUnit> expected) {
        for (DiplomacyProvince p : state.getMap().provinces())
            assertEquals("unit in " + p.name(), expected.get(p.name()), state.getUnit(p));
    }

    private void assertCentresAndAdjustments(int[] centres, int[] adjustments) {
        for (int p = 0; p < N_POWERS; p++) {
            assertEquals("centres of " + POWER_NAMES[p], centres[p], state.nCentres(p));
            assertEquals("adjustment of " + POWER_NAMES[p], adjustments[p], state.adjustment(p));
        }
    }

    @Test
    public void wholeSampleGameFromSpring1901ToThe1902Adjustments() {
        Game game = newGame(16, helpingAnyUnit());
        state = (DiplomacyGameState) game.getGameState();
        fm = (DiplomacyForwardModel) game.getForwardModel();

        // ---- Spring 1901 (p.19): all succeed but the two into Bla and the two into Gal (checked in
        // DiplomacyGameFlowTest.sampleGameSpring1901)
        DiplomacyGameFlowTest.playSampleSpring1901(state, fm);
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());

        // ---- Fall 1901 (p.20)
        // Ser: A Bud-Ser 1 vs A Bul-Ser 1 stand off, so A Bul stays and A Con-Bul fails ("Con-Bul also does not
        // succeed"). Mar: A Bur-Mar vs A Pie-Mar stand off. Bel: F Pic-Bel vs A Ruh-Bel stand off.
        // F Sev-Rum 2 (A Ukr) into empty Rum succeeds; A War-Gal enters empty Gal (Spring's standoff left it
        // empty, A Bud goes elsewhere). A Yor-Nwy is not adjacent: the only action for it is the via-convoy move
        // (the rulebook writes "A Yor-Nwy", which with F Nth convoying it is the same move).
        playExpecting(state, fm,
                "A Tri H: ok", "A Bud-Ser: fails", "F Alb-Gre: ok",
                "A Yor-Nwy via convoy: ok", "F Nth C A Yor-Nwy: ok", "F Nrg-Bar: ok",
                "A Bur-Mar: fails", "A Spa-Por: ok", "F Pic-Bel: fails",
                "A Kie-Hol: ok", "A Ruh-Bel: fails", "F Den H: ok",
                "A Ven H: ok", "A Pie-Mar: fails", "F Ion-Tun: ok",
                "A Ukr S F Sev-Rum: ok", "A War-Gal: ok", "F Bot-Swe: ok", "F Sev-Rum: ok",
                "A Bul-Ser: fails", "A Con-Bul: fails", "F Ank-Bla: ok");
        // no retreats: straight to the adjustments
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(1901, state.getYear());
        // centres: Austria Vie Bud Tri + Gre = 4, 3 units -> +1
        //          England Lon Edi Lvp + Nwy = 4, 3 units -> +1
        //          France Par Mar Bre + Por = 4 (Spa only passed through: A Spa left it in Fall), 3 units -> +1
        //          Germany Ber Kie Mun + Den Hol = 5, 3 units -> +2
        //          Italy Rom Nap Ven + Tun = 4, 3 units -> +1
        //          Russia Mos StP War Sev + Rum Swe = 6, 4 units -> +2
        //          Turkey Ank Con Smy + Bul = 4, 3 units -> +1
        // as the rulebook says: one build each for England, Turkey, Austria, Italy, France, two for Russia, Germany.
        // Bel, Ser, Spa are still neutral (31 + 3 = 34)
        assertCentresAndAdjustments(new int[]{4, 4, 4, 5, 4, 6, 4}, new int[]{1, 1, 1, 2, 1, 2, 1});
        assertEquals(-1, state.getOwner(prov(state, "Spa")));
        assertEquals(-1, state.getOwner(prov(state, "Bel")));
        assertEquals(-1, state.getOwner(prov(state, "Ser")));

        // ---- Fall 1901 builds (p.20). Rulebook text: "France builds one unit for Portugal 'A Por', but none for
        // Spain" - France is entitled to one build, already made with F Mar, and builds are only in home centres;
        // A Por is the army that moved Spa-Por, not a build. So France builds F Mar only.
        play(state, fm, "Build A Vie", "Build F Edi", "Build F Mar", "Build F Kie", "Build A Mun", "Build F Nap",
                "Build A StP", "Build A Sev", "Build A Smy");
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        Map<String, DiplomacyUnit> units = new HashMap<>();
        units.put("Tri", army(AUSTRIA));
        units.put("Bud", army(AUSTRIA));
        units.put("Gre", fleet(AUSTRIA));
        units.put("Vie", army(AUSTRIA));
        units.put("Nwy", army(ENGLAND));
        units.put("Nth", fleet(ENGLAND));
        units.put("Bar", fleet(ENGLAND));
        units.put("Edi", fleet(ENGLAND));
        units.put("Bur", army(FRANCE));
        units.put("Por", army(FRANCE));
        units.put("Pic", fleet(FRANCE));
        units.put("Mar", fleet(FRANCE));
        units.put("Hol", army(GERMANY));
        units.put("Ruh", army(GERMANY));
        units.put("Den", fleet(GERMANY));
        units.put("Kie", fleet(GERMANY));
        units.put("Mun", army(GERMANY));
        units.put("Ven", army(ITALY));
        units.put("Pie", army(ITALY));
        units.put("Tun", fleet(ITALY));
        units.put("Nap", fleet(ITALY));
        units.put("Ukr", army(RUSSIA));
        units.put("Gal", army(RUSSIA));
        units.put("Swe", fleet(RUSSIA));
        units.put("Rum", fleet(RUSSIA));
        units.put("StP", army(RUSSIA));
        units.put("Sev", army(RUSSIA));
        units.put("Bul", army(TURKEY));
        units.put("Con", army(TURKEY));
        units.put("Bla", fleet(TURKEY));
        units.put("Smy", army(TURKEY));
        assertUnits(units);

        // ---- Spring 1902 (p.20-21)
        // Nwy/StP: A Nwy-StP 2 (F Bar) and A StP-Nwy 2 (F Swe - Sweden touches Norway along the coast) are a
        // head-to-head battle of equal strength: both fail; F Nth-Nwy and then F Edi-Nth fail behind them.
        // Bud: A Bud-Ser enters empty Ser, and A Tri-Bud, A Vie-Bud, A Gal-Bud (1 each) stand off in the vacated Bud.
        // Bel: A Mun-Bur cuts A Bur's support (attack not from Bel); A Hol-Bel 2 (A Ruh) beats F Pic-Bel 1, and
        // F Kie-Hol follows into Hol. A Mun-Bur 1 vs A Bur's hold 1 fails.
        // Rum: A Bul-Rum 2 (F Bla) vs F Rum's hold 3 (A Ukr, A Sev - neither attacked): fails, and A Con-Bul behind
        // it. A Pie-Mar 1 vs F Mar 1 fails.
        playExpecting(state, fm,
                "A Tri-Bud: fails", "A Vie-Bud: fails", "A Bud-Ser: ok", "F Gre H: ok",
                "A Nwy-StP: fails", "F Nth-Nwy: fails", "F Bar S A Nwy-StP: ok", "F Edi-Nth: fails",
                "A Bur S F Pic-Bel: fails", "A Por-Spa: ok", "F Pic-Bel: fails", "F Mar H: ok",
                "A Hol-Bel: ok", "A Ruh S A Hol-Bel: ok", "A Mun-Bur: fails", "F Den H: ok", "F Kie-Hol: ok",
                "A Ven H: ok", "A Pie-Mar: fails", "F Tun-Wes: ok", "F Nap-Tyn: ok",
                "A Ukr S F Rum: ok", "A Gal-Bud: fails", "A StP-Nwy: fails", "A Sev S F Rum: ok",
                "F Swe S A StP-Nwy: ok", "F Rum H: ok",
                "A Bul-Rum: fails", "A Con-Bul: fails", "A Smy-Arm: ok", "F Bla S A Bul-Rum: ok");
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertTrue(state.isStandoff(prov(state, "Bud")));
        units.remove("Bud");
        units.put("Ser", army(AUSTRIA));
        units.remove("Por");
        units.put("Spa", army(FRANCE));
        units.remove("Hol");
        units.put("Bel", army(GERMANY));
        units.remove("Kie");
        units.put("Hol", fleet(GERMANY));
        units.remove("Tun");
        units.put("Wes", fleet(ITALY));
        units.remove("Nap");
        units.put("Tyn", fleet(ITALY));
        units.remove("Smy");
        units.put("Arm", army(TURKEY));
        assertUnits(units);

        // ---- Fall 1902 (p.21-22)
        // Cut supports: F Swe (by F Den-Swe), F Mar (A Pie-Mar), A Sev (A Arm-Sev), A Gal (A Vie-Gal), F Rum
        // (A Bul-Rum). A Bel's support of A Ruh-Bur is not cut: the attack comes from Bur, where it is given.
        // Mar: A Pie-Mar 1 vs F Mar 2 (A Spa) fails, and A Ven-Pie behind it. Sev: A Arm-Sev 1 vs A Sev 2 (A Ukr)
        // fails. Gal: A Vie-Gal 1 vs 1 fails.
        // Bur/Bel: A Bur-Bel 2 (F Pic) vs A Bel 2 (F Hol) fails; A Ruh-Bur 3 (A Mun, A Bel) vs A Bur's 1:
        // A Bur dislodged (not a head-to-head battle: A Bel does not move to Bur).
        // Rum: A Bul-Rum 3 (A Ser, F Bla) vs F Rum 1 (both its supports cut): F Rum dislodged; A Con-Bul follows.
        // Rulebook text calls it "the Russian Army there" - it is the fleet that moved Sev-Rum in Fall 1901.
        // StP: A Nwy-StP 2 (F Bar) beats A StP-Nwy 1 head to head: A StP dislodged. Its move made no standoff, so
        // F Nth-Nwy 1 enters Nwy and F Edi-Nth follows. A Tri-Bud enters empty Bud. F Den-Swe 1 vs 1 fails.
        playExpecting(state, fm,
                "A Vie-Gal: fails", "A Tri-Bud: ok", "A Ser S A Bul-Rum: ok", "F Gre H: ok",
                "A Nwy-StP: ok", "F Bar S A Nwy-StP: ok", "F Nth-Nwy: ok", "F Edi-Nth: ok",
                "A Bur-Bel: fails", "F Pic S A Bur-Bel: ok", "A Spa S F Mar: ok", "F Mar S A Spa: fails",
                "A Ruh-Bur: ok", "A Mun S A Ruh-Bur: ok", "A Bel S A Ruh-Bur: ok", "F Den-Swe: fails",
                "F Hol S A Bel: ok",
                "A Ven-Pie: fails", "A Pie-Mar: fails", "F Wes-Mid: ok", "F Tyn-GoL: ok",
                "A StP-Nwy: fails", "F Swe S A StP-Nwy: fails", "F Rum S A Sev: fails", "A Sev S F Rum: fails",
                "A Gal S F Rum: fails", "A Ukr S A Sev: ok",
                "A Bul-Rum: ok", "A Con-Bul: ok", "A Arm-Sev: fails", "F Bla S A Bul-Rum: ok");

        // ---- Fall 1902 retreats (p.22): three units dislodged. F Rum has nowhere to go (Bla and Sev occupied,
        // Bul occupied and the attacker's origin; a fleet cannot go inland) and is disbanded at once.
        // A Bur: Par or Gas (Pic, Mar, Bel, Mun occupied, Ruh the attacker's origin, no standoff anywhere).
        // A StP: Fin, Lvn or Mos (Nwy now occupied by F Nth).
        assertEquals(DiplomacyPhase.FALL_RETREATS, state.getPhase());
        assertEquals(army(FRANCE), state.getDislodged(prov(state, "Bur")));
        assertEquals(army(RUSSIA), state.getDislodged(prov(state, "StP")));
        assertNull(state.getDislodged(prov(state, "Rum")));
        assertEquals(2, totalDislodged(state));
        assertEquals(army(TURKEY), state.getUnit(prov(state, "Rum")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Bur")));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "StP")));
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(orderSet(state, "A Bur R Par", "A Bur R Gas", "Disband Bur"), legalSet(state, fm));
        play(state, fm, "A Bur R Gas");
        assertEquals(RUSSIA, state.getCurrentPlayer());
        assertEquals(orderSet(state, "A StP R Fin", "A StP R Lvn", "A StP R Mos", "Disband StP"), legalSet(state, fm));
        play(state, fm, "A StP R Mos");

        // ---- 1902 adjustments (p.22)
        // centres: Austria Vie Bud Tri Ser Gre = 5, 4 units -> +1 (Tri free)
        //          England Lon Edi Lvp Nwy StP = 5, 4 units -> +1
        //          France Par Mar Bre Por Spa = 5, 4 units -> +1
        //          Germany Ber Kie Mun Den Hol Bel = 6, 5 units -> +1
        //          Italy Rom Nap Ven Tun = 4, 4 units -> 0
        //          Russia Mos War Sev Swe = 4 (StP to England, Rum to Turkey), 5 units -> -1
        //          Turkey Ank Con Smy Bul Rum = 5, 4 units -> +1
        // every neutral centre is owned (34)
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertCentresAndAdjustments(new int[]{5, 5, 5, 6, 4, 4, 5}, new int[]{1, 1, 1, 1, 0, -1, 1});
        assertEquals(ENGLAND, state.getOwner(prov(state, "StP")));
        assertEquals(TURKEY, state.getOwner(prov(state, "Rum")));
        play(state, fm, "Build A Tri", "Build F Lon", "Build A Par", "Build F Kie", "Disband Gal", "Build F Smy");

        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1903, state.getYear());
        units.remove("Bur");
        units.put("Gas", army(FRANCE));
        units.remove("StP");
        units.put("Mos", army(RUSSIA));
        units.put("Rum", army(TURKEY));     // A Bul-Rum; F Rum disbanded
        units.put("Bul", army(TURKEY));     // A Con-Bul
        units.remove("Con");
        units.put("Bur", army(GERMANY));
        units.remove("Ruh");
        units.put("StP", army(ENGLAND));
        units.put("Nwy", fleet(ENGLAND));
        units.put("Nth", fleet(ENGLAND));
        units.remove("Edi");
        units.remove("Tri");
        units.put("Bud", army(AUSTRIA));
        units.remove("Wes");
        units.put("Mid", fleet(ITALY));
        units.remove("Tyn");
        units.put("GoL", fleet(ITALY));
        units.remove("Gal");
        // builds
        units.put("Tri", army(AUSTRIA));
        units.put("Lon", fleet(ENGLAND));
        units.put("Par", army(FRANCE));
        units.put("Kie", fleet(GERMANY));
        units.put("Smy", fleet(TURKEY));
        assertUnits(units);
        assertEquals(34, totalUnits(state));
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], state.nCentres(p), state.nUnits(p));
    }
}

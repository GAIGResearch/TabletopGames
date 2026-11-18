package games.diplomacy;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import org.junit.Test;

import java.util.*;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Integration: real games driven only by fm.next - the sample game's Spring 1901 (rulebook p.19) continued through
 * a Fall turn and adjustments, an eliminated power, and seeded random games played to the end.
 */
public class DiplomacyGameFlowTest {

    static final String[] SAMPLE_SPRING_1901 = {
            "A Vie-Tri", "A Bud-Gal", "F Tri-Alb",
            "A Lvp-Yor", "F Lon-Nth", "F Edi-Nrg",
            "A Par-Bur", "A Mar-Spa", "F Bre-Pic",
            "A Ber-Kie", "A Mun-Ruh", "F Kie-Den",
            "A Ven-Pie", "A Rom-Ven", "F Nap-Ion",
            "A Mos-Ukr", "A War-Gal", "F StP/sc-Bot", "F Sev-Bla",
            "A Con-Bul", "A Smy-Con", "F Ank-Bla"};

    /** The sample game's Spring 1901 orders: every order succeeds but the two into Bla and the two into Gal. */
    static void playSampleSpring1901(DiplomacyGameState state, DiplomacyForwardModel fm) {
        play(state, fm, SAMPLE_SPRING_1901);
    }

    private static int powerIndex(String unitProvince) {
        return switch (unitProvince) {
            case "Vie", "Bud", "Tri" -> AUSTRIA;
            case "Lvp", "Lon", "Edi" -> ENGLAND;
            case "Par", "Mar", "Bre" -> FRANCE;
            case "Ber", "Mun", "Kie" -> GERMANY;
            case "Ven", "Rom", "Nap" -> ITALY;
            case "Mos", "War", "StP/sc", "Sev" -> RUSSIA;
            default -> TURKEY;
        };
    }

    @Test
    public void sampleGameSpring1901() {
        Game game = newGame(11);
        DiplomacyGameState state = (DiplomacyGameState) game.getGameState();
        DiplomacyForwardModel fm = (DiplomacyForwardModel) game.getForwardModel();
        playSampleSpring1901(state, fm);

        Set<String> failing = Set.of("A Bud-Gal", "A War-Gal", "F Sev-Bla", "F Ank-Bla");
        Set<DiplomacyResult> expected = new HashSet<>();
        for (String o : SAMPLE_SPRING_1901)
            expected.add(result(state, powerIndex(o.substring(2).split("-")[0]), o, !failing.contains(o)));
        assertEquals(22, expected.size());
        assertEquals(expected, lastResults(state));

        Map<String, DiplomacyUnit> after = new HashMap<>();
        after.put("Tri", army(AUSTRIA));
        after.put("Bud", army(AUSTRIA));
        after.put("Alb", fleet(AUSTRIA));
        after.put("Yor", army(ENGLAND));
        after.put("Nth", fleet(ENGLAND));
        after.put("Nrg", fleet(ENGLAND));
        after.put("Bur", army(FRANCE));
        after.put("Spa", army(FRANCE));
        after.put("Pic", fleet(FRANCE));
        after.put("Kie", army(GERMANY));
        after.put("Ruh", army(GERMANY));
        after.put("Den", fleet(GERMANY));
        after.put("Pie", army(ITALY));
        after.put("Ven", army(ITALY));
        after.put("Ion", fleet(ITALY));
        after.put("Ukr", army(RUSSIA));
        after.put("War", army(RUSSIA));
        after.put("Bot", fleet(RUSSIA));
        after.put("Sev", fleet(RUSSIA));
        after.put("Bul", army(TURKEY));
        after.put("Con", army(TURKEY));
        after.put("Ank", fleet(TURKEY));
        for (DiplomacyProvince p : state.getMap().provinces())
            assertEquals("unit in " + p, after.get(p.name()), state.getUnit(p));

        // no retreats: straight to Fall 1901, Austria first, orders cleared, centres unchanged
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(1901, state.getYear());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertNoOrders(state);
        assertEquals(-1, state.getOwner(prov(state, "Bul")));
        assertEquals(-1, state.getOwner(prov(state, "Spa")));
        assertEquals(3, state.nCentres(TURKEY));
    }

    @Test
    public void sampleGameFall1901AndAdjustments() {
        Game game = newGame(12);
        DiplomacyGameState state = (DiplomacyGameState) game.getGameState();
        DiplomacyForwardModel fm = (DiplomacyForwardModel) game.getForwardModel();
        playSampleSpring1901(state, fm);

        // Fall 1901 (not the rulebook's): A Kie-Hol, F Alb-Gre, F Bot-Swe succeed into vacant centres;
        // French A Bur-Bel and F Pic-Bel stand off; everything else holds
        play(state, fm, "A Tri H", "A Bud H", "F Alb-Gre",
                "A Yor H", "F Nth H", "F Nrg H",
                "A Bur-Bel", "A Spa H", "F Pic-Bel",
                "A Kie-Hol", "A Ruh H", "F Den H",
                "A Pie H", "A Ven H", "F Ion H",
                "A Ukr H", "A War H", "F Bot-Swe", "F Sev H",
                "A Bul H", "A Con H", "F Ank H");
        assertEquals(22, totalUnits(state));
        assertNull(state.getUnit(prov(state, "Bel")));
        assertEquals(-1, state.getOwner(prov(state, "Bel")));
        // centres: Austria Bud, Tri, Vie (vacant, kept) + Gre = 4, 3 units -> +1, free Vie
        //          England Lon, Edi, Lvp (vacant, kept) = 3, 3 units -> 0
        //          France Par, Mar, Bre (vacant) + Spa = 4, 3 units -> +1, free Bre, Mar, Par
        //          Germany Ber, Kie, Mun (vacant: A Kie went to Hol) + Den + Hol = 5, 3 units -> +2, free all three
        //          Italy Rom, Nap (vacant), Ven = 3, 3 units -> 0
        //          Russia Mos, StP (vacant), War, Sev + Swe = 5, 4 units -> +1, free Mos, StP
        //          Turkey Ank, Con, Smy (vacant) + Bul = 4, 3 units -> +1, free Smy
        int[] centres = {4, 3, 4, 5, 3, 5, 4};
        int[] adjustment = {1, 0, 1, 2, 0, 1, 1};
        for (int p = 0; p < N_POWERS; p++) {
            assertEquals(POWER_NAMES[p], centres[p], state.nCentres(p));
            assertEquals(POWER_NAMES[p], adjustment[p], state.adjustment(p));
        }
        assertEquals(List.of(prov(state, "Vie")), state.freeHomeCentres(AUSTRIA));
        assertEquals(List.of(prov(state, "Ber"), prov(state, "Kie"), prov(state, "Mun")), state.freeHomeCentres(GERMANY));
        assertEquals(List.of(prov(state, "Mos"), prov(state, "StP")), state.freeHomeCentres(RUSSIA));
        assertEquals(List.of(prov(state, "Smy")), state.freeHomeCentres(TURKEY));
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(1901, state.getYear());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertTrue(state.isNotTerminal());

        // England and Italy have nothing to adjust and are skipped
        play(state, fm, "Build A Vie");
        assertEquals(FRANCE, state.getCurrentPlayer());
        play(state, fm, "Build F Bre", "Build A Mun", "Build F Kie");
        assertEquals(RUSSIA, state.getCurrentPlayer());
        play(state, fm, "Build F StP/nc", "Waive Turkey");

        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Vie")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Bre")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Mun")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Kie")));
        assertEquals(fleet(RUSSIA, "nc"), state.getUnit(prov(state, "StP")));
        assertNull(state.getUnit(prov(state, "Smy")));
        // 22 + 5 builds
        assertEquals(27, totalUnits(state));
        assertEquals(Set.of(result(state, AUSTRIA, "Build A Vie", true), result(state, FRANCE, "Build F Bre", true),
                result(state, GERMANY, "Build A Mun", true), result(state, GERMANY, "Build F Kie", true),
                result(state, RUSSIA, "Build F StP/nc", true), result(state, TURKEY, "Waive Turkey", true)),
                lastResults(state));
        assertNoOrders(state);
    }

    @Test
    public void eliminatedPowerIsSkippedInEveryPhase() {
        Game game = newGame(13);
        DiplomacyGameState state = (DiplomacyGameState) game.getGameState();
        DiplomacyForwardModel fm = (DiplomacyForwardModel) game.getForwardModel();
        // Austria has no units and no centres
        for (String c : List.of("Bud", "Vie", "Tri")) {
            state.setUnit(prov(state, c), null);
            state.setOwner(prov(state, c), -1);
        }
        state.setTurnOwner(ENGLAND);
        assertFalse(state.hasOrdersToGive(AUSTRIA));

        holdAll(state, fm);
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(ENGLAND, state.getCurrentPlayer());
        assertFalse(state.hasOrdersToGive(AUSTRIA));

        // Fall: Germany F Kie-Den gains Denmark (+1, Kie free); nobody else adjusts
        List<String> orders = new ArrayList<>();
        for (DiplomacyProvince p : state.getMap().provinces()) {
            DiplomacyUnit u = state.getUnit(p);
            if (u != null && !p.name().equals("Kie")) orders.add(u.type().letter + " " + p.name() + " H");
        }
        orders.add("F Kie-Den");
        play(state, fm, orders.toArray(new String[0]));
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(GERMANY, state.getCurrentPlayer());
        assertFalse(state.hasOrdersToGive(AUSTRIA));
        play(state, fm, "Build A Kie");

        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(ENGLAND, state.getCurrentPlayer());
        assertEquals(0.0, state.getGameScore(AUSTRIA), 0.0);
    }

    @Test
    public void englandTakesNorwayByConvoy() {
        // Spring 1901: F Lon-Nth, A Lvp-Yor; all else holds
        Game game = newGame(15);
        DiplomacyGameState state = (DiplomacyGameState) game.getGameState();
        DiplomacyForwardModel fm = (DiplomacyForwardModel) game.getForwardModel();
        play(state, fm, andHoldTheRest(state, "F Lon-Nth", "A Lvp-Yor"));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Yor")));

        // Fall 1901: A Yor-Nwy via convoy, F Nth C A Yor-Nwy (both checked legal by play); all else holds
        play(state, fm, andHoldTheRest(state, "A Yor-Nwy via convoy", "F Nth C A Yor-Nwy"));
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Nwy")));
        assertNull(state.getUnit(prov(state, "Yor")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertTrue(lastResults(state).contains(result(state, ENGLAND, "A Yor-Nwy via convoy", true)));
        assertTrue(lastResults(state).contains(result(state, ENGLAND, "F Nth C A Yor-Nwy", true)));
        // England: Edi, Lon, Lvp + Nwy = 4 centres for 3 units, the only power to adjust (Lon and Lvp free)
        assertEquals(ENGLAND, state.getOwner(prov(state, "Nwy")));
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(ENGLAND, state.getCurrentPlayer());
        play(state, fm, "Build A Lvp");
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Lvp")));
    }

    @Test
    public void supportedAttackRetreatIntoANeutralCentreAndBuilds() {
        // Spring 1901: A Par-Bur, A Mun-Ruh, A Ber-Mun (Mun vacated), F Kie-Den; all else holds. Nothing is
        // dislodged, so the Spring retreats are skipped.
        Game game = newGame(14);
        DiplomacyGameState state = (DiplomacyGameState) game.getGameState();
        DiplomacyForwardModel fm = (DiplomacyForwardModel) game.getForwardModel();
        play(state, fm, andHoldTheRest(state, "A Par-Bur", "A Mun-Ruh", "A Ber-Mun", "F Kie-Den"));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ruh")));

        // Fall 1901: A Ruh-Bur supported by A Mun: 2 vs A Bur's hold 1 - the French army is dislodged
        play(state, fm, andHoldTheRest(state, "A Ruh-Bur", "A Mun S A Ruh-Bur"));
        assertEquals(DiplomacyPhase.FALL_RETREATS, state.getPhase());
        assertEquals(1901, state.getYear());
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Bur")));
        assertEquals(army(FRANCE), state.getDislodged(prov(state, "Bur")));
        assertTrue(lastResults(state).contains(result(state, GERMANY, "A Mun S A Ruh-Bur", true)));
        // ownership waits for the end of the Fall turn: Den (German fleet) and Bel are still unowned
        assertEquals(-1, state.getOwner(prov(state, "Den")));
        assertEquals(-1, state.getOwner(prov(state, "Bel")));
        // A Bur: Bel, Gas, Par (vacated in Spring), Pic; Mar and Mun occupied, Ruh the attacker's origin
        assertEquals(orderSet(state, "A Bur R Bel", "A Bur R Gas", "A Bur R Par", "A Bur R Pic", "Disband Bur"),
                legalSet(state, fm));

        // the retreat into Bel counts for the Fall: France 4 centres (Bre, Mar, Par, Bel) for 3 units, Germany 4
        // (Ber, Kie, Mun, Den) for 3; nobody else changes
        play(state, fm, "A Bur R Bel");
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bel")));
        assertEquals(Set.of(result(state, FRANCE, "A Bur R Bel", true)), lastResults(state));
        assertEquals(FRANCE, state.getOwner(prov(state, "Bel")));
        assertEquals(GERMANY, state.getOwner(prov(state, "Den")));
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(1, state.adjustment(FRANCE));
        assertEquals(1, state.adjustment(GERMANY));
        for (int p : new int[]{AUSTRIA, ENGLAND, ITALY, RUSSIA, TURKEY})
            assertEquals(POWER_NAMES[p], 0, state.adjustment(p));
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(0, totalDislodged(state));
    }

    /** Seeded random games played to the end, checking the board invariants after every action and the results. */
    @Test
    public void randomGamesRunToTheEndKeepingTheInvariants() {
        final int LAST_YEAR = 1908;
        DiplomacyParameters params = new DiplomacyParameters();
        // long enough that random orders dislodge a unit with somewhere to retreat (convoy orders dilute the
        // supports among a random player's choices)
        params.setParameterValue("lastYear", LAST_YEAR);
        int springsChecked = 0, retreatPhases = 0;
        for (long seed = 1; seed <= 5; seed++) {
            Game game = newGame(seed, params);
            DiplomacyGameState state = (DiplomacyGameState) game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();
            Random rnd = new Random(seed);
            int steps = 0;
            DiplomacyPhase lastPhase = null;
            while (state.isNotTerminal() && steps++ < 5000) {
                DiplomacyPhase phase = state.getPhase();
                if (phase == DiplomacyPhase.SPRING_ORDERS && lastPhase != DiplomacyPhase.SPRING_ORDERS) {
                    springsChecked++;
                    for (int p = 0; p < N_POWERS; p++)
                        assertTrue(POWER_NAMES[p] + " starts " + state.getYear() + " with more units than centres",
                                state.nUnits(p) <= state.nCentres(p));
                }
                lastPhase = phase;
                if (phase.isRetreats()) {
                    retreatPhases++;
                    assertFalse("no unit to retreat for " + state.getCurrentPlayer(),
                            state.unitsToOrder(state.getCurrentPlayer()).isEmpty());
                } else {
                    assertEquals("dislodged units outside a retreat phase", 0, totalDislodged(state));
                }
                int unitsBefore = totalUnits(state) + totalDislodged(state);
                List<AbstractAction> actions = fm.computeAvailableActions(state);
                assertFalse("no actions for " + state.getCurrentPlayer() + " in " + phase + " " + state.getYear(),
                        actions.isEmpty());
                fm.next(state, actions.get(rnd.nextInt(actions.size())));
                // a dislodged unit is off the board until it retreats; units are only lost (disbanded) here
                if (phase != DiplomacyPhase.ADJUSTMENTS)
                    assertTrue("units added in " + phase,
                            totalUnits(state) + totalDislodged(state) <= unitsBefore);
                int centres = 0;
                for (int p = 0; p < N_POWERS; p++) centres += state.nCentres(p);
                assertTrue(centres <= 34);
                for (DiplomacyProvince p : state.getMap().provinces()) {
                    DiplomacyUnit u = state.getUnit(p);
                    if (u == null) continue;
                    if (u.isFleet()) assertNotEquals("fleet in " + p, DiplomacyProvince.Type.LAND, p.type());
                    else assertNotEquals("army in " + p, DiplomacyProvince.Type.SEA, p.type());
                }
            }
            assertFalse("game " + seed + " did not end within 5000 actions", state.isNotTerminal());

            // results oracle: 18+ centres wins outright; otherwise (the game ended at lastYear) most centres win,
            // shared top place draws
            int best = 0;
            for (int p = 0; p < N_POWERS; p++) best = Math.max(best, state.nCentres(p));
            int nBest = 0;
            for (int p = 0; p < N_POWERS; p++) if (state.nCentres(p) == best) nBest++;
            if (best < 18) assertEquals(LAST_YEAR, state.getYear());
            for (int p = 0; p < N_POWERS; p++) {
                CoreConstants.GameResult expected = state.nCentres(p) < best ? CoreConstants.GameResult.LOSE_GAME
                        : nBest > 1 ? CoreConstants.GameResult.DRAW_GAME : CoreConstants.GameResult.WIN_GAME;
                assertEquals("game " + seed + " " + POWER_NAMES[p], expected, state.getPlayerResults()[p]);
            }
        }
        // each game has a Spring in every year from 1901
        assertTrue(springsChecked >= 5 * (LAST_YEAR - 1900));
        // random supports dislodge units now and then: the retreat phases must have been played
        assertTrue("no retreat phase in 5 random games", retreatPhases > 0);
    }
}

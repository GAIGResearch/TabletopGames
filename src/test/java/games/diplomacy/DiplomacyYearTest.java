package games.diplomacy;

import core.CoreConstants;
import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The end of the year: supply centre ownership after the Fall turn (rulebook p.17), the adjustment phase (builds,
 * waivers and disbands, p.17-18), the end of the game (18 centres, or DiplomacyParameters.lastYear), and the next
 * year's Spring.
 */
public class DiplomacyYearTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
    }

    private Set<AbstractAction> actions() {
        List<AbstractAction> list = fm.computeAvailableActions(state);
        Set<AbstractAction> set = new HashSet<>(list);
        assertEquals("duplicate actions in " + list, list.size(), set.size());
        return set;
    }

    private Set<AbstractAction> orders(String... texts) {
        Set<AbstractAction> set = new HashSet<>();
        for (String t : texts) set.add(order(state, t));
        return set;
    }

    private List<DiplomacyProvince> provinces(String... names) {
        return Arrays.stream(names).map(n -> prov(state, n)).toList();
    }

    // ---------------------------------------------------------------- supply centres

    @Test
    public void afterFallOccupiedCentresChangeHandsAndVacantOnesKeepTheirOwner() {
        clearBoard(state);
        place(state, FRANCE, "A Bur", "A Pic");
        place(state, GERMANY, "A Ber");
        startPhase(state, DiplomacyPhase.FALL_ORDERS, FRANCE);
        // A Bur-Mun takes Germany's vacant Munich, A Pic-Bel takes neutral Belgium; Germany holds Berlin
        play(state, fm, "A Bur-Mun", "A Pic-Bel", "A Ber H");
        assertEquals(FRANCE, state.getOwner(prov(state, "Mun")));
        assertEquals(FRANCE, state.getOwner(prov(state, "Bel")));
        assertEquals(GERMANY, state.getOwner(prov(state, "Ber")));
        // vacant centres keep their owners: Kie (Germany), Par/Mar/Bre (France), Vie (Austria), Bul (nobody)
        assertEquals(GERMANY, state.getOwner(prov(state, "Kie")));
        assertEquals(FRANCE, state.getOwner(prov(state, "Bre")));
        assertEquals(AUSTRIA, state.getOwner(prov(state, "Vie")));
        assertEquals(-1, state.getOwner(prov(state, "Bul")));
        // France 3 + Mun + Bel = 5, Germany 3 - Mun = 2
        assertEquals(5, state.nCentres(FRANCE));
        assertEquals(2, state.nCentres(GERMANY));
        assertEquals(5.0, state.getGameScore(FRANCE), 0.0);
    }

    @Test
    public void springVisitThatLeavesInFallDoesNotTakeTheCentre() {
        clearBoard(state);
        place(state, FRANCE, "A Pic");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        play(state, fm, "A Pic-Bel");
        // ownership is not updated after Spring
        assertEquals(-1, state.getOwner(prov(state, "Bel")));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        play(state, fm, "A Bel-Hol");
        assertEquals(-1, state.getOwner(prov(state, "Bel")));
        assertEquals(FRANCE, state.getOwner(prov(state, "Hol")));
    }

    @Test
    public void centreOccupiedInSpringAndStillOccupiedInFallChangesHandsOnlyAfterFall() {
        clearBoard(state);
        place(state, RUSSIA, "A Ukr");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, RUSSIA);
        play(state, fm, "A Ukr-Rum");
        assertEquals(-1, state.getOwner(prov(state, "Rum")));
        play(state, fm, "A Rum H");
        assertEquals(RUSSIA, state.getOwner(prov(state, "Rum")));
    }

    @Test
    public void otherLandProvincesChangeHandsAfterFallButAreNotCentres() {
        clearBoard(state);
        place(state, FRANCE, "A Bur", "F Bre");
        place(state, GERMANY, "A Sil");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        play(state, fm, "A Bur-Ruh", "F Bre-Eng", "A Sil H");
        // not after Spring
        assertEquals(GERMANY, state.getOwner(prov(state, "Ruh")));
        play(state, fm, "A Ruh H", "F Eng-Pic", "A Sil-Boh");
        assertEquals(FRANCE, state.getOwner(prov(state, "Ruh")));
        assertEquals(GERMANY, state.getOwner(prov(state, "Boh")));
        // Picardy was France's already; vacated Burgundy and Silesia keep their controllers; seas have none
        assertEquals(FRANCE, state.getOwner(prov(state, "Pic")));
        assertEquals(FRANCE, state.getOwner(prov(state, "Bur")));
        assertEquals(GERMANY, state.getOwner(prov(state, "Sil")));
        assertEquals(-1, state.getOwner(prov(state, "Eng")));
        assertEquals(3, state.nCentres(FRANCE));
        assertEquals(3, state.nCentres(GERMANY));
        assertEquals(3, state.nCentres(AUSTRIA));
    }

    // ---------------------------------------------------------------- entering and skipping adjustments

    @Test
    public void adjustmentsAreSkippedWhenNobodyHasABuildOrDisbandToOrder() {
        // full board, everyone holds through Fall: units equal centres for every power
        startPhase(state, DiplomacyPhase.FALL_ORDERS, AUSTRIA);
        holdAll(state, fm);
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(22, totalUnits(state));
    }

    @Test
    public void aPowerEntitledToBuildsWithNoFreeHomeCentreHasNoDecision() {
        // Austria gains Serbia but Bud, Vie, Tri are all occupied by its own units: +1, but nowhere to build
        own(state, AUSTRIA, "Ser");
        assertEquals(1, state.adjustment(AUSTRIA));
        assertEquals(List.of(), state.freeHomeCentres(AUSTRIA));
        startPhase(state, DiplomacyPhase.FALL_ORDERS, AUSTRIA);
        holdAll(state, fm);
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
    }

    @Test
    public void aPowerThatLostAllItsHomeCentresCannotBuild() {
        // p.18: Austria holds Ser, Gre, Rum with one army, its home centres are Italy's: +2 but it cannot build
        for (String c : List.of("Bud", "Vie", "Tri")) state.setUnit(prov(state, c), null);
        own(state, ITALY, "Bud", "Vie", "Tri");
        own(state, AUSTRIA, "Ser", "Gre", "Rum");
        place(state, AUSTRIA, "A Ser");
        assertEquals(2, state.adjustment(AUSTRIA));
        assertEquals(List.of(), state.freeHomeCentres(AUSTRIA));
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, AUSTRIA);
        assertFalse(state.hasOrdersToGive(AUSTRIA));
        // Italy: 6 centres, 3 units, its home centres Rom, Ven, Nap all occupied: no decision either
        assertFalse(state.hasOrdersToGive(ITALY));

        startPhase(state, DiplomacyPhase.FALL_ORDERS, AUSTRIA);
        holdAll(state, fm);
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(1, state.nUnits(AUSTRIA));
    }

    @Test
    public void fallWithAGainLeadsToAdjustmentsForTheGainingPower() {
        // Germany F Kie-Den takes Denmark; Kiel left vacant stays German: 4 centres, 3 units, Kie free
        startPhase(state, DiplomacyPhase.FALL_ORDERS, AUSTRIA);
        List<String> orders = new ArrayList<>();
        for (DiplomacyProvince p : state.getMap().provinces()) {
            DiplomacyUnit u = state.getUnit(p);
            if (u != null && !p.name().equals("Kie")) orders.add(u.type().letter + " " + p.name() + " H");
        }
        orders.add("F Kie-Den");
        play(state, fm, orders.toArray(new String[0]));
        assertEquals(DiplomacyPhase.ADJUSTMENTS, state.getPhase());
        assertEquals(1901, state.getYear());
        assertEquals(GERMANY, state.getCurrentPlayer());
        assertEquals(1, state.adjustment(GERMANY));
        assertEquals(List.of(prov(state, "Kie")), state.freeHomeCentres(GERMANY));
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], p == GERMANY, state.hasOrdersToGive(p));
        assertEquals(orders("Build A Kie", "Build F Kie", "Waive Germany"), actions());

        play(state, fm, "Build F Kie");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Kie")));
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(23, totalUnits(state));
        assertEquals(Set.of(result(state, GERMANY, "Build F Kie", true)), lastResults(state));
        assertNoOrders(state);
    }

    // ---------------------------------------------------------------- builds

    @Test
    public void buildsAreOnlyInUnoccupiedHomeCentresStillControlled() {
        // p.17 example: Marseilles is Italy's and France has a unit in Brest: France may build only in Paris
        clearBoard(state);
        own(state, ITALY, "Mar");
        own(state, FRANCE, "Spa", "Por");
        place(state, FRANCE, "A Bre", "A Spa");
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, FRANCE);
        // France: Par, Bre, Spa, Por = 4 centres, 2 units
        assertEquals(2, state.adjustment(FRANCE));
        assertEquals(List.of(prov(state, "Par")), state.freeHomeCentres(FRANCE));
        assertTrue(state.hasOrdersToGive(FRANCE));
        // Paris is inland: an army only
        assertEquals(orders("Build A Par", "Waive France"), actions());

        // after building in Paris France has no free home centre left, though entitled to 2: the turn passes
        // to Germany (3 centres, no units, free Ber, Kie, Mun)
        fm.next(state, order(state, "Build A Par"));
        assertEquals(List.of(), state.freeHomeCentres(FRANCE));
        assertFalse(state.hasOrdersToGive(FRANCE));
        assertEquals(GERMANY, state.getCurrentPlayer());
        // nothing is built until the phase is resolved
        assertNull(state.getUnit(prov(state, "Par")));
        assertEquals(List.of(order(state, "Build A Par")), state.getOrders(FRANCE));
    }

    @Test
    public void coastalHomeCentreOffersArmyOrFleetAndStPetersburgAFleetOnEitherCoast() {
        clearBoard(state);
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, RUSSIA);
        // Russia: 4 centres, no units: Mos, Sev, StP, War free
        assertEquals(4, state.adjustment(RUSSIA));
        assertEquals(provinces("Mos", "Sev", "StP", "War"), state.freeHomeCentres(RUSSIA));
        assertEquals(orders("Build A Mos", "Build A Sev", "Build F Sev", "Build A StP", "Build F StP/nc",
                "Build F StP/sc", "Build A War", "Waive Russia"), actions());

        startPhase(state, DiplomacyPhase.ADJUSTMENTS, FRANCE);
        assertEquals(orders("Build A Bre", "Build F Bre", "Build A Mar", "Build F Mar", "Build A Par", "Waive France"),
                actions());
    }

    @Test
    public void powerBuildsOneAtATimeInTheCentresStillFree() {
        clearBoard(state);
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, FRANCE);
        fm.next(state, order(state, "Build F Bre"));
        // France entitled to 3, 1 ordered: still France, Brest no longer offered
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(provinces("Mar", "Par"), state.freeHomeCentres(FRANCE));
        assertEquals(orders("Build A Mar", "Build F Mar", "Build A Par", "Waive France"), actions());
        fm.next(state, order(state, "Build A Par"));
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(orders("Build A Mar", "Build F Mar", "Waive France"), actions());
        fm.next(state, order(state, "Build F Mar"));
        // 3 builds: done, Germany next
        assertFalse(state.hasOrdersToGive(FRANCE));
        assertEquals(GERMANY, state.getCurrentPlayer());
    }

    @Test
    public void waivingGivesUpTheRemainingBuilds() {
        // full board without France's Bre and Mar units: France +2 with Bre and Mar free; nobody else adjusts
        state.setUnit(prov(state, "Bre"), null);
        state.setUnit(prov(state, "Mar"), null);
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, FRANCE);
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], p == FRANCE, state.hasOrdersToGive(p));
        play(state, fm, "Build F Bre", "Waive France");
        // resolved: one fleet built, Marseilles left empty, next year
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Bre")));
        assertNull(state.getUnit(prov(state, "Mar")));
        assertEquals(2, state.nUnits(FRANCE));
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(Set.of(result(state, FRANCE, "Build F Bre", true), result(state, FRANCE, "Waive France", true)),
                lastResults(state));
    }

    @Test
    public void waivingBeforeAnyBuildGivesUpThemAll() {
        // France +2 with Bre and Mar free; its first order is the waiver, so it builds nothing although it has
        // given fewer orders (1) than the builds it was entitled to (2)
        state.setUnit(prov(state, "Bre"), null);
        state.setUnit(prov(state, "Mar"), null);
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, FRANCE);
        play(state, fm, "Waive France");
        assertNull(state.getUnit(prov(state, "Bre")));
        assertNull(state.getUnit(prov(state, "Mar")));
        assertEquals(1, state.nUnits(FRANCE));
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
    }

    @Test
    public void buildsOfSeveralPowersAreAppliedTogetherWhenAllAreDone() {
        // full board without France's A Par and Russia's F StP: France +1 (Par), Russia +1 (StP)
        state.setUnit(prov(state, "Par"), null);
        state.setUnit(prov(state, "StP"), null);
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, FRANCE);
        fm.next(state, order(state, "Build A Par"));
        assertEquals(RUSSIA, state.getCurrentPlayer());
        assertNull(state.getUnit(prov(state, "Par")));
        fm.next(state, order(state, "Build F StP/nc"));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Par")));
        assertEquals(fleet(RUSSIA, "nc"), state.getUnit(prov(state, "StP")));
        assertEquals(22, totalUnits(state));
        assertEquals(Set.of(result(state, FRANCE, "Build A Par", true), result(state, RUSSIA, "Build F StP/nc", true)),
                lastResults(state));
        assertNoOrders(state);
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
    }

    // ---------------------------------------------------------------- disbands

    @Test
    public void powerWithMoreUnitsThanCentresChoosesWhichToDisband() {
        // full board; France has taken Munich: Germany 2 centres, 3 units, must disband 1.
        // France 4 centres, 3 units, but its home centres are all occupied: no decision.
        own(state, FRANCE, "Mun");
        assertEquals(-1, state.adjustment(GERMANY));
        assertEquals(1, state.adjustment(FRANCE));
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, GERMANY);
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], p == GERMANY, state.hasOrdersToGive(p));
        assertEquals(orders("Disband Ber", "Disband Kie", "Disband Mun"), actions());
        play(state, fm, "Disband Mun");
        assertNull(state.getUnit(prov(state, "Mun")));
        assertEquals(2, state.nUnits(GERMANY));
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        assertEquals(Set.of(result(state, GERMANY, "Disband Mun", true)), lastResults(state));
    }

    @Test
    public void disbandsAreOrderedOneAtATimeAndAppliedAtTheEnd() {
        // Germany keeps only Kiel: 1 centre, 3 units, must disband 2
        own(state, FRANCE, "Mun", "Ber");
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, GERMANY);
        assertEquals(-2, state.adjustment(GERMANY));
        fm.next(state, order(state, "Disband Ber"));
        assertEquals(GERMANY, state.getCurrentPlayer());
        assertTrue(state.hasOrdersToGive(GERMANY));
        assertEquals(orders("Disband Kie", "Disband Mun"), actions());
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        fm.next(state, order(state, "Disband Kie"));
        assertNull(state.getUnit(prov(state, "Ber")));
        assertNull(state.getUnit(prov(state, "Kie")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Mun")));
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
    }

    // ---------------------------------------------------------------- end of the game

    /** France controls 17 centres and has an army in Burgundy next to neutral, vacant Belgium; no other units. */
    private void franceOnSeventeen() {
        clearBoard(state);
        own(state, FRANCE, "Par", "Mar", "Bre", "Spa", "Por", "Hol", "Den", "Kie", "Ber", "Mun", "Lon", "Lvp", "Edi",
                "Nwy", "Swe", "Tun", "Rom");
        place(state, FRANCE, "A Bur");
        assertEquals(17, state.nCentres(FRANCE));
    }

    @Test
    public void eighteenCentresAfterFallWinsTheGame() {
        franceOnSeventeen();
        startPhase(state, DiplomacyPhase.FALL_ORDERS, FRANCE);
        play(state, fm, "A Bur-Bel");
        assertEquals(18, state.nCentres(FRANCE));
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], p == FRANCE ? CoreConstants.GameResult.WIN_GAME : CoreConstants.GameResult.LOSE_GAME,
                    state.getPlayerResults()[p]);
    }

    @Test
    public void seventeenCentresAfterFallDoNotWin() {
        // as above, but German A Ruh-Bel stands France off: France stays on 17
        franceOnSeventeen();
        place(state, GERMANY, "A Ruh");
        startPhase(state, DiplomacyPhase.FALL_ORDERS, FRANCE);
        play(state, fm, "A Bur-Bel", "A Ruh-Bel");
        assertEquals(17, state.nCentres(FRANCE));
        assertTrue(state.isNotTerminal());
    }

    @Test
    public void gameIsNotWonAfterSpring() {
        franceOnSeventeen();
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        play(state, fm, "A Bur-Bel");
        // ownership is not updated after Spring, and the game goes on
        assertEquals(17, state.nCentres(FRANCE));
        assertTrue(state.isNotTerminal());
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void gameEndsAfterFallOfTheLastYearWithTheMostCentresWinning() {
        DiplomacyParameters params = new DiplomacyParameters();
        params.setParameterValue("lastYear", 1901);
        state = newState(params);
        startPhase(state, DiplomacyPhase.FALL_ORDERS, AUSTRIA);
        holdAll(state, fm);
        // nothing changes: Russia 4 centres, everyone else 3
        assertFalse(state.isNotTerminal());
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], p == RUSSIA ? CoreConstants.GameResult.WIN_GAME : CoreConstants.GameResult.LOSE_GAME,
                    state.getPlayerResults()[p]);
    }

    @Test
    public void tiedPowersDrawAtTheLastYear() {
        DiplomacyParameters params = new DiplomacyParameters();
        params.setParameterValue("lastYear", 1901);
        state = newState(params);
        own(state, AUSTRIA, "Ser");
        startPhase(state, DiplomacyPhase.FALL_ORDERS, AUSTRIA);
        holdAll(state, fm);
        // Austria 3 + Ser = 4 (vacant, kept), Russia 4: both top, a draw; the rest lose
        assertFalse(state.isNotTerminal());
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], p == RUSSIA || p == AUSTRIA ? CoreConstants.GameResult.DRAW_GAME
                    : CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[p]);
    }

    @Test
    public void gameDoesNotEndAfterSpringOfTheLastYearOrBeforeTheLastYear() {
        DiplomacyParameters params = new DiplomacyParameters();
        params.setParameterValue("lastYear", 1902);
        state = newState(params);
        startPhase(state, DiplomacyPhase.FALL_ORDERS, AUSTRIA);
        holdAll(state, fm);
        // Fall 1901 < lastYear 1902: goes on
        assertTrue(state.isNotTerminal());
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(1902, state.getYear());
        holdAll(state, fm);
        // Spring of the last year: goes on
        assertTrue(state.isNotTerminal());
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        holdAll(state, fm);
        assertFalse(state.isNotTerminal());
    }
}

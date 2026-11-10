package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Resolving an orders phase of holds and moves (all units strength 1, so nothing is dislodged): standoffs, blocked
 * chains, no swapping, rotation (rulebook p.6-7, Diagrams 4-7), and what the resolution leaves behind (results,
 * orders cleared, the next phase). Each test clears the board and places only the units it needs, in Spring 1901.
 */
public class DiplomacyResolutionTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
        clearBoard(state);
    }

    @Test
    public void twoMovesIntoOneProvinceBothFail() {
        // Diagram 4: A Ber-Sil (Germany), A War-Sil (Russia): neither moves, Sil stays vacant
        place(state, GERMANY, "A Ber");
        place(state, RUSSIA, "A War");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        play(state, fm, "A Ber-Sil", "A War-Sil");
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "War")));
        assertNull(state.getUnit(prov(state, "Sil")));
        assertEquals(Set.of(result(state, GERMANY, "A Ber-Sil", false), result(state, RUSSIA, "A War-Sil", false)),
                lastResults(state));
    }

    @Test
    public void standoffDoesNotDislodgeAUnitHoldingThere() {
        // Diagram 4 with a unit holding in Silesia (rule 4): it stays, the attackers stay home, nothing dislodged
        place(state, GERMANY, "A Ber");
        place(state, RUSSIA, "A War");
        place(state, AUSTRIA, "A Sil");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, AUSTRIA);
        play(state, fm, "A Sil H", "A Ber-Sil", "A War-Sil");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Sil")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "War")));
        assertNull(state.getDislodged(prov(state, "Sil")));
        assertEquals(Set.of(result(state, AUSTRIA, "A Sil H", true), result(state, GERMANY, "A Ber-Sil", false),
                result(state, RUSSIA, "A War-Sil", false)), lastResults(state));
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
    }

    @Test
    public void twoMovesOfOnePowerIntoOneProvinceAlsoStandOff() {
        // the standoff rule applies whoever the units belong to
        place(state, FRANCE, "A Bur", "F Pic");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        play(state, fm, "A Bur-Bel", "F Pic-Bel");
        assertNull(state.getUnit(prov(state, "Bel")));
        assertEquals(army(FRANCE), state.getUnit(prov(state, "Bur")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Pic")));
    }

    @Test
    public void moveIntoAHoldingUnitFailsAndBlocksTheChainBehindIt() {
        // Diagram 5: F Kie-Ber, A Ber-Pru (Germany), A Pru holds (Russia): nothing moves
        place(state, GERMANY, "F Kie", "A Ber");
        place(state, RUSSIA, "A Pru");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        play(state, fm, "F Kie-Ber", "A Ber-Pru", "A Pru H");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Kie")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Pru")));
        assertEquals(Set.of(result(state, GERMANY, "F Kie-Ber", false), result(state, GERMANY, "A Ber-Pru", false),
                result(state, RUSSIA, "A Pru H", true)), lastResults(state));
    }

    @Test
    public void bouncedMoveAlsoBlocksTheChainBehindIt() {
        // A Ber-Sil and A War-Sil stand off, so Ber is not vacated and F Kie-Ber fails too
        place(state, GERMANY, "F Kie", "A Ber");
        place(state, RUSSIA, "A War");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        play(state, fm, "F Kie-Ber", "A Ber-Sil", "A War-Sil");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Kie")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "War")));
        assertNull(state.getUnit(prov(state, "Sil")));
        assertEquals(Set.of(result(state, GERMANY, "F Kie-Ber", false), result(state, GERMANY, "A Ber-Sil", false),
                result(state, RUSSIA, "A War-Sil", false)), lastResults(state));
    }

    @Test
    public void twoUnitsCannotSwapPlaces() {
        // Diagram 6: F Ber-Pru, A Pru-Ber (both Germany): neither moves
        place(state, GERMANY, "F Ber", "A Pru");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        play(state, fm, "F Ber-Pru", "A Pru-Ber");
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Ber")));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Pru")));
        assertEquals(Set.of(result(state, GERMANY, "F Ber-Pru", false), result(state, GERMANY, "A Pru-Ber", false)),
                lastResults(state));
    }

    @Test
    public void threeUnitsRotate() {
        // Diagram 7: A Hol-Bel, F Bel-Nth (England), F Nth-Hol (France): no two units trade places, all succeed
        place(state, ENGLAND, "A Hol", "F Bel");
        place(state, FRANCE, "F Nth");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, ENGLAND);
        play(state, fm, "A Hol-Bel", "F Bel-Nth", "F Nth-Hol");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Hol")));
        assertEquals(3, totalUnits(state));
        assertEquals(Set.of(result(state, ENGLAND, "A Hol-Bel", true), result(state, ENGLAND, "F Bel-Nth", true),
                result(state, FRANCE, "F Nth-Hol", true)), lastResults(state));
    }

    @Test
    public void rotationWithAStandoffInItFailsEntirely() {
        // Diagram 7 plus German F Hel-Hol: Hol is attacked by F Nth and F Hel, a standoff, so F Nth stays,
        // F Bel-Nth fails, so A Hol-Bel fails too: nothing moves
        place(state, ENGLAND, "A Hol", "F Bel");
        place(state, FRANCE, "F Nth");
        place(state, GERMANY, "F Hel");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, ENGLAND);
        play(state, fm, "A Hol-Bel", "F Bel-Nth", "F Nth-Hol", "F Hel-Hol");
        assertEquals(army(ENGLAND), state.getUnit(prov(state, "Hol")));
        assertEquals(fleet(ENGLAND), state.getUnit(prov(state, "Bel")));
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Nth")));
        assertEquals(fleet(GERMANY), state.getUnit(prov(state, "Hel")));
        assertEquals(Set.of(result(state, ENGLAND, "A Hol-Bel", false), result(state, ENGLAND, "F Bel-Nth", false),
                result(state, FRANCE, "F Nth-Hol", false), result(state, GERMANY, "F Hel-Hol", false)),
                lastResults(state));
    }

    @Test
    public void moveIntoAProvinceVacatedThisTurnSucceeds() {
        // sample game: A Vie-Tri succeeds because F Tri-Alb leaves
        place(state, AUSTRIA, "A Vie", "F Tri");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, AUSTRIA);
        play(state, fm, "A Vie-Tri", "F Tri-Alb");
        assertNull(state.getUnit(prov(state, "Vie")));
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Tri")));
        assertEquals(fleet(AUSTRIA), state.getUnit(prov(state, "Alb")));
        assertEquals(Set.of(result(state, AUSTRIA, "A Vie-Tri", true), result(state, AUSTRIA, "F Tri-Alb", true)),
                lastResults(state));
    }

    @Test
    public void moveIntoAUnitThatFailsToLeaveFails() {
        // A Vie-Tri, F Tri-Ven, and Italian A Ven holds: F Tri bounces off Ven, so A Vie-Tri fails
        place(state, AUSTRIA, "A Vie", "F Tri");
        place(state, ITALY, "A Ven");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, AUSTRIA);
        play(state, fm, "A Vie-Tri", "F Tri-Ven", "A Ven H");
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Vie")));
        assertEquals(fleet(AUSTRIA), state.getUnit(prov(state, "Tri")));
        assertEquals(army(ITALY), state.getUnit(prov(state, "Ven")));
    }

    @Test
    public void fleetEnteringASplitCoastStaysOnThatCoastAndMovesFromIt() {
        place(state, FRANCE, "F Mid");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        play(state, fm, "F Mid-Spa/sc");
        assertEquals(fleet(FRANCE, "sc"), state.getUnit(prov(state, "Spa")));
        assertNull(state.getUnit(prov(state, "Mid")));
        // in Fall it is offered the south coast's moves
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(FRANCE, state.getCurrentPlayer());
        assertEquals(Set.of(order(state, "F Spa H"), order(state, "F Spa-GoL"), order(state, "F Spa-Mar"),
                        order(state, "F Spa-Mid"), order(state, "F Spa-Por"), order(state, "F Spa-Wes")),
                new java.util.HashSet<>(fm.computeAvailableActions(state)));
        // leaving it, the fleet has no coast
        play(state, fm, "F Spa-Mar");
        assertEquals(fleet(FRANCE), state.getUnit(prov(state, "Mar")));
    }

    @Test
    public void resolutionClearsTheOrdersAndRecordsEveryOrderOnce() {
        place(state, GERMANY, "A Ber", "A Mun");
        place(state, RUSSIA, "A War");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        play(state, fm, "A Ber-Sil", "A Mun H", "A War-Pru");
        assertNoOrders(state);
        // Ber-Sil and War-Pru go to different provinces: both succeed; the hold succeeds
        assertEquals(Set.of(result(state, GERMANY, "A Ber-Sil", true), result(state, GERMANY, "A Mun H", true),
                result(state, RUSSIA, "A War-Pru", true)), lastResults(state));
        assertEquals(army(GERMANY), state.getUnit(prov(state, "Sil")));
        assertEquals(army(RUSSIA), state.getUnit(prov(state, "Pru")));
        assertNull(state.getUnit(prov(state, "Ber")));
        assertEquals(3, totalUnits(state));
        // Spring with nobody dislodged goes straight to Fall, Germany first
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(1901, state.getYear());
        assertEquals(GERMANY, state.getCurrentPlayer());
        // the next phase's results replace these
        play(state, fm, "A Mun H", "A Sil H", "A Pru H");
        assertEquals(Set.of(result(state, GERMANY, "A Sil H", true), result(state, GERMANY, "A Mun H", true),
                result(state, RUSSIA, "A Pru H", true)), lastResults(state));
    }
}

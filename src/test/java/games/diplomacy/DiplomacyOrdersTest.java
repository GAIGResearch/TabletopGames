package games.diplomacy;

import core.actions.AbstractAction;
import games.diplomacy.actions.DiplomacyOrder;
import games.diplomacy.actions.Hold;
import games.diplomacy.actions.Move;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The orders phases: which unit is ordered next, the Hold and Move orders it is offered, and how the turn passes.
 * Exact action sets are checked on boards with a single unit, where no support or convoy order is offered.
 */
public class DiplomacyOrdersTest {

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

    @Test
    public void unitsToOrderAreThePowersUnorderedUnitsInProvinceOrder() {
        // province index order is the map file's (alphabetical) order
        assertEquals(List.of(prov(state, "Bud"), prov(state, "Tri"), prov(state, "Vie")), state.unitsToOrder(AUSTRIA));
        assertEquals(List.of(prov(state, "Mos"), prov(state, "Sev"), prov(state, "StP"), prov(state, "War")),
                state.unitsToOrder(RUSSIA));
        for (int p = 0; p < N_POWERS; p++)
            assertTrue(POWER_NAMES[p], state.hasOrdersToGive(p));

        fm.next(state, order(state, "A Bud H"));
        assertEquals(List.of(prov(state, "Tri"), prov(state, "Vie")), state.unitsToOrder(AUSTRIA));
        assertTrue(state.hasOrdersToGive(AUSTRIA));
        fm.next(state, order(state, "F Tri-Alb"));
        fm.next(state, order(state, "A Vie-Tri"));
        assertEquals(List.of(), state.unitsToOrder(AUSTRIA));
        assertFalse(state.hasOrdersToGive(AUSTRIA));
        // the orders are recorded, in the order given
        assertEquals(List.of(order(state, "A Bud H"), order(state, "F Tri-Alb"), order(state, "A Vie-Tri")),
                state.getOrders(AUSTRIA));
    }

    @Test
    public void aPowerWithNoUnitsHasNoOrdersToGive() {
        clearBoard(state);
        place(state, GERMANY, "A Ber");
        for (int p = 0; p < N_POWERS; p++) {
            assertEquals(POWER_NAMES[p], p == GERMANY, state.hasOrdersToGive(p));
            assertEquals(POWER_NAMES[p], p == GERMANY ? List.of(prov(state, "Ber")) : List.of(), state.unitsToOrder(p));
        }
    }

    @Test
    public void firstUnitOfTheCurrentPowerIsTheOneOffered() {
        // full board: Austria's first unit in province order is A Bud; every action concerns it, Hold included
        // (only the unit is checked here, as support orders between neighbouring units are offered too)
        List<AbstractAction> list = fm.computeAvailableActions(state);
        assertFalse(list.isEmpty());
        assertEquals(new Hold(prov(state, "Bud")), list.get(0));
        for (AbstractAction a : list)
            assertEquals(a.toString(), prov(state, "Bud"), ((DiplomacyOrder) a).province());
        assertTrue(list.containsAll(orders("A Bud-Gal", "A Bud-Rum", "A Bud-Ser", "A Bud-Tri", "A Bud-Vie")));

        fm.next(state, order(state, "A Bud H"));
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        list = fm.computeAvailableActions(state);
        for (AbstractAction a : list)
            assertEquals(a.toString(), prov(state, "Tri"), ((DiplomacyOrder) a).province());
        assertTrue(list.containsAll(orders("F Tri H", "F Tri-Adr", "F Tri-Alb", "F Tri-Ven")));
    }

    @Test
    public void armyIsOfferedHoldAndAMoveToEachAdjacentLandOrCoastalProvince() {
        clearBoard(state);
        place(state, FRANCE, "A Par");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        // Diagram 1
        assertEquals(orders("A Par H", "A Par-Bre", "A Par-Pic", "A Par-Bur", "A Par-Gas"), actions());
        assertEquals(new Hold(prov(state, "Par")), fm.computeAvailableActions(state).get(0));
    }

    @Test
    public void coastalArmyIsNotOfferedTheSea() {
        clearBoard(state);
        place(state, TURKEY, "A Con");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, TURKEY);
        // Con borders Aeg, Ank, Bla, Bul, Smy; the army may enter Bul without a coast
        assertEquals(orders("A Con H", "A Con-Ank", "A Con-Bul", "A Con-Smy"), actions());
    }

    @Test
    public void fleetIsOfferedHoldAndItsFleetMovesWithCoastsNamed() {
        clearBoard(state);
        place(state, TURKEY, "F Con");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, TURKEY);
        assertEquals(orders("F Con H", "F Con-Aeg", "F Con-Ank", "F Con-Bla", "F Con-Bul/ec", "F Con-Bul/sc", "F Con-Smy"),
                actions());
    }

    @Test
    public void fleetOnASplitCoastMovesFromThatCoastOnly() {
        clearBoard(state);
        place(state, FRANCE, "F Spa/nc");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, FRANCE);
        // p.5: not Wes, GoL or Mar from the north coast
        assertEquals(orders("F Spa H", "F Spa-Gas", "F Spa-Mid", "F Spa-Por"), actions());

        clearBoard(state);
        place(state, RUSSIA, "F StP/sc");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, RUSSIA);
        assertEquals(orders("F StP H", "F StP-Bot", "F StP-Fin", "F StP-Lvn"), actions());
    }

    @Test
    public void fleetInTheEnglishChannelIsOfferedEightMoves() {
        clearBoard(state);
        place(state, ENGLAND, "F Eng");
        startPhase(state, DiplomacyPhase.FALL_ORDERS, ENGLAND);
        // Diagram 2, in a Fall orders phase
        assertEquals(orders("F Eng H", "F Eng-Iri", "F Eng-Wal", "F Eng-Lon", "F Eng-Nth", "F Eng-Bel", "F Eng-Pic",
                "F Eng-Bre", "F Eng-Mid"), actions());
    }

    @Test
    public void powerKeepsTheTurnUntilAllItsUnitsAreOrderedThenItPassesOn() {
        // full board: Austria orders Bud, Tri, Vie, then England (1) has the turn with its first unit, F Edi
        play(state, fm, "A Bud H", "F Tri H");
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        play(state, fm, "A Vie H");
        assertEquals(ENGLAND, state.getCurrentPlayer());
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(List.of(prov(state, "Edi"), prov(state, "Lon"), prov(state, "Lvp")), state.unitsToOrder(ENGLAND));
        for (AbstractAction a : fm.computeAvailableActions(state))
            assertEquals(a.toString(), prov(state, "Edi"), ((DiplomacyOrder) a).province());
        // orders stay recorded until the phase is resolved; units do not move yet
        assertEquals(3, state.getOrders(AUSTRIA).size());
        assertEquals(army(AUSTRIA), state.getUnit(prov(state, "Bud")));
    }

    @Test
    public void turnPassesCyclicallySkippingPowersWithNothingToOrder() {
        clearBoard(state);
        place(state, ITALY, "A Rom");
        place(state, TURKEY, "A Con");
        place(state, AUSTRIA, "A Vie");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, ITALY);
        // Italy (4) -> Russia (5) has no units -> Turkey (6) -> round to Austria (0) -> resolved
        fm.next(state, order(state, "A Rom H"));
        assertEquals(TURKEY, state.getCurrentPlayer());
        fm.next(state, order(state, "A Con H"));
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        fm.next(state, order(state, "A Vie H"));
        // resolved: Fall, lowest-index power with units (Austria) first
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(AUSTRIA, state.getCurrentPlayer());
        assertEquals(1901, state.getYear());
    }

    @Test
    public void nextPhaseStartsWithTheLowestIndexPowerThatHasUnits() {
        clearBoard(state);
        place(state, TURKEY, "A Con");
        place(state, GERMANY, "A Ber");
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, GERMANY);
        play(state, fm, "A Ber H", "A Con H");
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(GERMANY, state.getCurrentPlayer());
    }
}

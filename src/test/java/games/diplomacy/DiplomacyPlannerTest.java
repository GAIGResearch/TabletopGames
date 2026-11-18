package games.diplomacy;

import core.actions.AbstractAction;
import games.diplomacy.actions.Hold;
import games.diplomacy.actions.WaiveBuilds;
import games.diplomacy.gui.DiplomacyPlanner;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * The planner with which a power's orders are planned in any order in the web server's page, and sent together:
 * England's orders in Spring 1901 (F Edi, F Lon, A Lvp) unless arranged otherwise.
 */
public class DiplomacyPlannerTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;
    DiplomacyPlanner planner;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
        planner = new DiplomacyPlanner(fm);
        startPhase(state, DiplomacyPhase.SPRING_ORDERS, ENGLAND);
    }

    /** The planned state after the orders written. */
    DiplomacyGameState planned(String... orders) {
        DiplomacyGameState planned = (DiplomacyGameState) state.copy();
        for (String o : orders)
            planner.apply(planned, order(planned, o));
        return planned;
    }

    @Test
    public void optionsAreTheOrdersTheForwardModelOffersForEachOfThePowersUnits() {
        // the forward model asks about the units one by one (each holding here); the planner offers them all at once
        Set<AbstractAction> asked = new HashSet<>();
        DiplomacyGameState s = (DiplomacyGameState) state.copy();
        while (s.getCurrentPlayer() == ENGLAND) {
            List<AbstractAction> available = fm.computeAvailableActions(s);
            asked.addAll(available);
            fm.next(s, available.stream().filter(a -> a instanceof Hold).findFirst().orElseThrow());
        }
        assertEquals(3, s.getOrders(ENGLAND).size());
        assertEquals(asked, new HashSet<>(planner.options(state, ENGLAND)));
    }

    @Test
    public void aPlanStartsAtThePowersFirstDecisionOfThePhase() {
        assertTrue(planner.plans(state, ENGLAND));
        assertFalse(planner.plans(state, FRANCE));
        assertFalse(planner.plans(planned("A Lvp-Yor"), ENGLAND));
    }

    @Test
    public void anyUnitMayBeOrderedFirstAndItsOrderChangedAfterwards() {
        // Lvp is the last unit the forward model would ask about
        DiplomacyGameState planned = planned("A Lvp-Yor");
        assertEquals(List.of(order(state, "A Lvp-Yor")), planned.getOrders(ENGLAND));
        // the turn is not passed on, and Lvp may still be given another order
        assertEquals(ENGLAND, planned.getCurrentPlayer());
        assertTrue(planner.options(planned, ENGLAND).contains(order(state, "A Lvp-Wal")));
        assertTrue(planner.options(planned, ENGLAND).contains(order(state, "F Edi-Nth")));
        assertTrue(planner.replaces(order(state, "A Lvp-Yor"), order(state, "A Lvp-Wal")));
        assertFalse(planner.replaces(order(state, "A Lvp-Yor"), order(state, "F Lon-Nth")));
    }

    @Test
    public void aUnitGivenNoOrderHolds() {
        List<AbstractAction> available = fm.computeAvailableActions(state);
        AbstractAction fallback = planner.fallback(state, available);
        assertTrue(fallback instanceof Hold);
        assertTrue(available.contains(fallback));
    }

    @Test
    public void aDislodgedUnitGivenNoRetreatIsDisbanded() {
        clearBoard(state);
        place(state, ENGLAND, "F Nth");
        dislodge(state, ENGLAND, "F Nth", "Hel");
        startPhase(state, DiplomacyPhase.SPRING_RETREATS, ENGLAND);
        List<AbstractAction> available = fm.computeAvailableActions(state);
        assertEquals(order(state, "Disband Nth"), planner.fallback(state, available));
    }

    @Test
    public void unusedBuildsAreWaived() {
        clearBoard(state);
        own(state, ENGLAND, "Edi", "Lon", "Lvp");
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, ENGLAND);
        List<AbstractAction> available = fm.computeAvailableActions(state);
        assertEquals(new HashSet<>(available), new HashSet<>(planner.options(state, ENGLAND)));
        assertTrue(planner.fallback(state, available) instanceof WaiveBuilds);
        assertTrue(String.join(" ", planner.warnings(state, ENGLAND)).contains("3 builds not used"));
    }

    @Test
    public void unitsWithoutOrdersAreNamedAsHolding() {
        List<String> warnings = planner.warnings(planned("F Lon-Nth"), ENGLAND);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0), warnings.get(0).startsWith("No order, so holding: "));
        assertEquals(2, warnings.get(0).substring(warnings.get(0).indexOf(":")).split(",").length);
    }

    @Test
    public void aSupportForAMoveNotOrderedIsPointedOut() {
        List<String> warnings = planner.warnings(planned("F Lon H", "F Edi S A Lvp-Yor", "A Lvp H"), ENGLAND);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0), warnings.get(0).contains("supports a move"));
        assertTrue(planner.warnings(planned("F Lon H", "F Edi S A Lvp-Yor", "A Lvp-Yor"), ENGLAND).isEmpty());
    }

    @Test
    public void twoUnitsOrderedIntoOneProvinceArePointedOut() {
        List<String> warnings = planner.warnings(planned("F Lon H", "F Edi-Yor", "A Lvp-Yor"), ENGLAND);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0), warnings.get(0).contains("none of them gets there"));
    }
}

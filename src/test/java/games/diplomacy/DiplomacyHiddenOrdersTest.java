package games.diplomacy;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.diplomacy.DiplomacyTestUtils.*;
import static org.junit.Assert.*;

/**
 * Orders are hidden until the phase is resolved. A copy for one power (partial observability, the default core
 * parameters) keeps its own orders, wipes the other powers', and gives it the turn while it still has orders to
 * give. A faithful copy keeps everything.
 */
public class DiplomacyHiddenOrdersTest {

    DiplomacyGameState state;
    DiplomacyForwardModel fm;

    @Before
    public void setup() {
        state = newState();
        fm = new DiplomacyForwardModel();
    }

    /**
     * Austria has ordered all three units and England F Edi; England has the turn. Arranged directly (addOrder,
     * setTurnOwner) so these tests depend on redeterminise, not on turn passing.
     */
    private void austriaAndOneEnglishOrder() {
        for (String o : List.of("A Bud H", "F Tri-Alb", "A Vie-Tri"))
            state.addOrder(AUSTRIA, order(state, o));
        state.addOrder(ENGLAND, order(state, "F Edi-Nrg"));
        state.setTurnOwner(ENGLAND);
    }

    @Test
    public void copyForAPowerKeepsItsOwnOrdersAndWipesTheOthers() {
        austriaAndOneEnglishOrder();
        DiplomacyGameState forEngland = (DiplomacyGameState) state.copy(ENGLAND);
        assertEquals(List.of(), forEngland.getOrders(AUSTRIA));
        assertEquals(List.of(order(state, "F Edi-Nrg")), forEngland.getOrders(ENGLAND));
        // England still has F Lon and A Lvp to order, and has the turn
        assertEquals(ENGLAND, forEngland.getCurrentPlayer());

        DiplomacyGameState forAustria = (DiplomacyGameState) state.copy(AUSTRIA);
        assertEquals(List.of(order(state, "A Bud H"), order(state, "F Tri-Alb"), order(state, "A Vie-Tri")),
                forAustria.getOrders(AUSTRIA));
        assertEquals(List.of(), forAustria.getOrders(ENGLAND));
        // Austria has nothing left to order, so the turn is left where it was
        assertEquals(ENGLAND, forAustria.getCurrentPlayer());

        // the original is untouched
        assertEquals(3, state.getOrders(AUSTRIA).size());
        assertEquals(1, state.getOrders(ENGLAND).size());
    }

    @Test
    public void copyForAPowerStillToOrderGivesItTheTurnAndTheWipedPowersOrderAgainAfterIt() {
        austriaAndOneEnglishOrder();
        DiplomacyGameState forFrance = (DiplomacyGameState) state.copy(FRANCE);
        assertEquals(FRANCE, forFrance.getCurrentPlayer());
        for (int p = 0; p < N_POWERS; p++)
            assertEquals(POWER_NAMES[p], List.of(), forFrance.getOrders(p));
        assertEquals(List.of(prov(state, "Bud"), prov(state, "Tri"), prov(state, "Vie")), forFrance.unitsToOrder(AUSTRIA));
        // the whole phase can be played out in the copy: France, Germany ... Turkey, then round to Austria and
        // England, whose orders were wiped (play checks the turn order and legality of each)
        play(forFrance, fm, "F Bre H", "A Mar H", "A Par H");
        assertEquals(GERMANY, forFrance.getCurrentPlayer());
        holdAll(forFrance, fm);
        assertEquals(DiplomacyPhase.FALL_ORDERS, forFrance.getPhase());
        assertEquals(22, forFrance.getLastResults().size());
        // the original is still waiting for England
        assertEquals(DiplomacyPhase.SPRING_ORDERS, state.getPhase());
        assertEquals(ENGLAND, state.getCurrentPlayer());
    }

    @Test
    public void faithfulCopyKeepsEveryOrder() {
        austriaAndOneEnglishOrder();
        DiplomacyGameState copy = (DiplomacyGameState) state.copy();
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        assertEquals(3, copy.getOrders(AUSTRIA).size());
        assertEquals(ENGLAND, copy.getCurrentPlayer());
    }

    @Test
    public void freshCopiesEqualTheOriginal() {
        // no orders yet and Austria to act: a copy for Austria wipes nothing and changes no turn
        DiplomacyGameState copy = (DiplomacyGameState) state.copy(AUSTRIA);
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
        DiplomacyGameState full = (DiplomacyGameState) state.copy();
        assertEquals(state, full);
        assertEquals(state.hashCode(), full.hashCode());
    }

    @Test
    public void copiesAfterAResolvedPhaseKeepItsResults() {
        // every unit holds in Spring 1901: Fall 1901 with Austria to act and 22 results to see
        holdAll(state, fm);
        assertEquals(DiplomacyPhase.FALL_ORDERS, state.getPhase());
        assertEquals(22, state.getLastResults().size());
        DiplomacyGameState copy = (DiplomacyGameState) state.copy(AUSTRIA);
        assertEquals(state.getLastResults(), copy.getLastResults());
        assertEquals(state, copy);
        assertEquals(state.hashCode(), copy.hashCode());
    }

    @Test
    public void copysOrdersAreIndependentOfTheOriginal() {
        DiplomacyGameState copy = (DiplomacyGameState) state.copy();
        copy.addOrder(AUSTRIA, order(state, "A Bud H"));
        assertEquals(List.of(), state.getOrders(AUSTRIA));
        assertNotEquals(state, copy);
        state.addOrder(ENGLAND, order(state, "F Edi H"));
        assertEquals(List.of(), copy.getOrders(ENGLAND));
    }

    @Test
    public void adjustmentOrdersAreHiddenToo() {
        // France +2 (Bre, Mar free), Germany +1 (Kie free); France builds F Bre and waives, Germany to act
        state.setUnit(prov(state, "Bre"), null);
        state.setUnit(prov(state, "Mar"), null);
        state.setUnit(prov(state, "Kie"), null);
        startPhase(state, DiplomacyPhase.ADJUSTMENTS, GERMANY);
        state.addOrder(FRANCE, order(state, "Build F Bre"));
        state.addOrder(FRANCE, order(state, "Waive France"));

        DiplomacyGameState forGermany = (DiplomacyGameState) state.copy(GERMANY);
        assertEquals(List.of(), forGermany.getOrders(FRANCE));
        assertEquals(GERMANY, forGermany.getCurrentPlayer());

        DiplomacyGameState forFrance = (DiplomacyGameState) state.copy(FRANCE);
        assertEquals(List.of(order(state, "Build F Bre"), order(state, "Waive France")), forFrance.getOrders(FRANCE));
        // France has waived: nothing more to give, Germany keeps the turn
        assertFalse(forFrance.hasOrdersToGive(FRANCE));
        assertEquals(GERMANY, forFrance.getCurrentPlayer());

        // a copy for Italy (nothing to adjust) wipes France's orders; France then has its decision again
        DiplomacyGameState forItaly = (DiplomacyGameState) state.copy(ITALY);
        assertEquals(List.of(), forItaly.getOrders(FRANCE));
        assertTrue(forItaly.hasOrdersToGive(FRANCE));
        assertEquals(List.of(prov(state, "Bre"), prov(state, "Mar")), forItaly.freeHomeCentres(FRANCE));
        assertEquals(GERMANY, forItaly.getCurrentPlayer());
    }
}

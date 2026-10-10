package games.risk;

import core.actions.AbstractAction;
import games.risk.actions.PlaceArmy;
import games.risk.gui.RiskPlanner;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static games.risk.RiskTestUtils.*;
import static games.risk.WorldMap.*;
import static org.junit.Assert.*;

/**
 * The planner with which a player plans their reinforcements in the web server's page and sends them together:
 * player 0 holds Alaska and Kamchatka, player 1 everything else.
 */
public class RiskPlannerTest {

    RiskForwardModel fm = new RiskForwardModel();
    RiskPlanner planner = new RiskPlanner(fm);
    RiskGameState state;

    @Before
    public void setup() {
        state = newState(3, 7, null);
        fillBoard(state, 1);
        give(state, 0, 1, ALASKA, KAMCHATKA);
        startPlay(state, 0, RiskGamePhase.REINFORCE, 7);
    }

    @Test
    public void aPlanIsMadeOnlyForThePlayersReinforcements() {
        assertTrue(planner.plans(state, 0));
        assertFalse(planner.plans(state, 1));
        startPlay(state, 0, RiskGamePhase.ATTACK, 0);
        assertFalse(planner.plans(state, 0));
    }

    @Test
    public void theOptionsAreTheForwardModelsAndArePlannedOnTheCopy() {
        assertEquals(fm.computeAvailableActions(state), planner.options(state, 0));
        RiskGameState planned = (RiskGameState) state.copy();
        PlaceArmy five = (PlaceArmy) planner.options(planned, 0).stream()
                .filter(a -> a instanceof PlaceArmy p && p.territory.equals(KAMCHATKA)).findFirst().orElseThrow();
        assertEquals(5, five.n);
        planner.apply(planned, five);
        assertEquals(6, planned.getArmies(KAMCHATKA));
        assertEquals(2, planned.getArmiesToPlace(0));
        // the state planned from is untouched
        assertEquals(1, state.getArmies(KAMCHATKA));
        assertEquals(7, state.getArmiesToPlace(0));
    }

    @Test
    public void thePlanEndsWhenEveryArmyIsPlaced() {
        RiskGameState planned = (RiskGameState) state.copy();
        while (planned.getGamePhase() == RiskGamePhase.REINFORCE) {
            List<String> warnings = planner.warnings(planned, 0);
            assertEquals(1, warnings.size());
            assertTrue(warnings.get(0), warnings.get(0).startsWith(planned.getArmiesToPlace(0) + " arm"));
            AbstractAction place = planner.options(planned, 0).stream().filter(a -> a instanceof PlaceArmy)
                    .findFirst().orElseThrow();
            planner.apply(planned, place);
        }
        assertEquals(RiskGamePhase.ATTACK, planned.getGamePhase());
        assertTrue(planner.options(planned, 0).isEmpty());
        assertTrue(planner.warnings(planned, 0).isEmpty());
        assertEquals(9, planned.getArmies(ALASKA) + planned.getArmies(KAMCHATKA));
    }
}

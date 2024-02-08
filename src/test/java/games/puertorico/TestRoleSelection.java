package games.puertorico;

import core.actions.AbstractAction;
import games.puertorico.*;
import games.puertorico.actions.SelectRole;
import games.puertorico.roles.Prospector;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static games.puertorico.PuertoRicoConstants.Role.*;
import static org.junit.Assert.*;

public class TestRoleSelection {

    PuertoRicoForwardModel fm = new PuertoRicoForwardModel();
    PuertoRicoGameState state = new PuertoRicoGameState(new PuertoRicoParameters(), 4);

    @Before
    public void setup() {
        fm.setup(state);
    }
    @Test
    public void onlyAvailableRolesCanBeSelected() {
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(7, actions.size());
        assertTrue(actions.contains(new SelectRole(CAPTAIN)));
        assertTrue(actions.contains(new SelectRole(SETTLER)));
        assertTrue(actions.contains(new SelectRole(MAYOR)));
        assertTrue(actions.contains(new SelectRole(BUILDER)));
        assertTrue(actions.contains(new SelectRole(PROSPECTOR)));
        assertTrue(actions.contains(new SelectRole(TRADER)));
        assertTrue(actions.contains(new SelectRole(CRAFTSMAN)));
        state.setCurrentRole(SETTLER);
        actions = fm.computeAvailableActions(state);
        assertEquals(6, actions.size());
        assertTrue(actions.contains(new SelectRole(CAPTAIN)));
        assertTrue(actions.contains(new SelectRole(MAYOR)));
        assertTrue(actions.contains(new SelectRole(BUILDER)));
        assertTrue(actions.contains(new SelectRole(PROSPECTOR)));
        assertTrue(actions.contains(new SelectRole(TRADER)));
        assertTrue(actions.contains(new SelectRole(CRAFTSMAN)));
    }

    @Test
    public void testRoleSelectionProcessesThroughAllPlayersInOneCompleteRound() {
        for (int player = 0; player < 4; player++) {
            System.out.println("Player " + player);
            assertEquals(player, state.getCurrentPlayer());
            assertEquals(0, state.getRoundCounter());
            do {
                assertEquals(player, state.getTurnOwner());
                fm.next(state, fm.computeAvailableActions(state).get(0));
            } while (state.isActionInProgress());
        }
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(1, state.getRoundCounter());
    }

    @Test
    public void gameEndsAtEndOfTriggeringRound() {
        fm.next(state, new SelectRole(SETTLER));
        assertFalse(state.isLastRound());
        state.setGameEndTriggered();
        assertTrue(state.isLastRound());
        Set<Integer> roleOwners = new HashSet<>();
        do {
            roleOwners.add(state.getTurnOwner());
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.isNotTerminal() && state.getRoundCounter() == 0);

        assertEquals(4, roleOwners.size()); // all players had a turn
        assertFalse(state.isNotTerminal());
        assertEquals(0, state.getRoundCounter());
    }

    @Test
    public void testEndOfRoundProcessing() {
        // here we check that money is put on the unused Roles, and that they are all refreshed
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.getRoundCounter() == 0);

        assertEquals(7, state.getAvailableRoles().size());
        assertEquals(3, state.getAvailableRoles().values().stream().filter(i -> i == 1).count());
    }

    @Test
    public void testSelectingRoleWithMoneyOnIt() {
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.getRoundCounter() == 0);
        // find an unpicked role with money on it
        PuertoRicoConstants.Role role = state.getAvailableRoles().entrySet().stream()
                .filter(e -> e.getValue() > 0)
                .map(Map.Entry::getKey)
                .findFirst().orElseThrow(() -> new AssertionError("No role with money on it found"));
        int startingMoney = state.getDoubloons(state.getCurrentPlayer());
        int player = state.getCurrentPlayer();
        fm.next(state, new SelectRole(role));
        assertEquals(startingMoney + 1 + (role == PROSPECTOR ? 1 : 0), state.getDoubloons(player));
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.getRoundCounter() == 1);

        // and check that there is no money on this role at end of round
        assertEquals(0, state.getAvailableRoles().get(role).intValue());
    }

}

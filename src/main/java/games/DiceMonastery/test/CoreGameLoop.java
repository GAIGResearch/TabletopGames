package games.DiceMonastery.test;

import core.actions.AbstractAction;
import games.DiceMonastery.*;
import org.junit.*;

import java.util.*;

import static games.DiceMonastery.DiceMonasteryConstants.*;
import static games.DiceMonastery.DiceMonasteryConstants.Phase.*;
import static games.DiceMonastery.DiceMonasteryConstants.Resource.*;
import static games.DiceMonastery.DiceMonasteryConstants.Season.*;
import static org.junit.Assert.*;


public class CoreGameLoop {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    DiceMonasteryGame game = new DiceMonasteryGame(fm, new DiceMonasteryGameState(new DiceMonasteryParams(3), 4));

    @Test
    public void gameSetup() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        for (int p = 0; p < 4; p++) {
            assertEquals(2, state.getResource(p, GRAIN));
            assertEquals(2, state.getResource(p, WAX));
            assertEquals(2, state.getResource(p, HONEY));
            assertEquals(2, state.getResource(p, BREAD));
            assertEquals(6, state.getResource(p, SHILLINGS));
            assertEquals(1, state.getResource(p, PRAYERS));

            assertEquals(4 + 3 + 2 + 2 + 1 + 1, state.getGameScore(p), 0.01);
            assertEquals(6, state.monksIn(ActionArea.DORMITORY, p).size());
        }
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(SPRING, ((DiceMonasteryTurnOrder) state.getTurnOrder()).getSeason());
        assertEquals(1, ((DiceMonasteryTurnOrder) state.getTurnOrder()).getYear());

        assertEquals(24, state.monksIn(ActionArea.DORMITORY, -1).size());
    }

    @Test
    public void placeMonkActionsGeneratedCorrectly() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(6, actions.size());
        for (ActionArea a : ActionArea.values()) {
            if (a.dieMinimum > 0)
                assertTrue(actions.contains(new PlaceMonk(0, a)));
        }

        fm.next(state, new PlaceMonk(0, ActionArea.MEADOW));
        actions = fm.computeAvailableActions(state);
        assertEquals(4, actions.size());
        assertTrue(actions.stream().allMatch(a -> a.toString().contains("Choose Monk")));

        assertEquals(24, state.monksIn(ActionArea.DORMITORY, -1).size());
        assertEquals(6, state.monksIn(ActionArea.DORMITORY, 0).size());
        assertEquals(0, state.monksIn(ActionArea.MEADOW, -1).size());
        assertEquals(0, state.monksIn(ActionArea.MEADOW, 0).size());

        fm.next(state, actions.get(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(PLACE_MONKS, state.getGamePhase());
        assertEquals(23, state.monksIn(ActionArea.DORMITORY, -1).size());
        assertEquals(5, state.monksIn(ActionArea.DORMITORY, 0).size());
        assertEquals(4 + 3 + 2 + 2 + 1 + 1, state.getGameScore(0), 0.01);
        assertEquals(1, state.monksIn(ActionArea.MEADOW, -1).size());
        assertEquals(1, state.monksIn(ActionArea.MEADOW, 0).size());
    }

    @Test
    public void varyingNumbersOfMonksWorksWhenPlacing() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.createMonk(5, 3);

        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(ActionArea.DORMITORY, 3).size() > 1);

        // at this point we should have 1 monks still to place for P3, and 0 each for all other players
        assertEquals(3, state.getCurrentPlayer());
        assertEquals(Phase.PLACE_MONKS, state.getGamePhase());
        fm.next(state, fm.computeAvailableActions(state).get(0)); // PlaceMonk
        fm.next(state, fm.computeAvailableActions(state).get(0)); // ChooseMonk
        assertEquals(USE_MONKS, state.getGamePhase());
    }
}

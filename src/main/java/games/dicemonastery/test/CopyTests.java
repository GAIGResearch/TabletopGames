package games.dicemonastery.test;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dicemonastery.*;
import games.dicemonastery.actions.PlaceMonk;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;

import static games.dicemonastery.DiceMonasteryConstants.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.Phase.PLACE_MONKS;
import static games.dicemonastery.DiceMonasteryConstants.Phase.USE_MONKS;
import static games.dicemonastery.DiceMonasteryConstants.Season.AUTUMN;
import static games.dicemonastery.DiceMonasteryConstants.Season.SPRING;
import static java.util.stream.Collectors.*;
import static java.util.stream.Collectors.summingInt;
import static org.junit.Assert.*;


public class CopyTests {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    DiceMonasteryGame game = new DiceMonasteryGame(fm, new DiceMonasteryGameState(new DiceMonasteryParams(3), 4));
    RandomPlayer rnd = new RandomPlayer();


    @Test
    public void placeMonkActionsGeneratedCorrectly() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        List<AbstractAction> actions;

        fm.next(state, new PlaceMonk(0, ActionArea.MEADOW));
        actions = fm.computeAvailableActions(state);

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, actions.get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void varyingNumbersOfMonksWorksWhenPlacing() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.createMonk(5, 3);

        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(ActionArea.DORMITORY, 3).size() > 1);

        // at this point we should have 1 monks still to place for P3, and 0 each for all other players
        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0)); // PlaceMonk

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0)); // ChooseMonk

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void usingAllMonks() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void seasonMovesOnAfterPlacingAndUsingAllMonks() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();
        do {
            fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));
        } while (state.monksIn(DORMITORY, 3).size() > 0);

        for (int i = 0; i < 4 * 6 - 2; i++) { // 24 monks in total
            assertEquals(USE_MONKS, state.getGamePhase());
            fm.next(state, fm.computeAvailableActions(state).get(0));
            assertEquals(i + 1, state.monksIn(DORMITORY, -1).size());
        }

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, fm.computeAvailableActions(state).get(0));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, fm.computeAvailableActions(state).get(0));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

}

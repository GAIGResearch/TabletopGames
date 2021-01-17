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


public class CopyTests {

    DiceMonasteryForwardModel fm = new DiceMonasteryForwardModel();
    DiceMonasteryGame game = new DiceMonasteryGame(fm, new DiceMonasteryGameState(new DiceMonasteryParams(3), 4));


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
}

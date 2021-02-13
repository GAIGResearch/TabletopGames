package games.dicemonastery.test;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import games.dicemonastery.*;
import games.dicemonastery.actions.BakeBread;
import games.dicemonastery.actions.HireNovice;
import games.dicemonastery.actions.PlaceMonk;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.List;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.*;
import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;
import static games.dicemonastery.DiceMonasteryConstants.Resource.*;
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

        do { // until we have two players left wit monks in the Chapel
            fm.next(state, fm.computeAvailableActions(state).get(0));
        } while (state.monksIn(CHAPEL, -1).stream().map(Monk::getComponentID).distinct().count() > 2);

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
    public void bakeBread() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.useAP(-1);
        // Has 2 Grain in STOREROOM at setup

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        (new BakeBread()).execute(state);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }


    @Test
    public void hireNovice() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.useAP(-1);
        HireNovice action = new HireNovice();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        try {
            fm.next(state, action);
            fail("Should throw exception as not enough AP");
        } catch (IllegalArgumentException e) {
            // expected
        }

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertEquals(midHash, startHash); // special case as the next action fails

        state.useAP(-2);
        fm.next(state, action);

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }


    @Test
    public void foodIsRemovedToFeedMonksAtYearEnd() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        state.addResource(1, BERRIES, 1);
        state.addResource(1, BREAD, 10);
        state.addResource(1, HONEY, 2);
        state.addResource(1, GRAIN, 20);
        state.addResource(2, BERRIES, 1);
        state.addResource(2, BREAD, 2);
        state.addResource(2, HONEY, 10);

        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, rnd.getAction(state, fm.computeAvailableActions(state)));

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());
    }

    @Test
    public void allSurplusPerishablesAreRemovedAtYearEnd() {
        DiceMonasteryGameState state = (DiceMonasteryGameState) game.getGameState();

        int startHash = state.hashCode();
        DiceMonasteryGameState copy = (DiceMonasteryGameState) state.copy();
        assertEquals(startHash, copy.hashCode());

        state.addResource(0, BERRIES, 20);
        state.addResource(0, BREAD, 20);
        state.addResource(0, HONEY, 20);
        state.addResource(0, CALF_SKIN, 20 - state.getResource(0, CALF_SKIN, STOREROOM));
        state.addResource(0, BEER, 20 - state.getResource(0, BEER, STOREROOM));
        state.addResource(0, GRAIN, 20 - state.getResource(0, GRAIN, STOREROOM));

        int midHash = state.hashCode();
        DiceMonasteryGameState midCopy = (DiceMonasteryGameState) state.copy();
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == startHash);

        fm.next(state, new DoNothing()); // don't promote anyone

        assertEquals(startHash, copy.hashCode());
        assertFalse(startHash == state.hashCode());
        assertEquals(midHash, midCopy.hashCode());
        assertFalse(midHash == state.hashCode());

    }

}

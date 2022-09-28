package test.games.cantstop;

import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.cantstop.*;
import games.cantstop.actions.*;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

public class TestCantStop {

    Game cantStop;
    List<AbstractPlayer> players;
    CantStopForwardModel fm = new CantStopForwardModel();

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        cantStop = GameType.CantStop.createGameInstance(3, 34, new CantStopParameters(-274));
        cantStop.reset(players);
    }

    @Test
    public void testPassMovesToNextPlayerDirectly() {
        assertEquals(CantStopGamePhase.Decision, cantStop.getGameState().getGamePhase());
        assertEquals(0, cantStop.getGameState().getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(cantStop.getGameState());
        assertEquals(2, actions.size());
        assertEquals(new Pass(false), actions.get(0));
        assertEquals(new RollDice(), actions.get(1));

        fm.next(cantStop.getGameState(), new Pass(false));
        assertEquals(CantStopGamePhase.Decision, cantStop.getGameState().getGamePhase());
        assertEquals(1, cantStop.getGameState().getCurrentPlayer());
    }

    @Test
    public void testRollDiceMovesToAllocatePhase() {
        assertEquals(CantStopGamePhase.Decision, cantStop.getGameState().getGamePhase());
        assertEquals(0, cantStop.getGameState().getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(cantStop.getGameState());
        assertEquals(2, actions.size());
        assertEquals(new Pass(false), actions.get(0));
        assertEquals(new RollDice(), actions.get(1));

        fm.next(cantStop.getGameState(), new RollDice());
        assertEquals(CantStopGamePhase.Allocation, cantStop.getGameState().getGamePhase());
        assertEquals(0, cantStop.getGameState().getCurrentPlayer());
    }


    @Test
    public void testDiceAllocationMovesMarkers() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{6, 5, 3, 1});

        assertTrue(fm.computeAvailableActions(state).stream().allMatch(a -> a instanceof AllocateDice));
        AllocateDice action = (AllocateDice) fm.computeAvailableActions(state).get(0);
        assertEquals(0, state.getMarkerPosition(action.getValues()[0], 0));
        assertEquals(0, state.getMarkerPosition(action.getValues()[1], 0));
        assertEquals(0, state.getTemporaryMarkerPosition(action.getValues()[0]));
        assertEquals(0, state.getTemporaryMarkerPosition(action.getValues()[1]));
        fm.next(state, action);
        assertEquals(0, state.getMarkerPosition(action.getValues()[0], 0));
        assertEquals(0, state.getMarkerPosition(action.getValues()[1], 0));
        assertEquals(1, state.getTemporaryMarkerPosition(action.getValues()[0]));
        assertEquals(1, state.getTemporaryMarkerPosition(action.getValues()[1]));
    }

    @Test
    public void testThreeMarkersMeansBust() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{4, 5, 1, 4});  // gives 9, 8, 5, 6
        // get the unique numbers
        state.moveMarker(2);
        state.moveMarker(3);
        state.moveMarker(12);

        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new Pass(true), fm.computeAvailableActions(state).get(0));
    }

    @Test
    public void testBustIfNoMovesPossible() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{1, 1, 1, 2});  // gives 2, 3 only

        // we now complete all of the numbers
        CantStopParameters params = (CantStopParameters) state.getGameParameters();
        for (int n : new int[]{2, 3}) {
            do {
                state.moveMarker(n);
            } while (state.getTemporaryMarkerPosition(n) < params.maxValue(n));
        }
        fm.makeTemporaryMarkersPermanent(state);
        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new Pass(true), fm.computeAvailableActions(state).get(0));
    }


    @Test
    public void testBustIfAllCompletedOrTempIsAtMax() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{6, 1, 1, 2});  // gives 2, 3, 7, 8

        // we now complete two of the numbers
        CantStopParameters params = (CantStopParameters) state.getGameParameters();
        for (int n : new int[]{2, 3}) {
            do {
                state.moveMarker(n);
            } while (state.getTemporaryMarkerPosition(n) < params.maxValue(n));
        }
        fm.makeTemporaryMarkersPermanent(state);

        // and put the other two at the top - so they cannot be increased any more
        for (int n : new int[]{7, 8}) {
            do {
                state.moveMarker(n);
            } while (state.getTemporaryMarkerPosition(n) < params.maxValue(n));
        }

        assertEquals(1, fm.computeAvailableActions(state).size());
        assertEquals(new Pass(true), fm.computeAvailableActions(state).get(0));
    }

    @Test
    public void testMarkersBecomePermanentOnPass() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        state.moveMarker(3);
        state.moveMarker(3);
        state.moveMarker(7);
        assertEquals(2, state.getTemporaryMarkerPosition(3));
        assertEquals(1, state.getTemporaryMarkerPosition(7));
        fm.next(state, new Pass(false));
        assertEquals(2, state.getMarkerPosition(3, 0));
        assertEquals(1, state.getMarkerPosition(7, 0));
        assertEquals(0, state.getTemporaryMarkerPosition(3));
        assertEquals(0, state.getTemporaryMarkerPosition(7));
    }

    @Test
    public void testMarkersZeroedOnBust() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        state.moveMarker(3);
        state.moveMarker(3);
        state.moveMarker(7);
        assertEquals(2, state.getTemporaryMarkerPosition(3));
        assertEquals(1, state.getTemporaryMarkerPosition(7));
        fm.next(state, new Pass(true));
        assertEquals(0, state.getMarkerPosition(3, 0));
        assertEquals(0, state.getMarkerPosition(7, 0));
        assertEquals(0, state.getTemporaryMarkerPosition(3));
        assertEquals(0, state.getTemporaryMarkerPosition(7));
    }

    @Test
    public void testAllocateDiceEqualityCombos() {
        assertEquals(new AllocateDice(6, 8), new AllocateDice(8, 6));
    }

    @Test
    public void testFourthMarkerNotCreatedIfInvalid() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{6, 1, 1, 2});  // gives 2, 3, 7, 8

        // then move two of the markers so that only one is available
        state.moveMarker(4);
        state.moveMarker(5);

        assertEquals(4, fm.computeAvailableActions(state).size()); // all options are possible (2, 3, 7, 8)
        fm.next(state, new AllocateDice(7));
        assertEquals(1, state.getTemporaryMarkerPosition(7));
    }

    @Test
    public void testDoubleIsValidIfOnlyOneSpaceToMove() {
        // Rules slightly unclear on this one. We interpret the rule that a runner must be moved by one of the dice
        // to enable a 4+4 pairing to be chosen, and then to just use one 4.
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{3, 1, 3, 1});  // gives 4+4, 2+6

        // then move two of the markers so that only one is available
        for (int i = 0; i < 5; i++)
            state.moveMarker(4);  // one off the top

        assertEquals(2, fm.computeAvailableActions(state).size()); // 4, 2+6
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(2, 6)));
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(4)));
    }

    @Test
    public void testDoubleIsValidIfOneFreeMarker() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{3, 4, 3, 4});  // gives 6+8 or 7+7

        // then move two of the markers so that only one is available
        state.moveMarker(4);
        state.moveMarker(5);

        assertEquals(3, fm.computeAvailableActions(state).size()); // 7+7, 6, 8
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(6)));
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(8)));
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(7, 7)));
    }

}

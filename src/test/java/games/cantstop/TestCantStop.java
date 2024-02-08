package games.cantstop;

import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.cantstop.CantStopForwardModel;
import games.cantstop.CantStopGamePhase;
import games.cantstop.CantStopGameState;
import games.cantstop.CantStopParameters;
import games.cantstop.actions.AllocateDice;
import games.cantstop.actions.Pass;
import games.cantstop.actions.RollDice;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;

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
        CantStopParameters params = new CantStopParameters();
        params.setRandomSeed(-274);
        cantStop = GameType.CantStop.createGameInstance(3, 34, params);
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
        fm.makeTemporaryMarkersPermanentAndClear(state);
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
        fm.makeTemporaryMarkersPermanentAndClear(state);

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

    @Test
    public void testDoubleIsNotValidIfOnlyOneStepLeftFromTempMarker() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{3, 4, 3, 4});  // gives 6+8 or 7+7

        // then move two of the markers so that only one is available
        for (int i = 0; i < 11; i++)
            state.moveMarker(7);

        assertEquals(2, fm.computeAvailableActions(state).size()); // 7, 6+8
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(6, 8)));
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(7)));
    }
    @Test
    public void testDoubleIsNotValidIfOnlyOneStepLeftFromPermMarker() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{3, 4, 3, 4});  // gives 6+8 or 7+7

        // then move two of the markers so that only one is available
        for (int i = 0; i < 11; i++)
            state.moveMarker(7);
        fm.makeTemporaryMarkersPermanentAndClear(state);

        assertEquals(2, fm.computeAvailableActions(state).size()); // 7, 6+8
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(6, 8)));
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(7)));
    }

    @Test
    public void testMarkerMovesInAdditionToStartingPlace() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        fm.next(state, new AllocateDice(7, 7));
        fm.next(state, new Pass(false));
        assertEquals(2, state.getMarkerPosition(7, 0));
        // we now continue until we get back to P0
        fm.next(state, new Pass(false));
        fm.next(state, new Pass(false));
        fm.next(state, new RollDice());
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, new AllocateDice(7, 7));
        assertEquals(4, state.getTemporaryMarkerPosition(7));
        assertEquals(2, state.getMarkerPosition(7, 0));
        fm.next(state, new Pass(false));
        assertEquals(4, state.getMarkerPosition(7, 0));
    }

    @Test
    public void testPermanentPositionDoesNotInterfere() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{2, 5, 5, 3}); // 5+10, 7+8
        state.moveMarker(5);
        state.moveMarker(5);
        state.moveMarker(7);
        state.moveMarker(7);
        fm.makeTemporaryMarkersPermanentAndClear(state);
        state.moveMarker(7);
        state.moveMarker(9);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(3, actions.size());
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(7, 8)));
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(10)));
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(5)));
    }

    @Test
    public void testTwoDiceWithNoAvailableMarker() {
        // we have markers set on 3 and 8
        // dice of 2, 2, 5, 3
        // should not have the available actions of 4+8, 7+5
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{2, 2, 5, 3});

        state.moveMarker(3);
        state.moveMarker(8);
        state.moveMarker(2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(8)));
    }

    @Test
    public void checkGameTerminates() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        assertEquals(0, state.getCurrentPlayer());
        state.moveMarker(2);
        state.moveMarker(2);
        state.moveMarker(3);
        state.moveMarker(3);
        state.moveMarker(3);
        state.moveMarker(3);
        fm.makeTemporaryMarkersPermanentAndClear(state);
        assertTrue(state.isNotTerminal());
        state.moveMarker(12);
        state.moveMarker(12);
        fm.makeTemporaryMarkersPermanentAndClear(state);
        assertEquals(0, state.getCurrentPlayer());
        assertFalse(state.isNotTerminal());
        assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[0]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[1]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[2]);
        assertEquals(CoreConstants.GameResult.GAME_END, state.getGameStatus());
    }

    @Test
    public void testFourIdenticalDice() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        state.setDice(new int[]{1, 1, 1, 1});
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertTrue(fm.computeAvailableActions(state).contains(new AllocateDice(2, 2)));
    }

    @Test
    public void testBustChangesPhase() {
        CantStopGameState state = (CantStopGameState) cantStop.getGameState();
        fm.next(state, new RollDice());
        do {
            fm.next(state, fm.computeAvailableActions(state).get(0));
            fm.next(state, new RollDice());
            // we keep rolling dice until we go bust
        } while (!fm.computeAvailableActions(state).get(0).equals(new Pass(true)));
        fm.next(state, new Pass(true));
        assertEquals(CantStopGamePhase.Decision, state.getGamePhase());
    }


}

package test.games.sirius;

import core.actions.AbstractAction;
import games.*;
import games.sirius.*;
import core.*;
import games.sirius.actions.*;
import org.junit.*;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static games.sirius.SiriusConstants.SiriusCardType.AMMONIA;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class TestMoves {

    Game game;
    SiriusGameState state;
    SiriusForwardModel fm = new SiriusForwardModel();
    List<AbstractPlayer> players = new ArrayList<>();

    @Before
    public void setup() {
        players = Arrays.asList(new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer());
        game = GameType.Sirius.createGameInstance(3, 34, new SiriusParameters());
        game.reset(players);
        state = (SiriusGameState) game.getGameState();
    }

    @Test
    public void plannedLocationOfPlayerMoves() {
        int currentPlayer = state.getCurrentPlayer();
        for (int i = 0; i < 3; i++)
            assertEquals(0, state.getLocationIndex(i));
        MoveToMoon moveAction = new MoveToMoon(1);
        moveAction.execute(game.getGameState());
        int[] plannedMoves = state.getMoveSelected();
        for (int i = 0; i < 3; i++) {
            assertEquals(0, state.getLocationIndex(i));
            assertEquals(i == currentPlayer ? 1 : -1, plannedMoves[i]);
        }
    }

    @Test
    public void actionLocationUpdatedOnceAllDecisionsMade() {
        MoveToMoon moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        assertEquals(1, state.getCurrentPlayer());
        moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        assertEquals(2, state.getCurrentPlayer());
        moveAction = new MoveToMoon(2);
        fm.next(state, moveAction);
        assertEquals(SiriusConstants.SiriusPhase.Draw, state.getGamePhase());

        assertEquals(1, state.getLocationIndex(0));
        assertEquals(1, state.getLocationIndex(1));
        assertEquals(2, state.getLocationIndex(2));
    }

    @Test
    public void allMovesAvailableExceptForCurrentLocation() {
        List<AbstractAction> moves = fm.computeAvailableActions(state);
        assertEquals(2, moves.size());
        assertEquals(new MoveToMoon(1), moves.get(0));
        assertEquals(new MoveToMoon(2), moves.get(1));

        state.movePlayerTo(0, 1);
        moves = fm.computeAvailableActions(state);
        assertEquals(2, moves.size());
        assertEquals(new MoveToMoon(0), moves.get(0));
        assertEquals(new MoveToMoon(2), moves.get(1));
    }

    @Test
    public void copyStateFromPerspectiveRemovesMoveInformation() {
        // first we set up some moves
        MoveToMoon moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        assertEquals(1, state.getCurrentPlayer());
        moveAction = new MoveToMoon(1);
        fm.next(state, moveAction);
        int[] plannedMoves = state.getMoveSelected();

        SiriusGameState copyFull = (SiriusGameState) state.copy();
        SiriusGameState copy0 = (SiriusGameState) state.copy(0);
        SiriusGameState copy1 = (SiriusGameState) state.copy(1);
        SiriusGameState copy2 = (SiriusGameState) state.copy(2);

        for (int i = 0; i < 3; i++) {
            assertEquals(plannedMoves[i], copyFull.getMoveSelected()[i]);
            assertEquals(i == 0 ? plannedMoves[i] : -1, copy0.getMoveSelected()[i]);
            assertEquals(i == 1 ? plannedMoves[i] : -1, copy1.getMoveSelected()[i]);
            assertEquals(i == 2 ? plannedMoves[i] : -1, copy2.getMoveSelected()[i]);
        }

    }

    @Test
    public void testTakeCardActionsInDrawPhase() {
        state.setGamePhase(SiriusConstants.SiriusPhase.Draw);
        state.movePlayerTo(0, 1);
        List<Integer> distinctCardValues = state.getMoon(1).getDeck().stream().mapToInt(c -> c.value).distinct().boxed().collect(toList());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(distinctCardValues.size(), actions.size());
        assertTrue(distinctCardValues.stream().map(TakeCard::new).allMatch(actions::contains));

        state.getMoon(1).addCard(new SiriusCard("TestAmmonia", AMMONIA, 4));
        List<AbstractAction> extendedActions = fm.computeAvailableActions(state);
        assertEquals(distinctCardValues.size() + 1, extendedActions.size());
    }

    @Test
    public void testTakeCardFunctionsAsExpected() {
        TakeCard action = new TakeCard(1);
        state.getMoon(1).addCard(new SiriusCard("ammonia", AMMONIA, 1));
        assertEquals(3,state.getMoon(1).getDeckSize());
        assertEquals(0,state.getPlayerHand(0).getSize());
        state.movePlayerTo(0, 1);
        fm.next(state, action);
        assertEquals(2,state.getMoon(1).getDeckSize());
        assertEquals(1,state.getPlayerHand(0).getSize());
        assertEquals(1, state.getPlayerHand(0).draw().value);
    }
}

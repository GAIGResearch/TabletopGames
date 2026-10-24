package games.chinesecheckers;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.chinesecheckers.actions.MovePeg;
import games.chinesecheckers.components.Peg;
import games.chinesecheckers.components.StarBoard;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ActionTests {
    CCForwardModel fm = new CCForwardModel();
    Game game = GameType.ChineseCheckers.createGameInstance(2, new CCParameters());
    CCGameState state = (CCGameState) game.getGameState();

    @Test
    public void movePegTest(){
        CCGameState state = (CCGameState) game.getGameState();
        MovePeg movePeg = new MovePeg(6, 14);
        MovePeg movePeg_ = new MovePeg(14, 90);

        fm.computeAvailableActions(state);
        fm.next(state, movePeg);
        assertEquals("Expected a purple peg at position 14", "purple", state.getStarBoard().getPeg(14).colour().name());
        assertFalse("Expected position 6 to be empty", state.getStarBoard().isOccupied(6));

        fm.computeAvailableActions(state);
        fm.next(state, movePeg_);
        assertEquals("Expected a purple peg at position 90", "purple", state.getStarBoard().getPeg(90).colour().name());
        assertFalse("Expected position 14 to be empty", state.getStarBoard().isOccupied(14));
    }

    @Test
    public void initialMovesCorrect() {
        assertEquals(0, state.getCurrentPlayer());
        assertEquals(14, fm.computeAvailableActions(state).size());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(14, fm.computeAvailableActions(state).size());
    }

    @Test
    public void jumpingMovesCorrect() {
        fm.next(state, new MovePeg(6, 15));
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(5, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 15).count());
        assertEquals(2, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 9).count());
        assertEquals(4, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 8).count());
        assertEquals(3, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 7).count());
        assertEquals(0, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 6).count());
        assertEquals(3, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 5).count());
        assertEquals(1, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 4).count());
        assertEquals(3, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 3).count());
        assertEquals(0, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 2).count());
        assertEquals(2, actions.stream().filter(a -> ((MovePeg)a).getFrom() == 1).count());
        assertEquals(23, actions.size());
    }
    @Test
    public void aPegMovingWithinItsStartingAreaHasNotReachedItsDestination() {
        // purple starts on the red nodes 0 to 9
        fm.next(state, new MovePeg(6, 14));
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertTrue(fm.computeAvailableActions(state).contains(new MovePeg(3, 6)));
        fm.next(state, new MovePeg(3, 6));
        assertFalse(state.getStarBoard().getPeg(6).inDestination());

        // so it may still step out onto the neutral nodes
        fm.next(state, fm.computeAvailableActions(state).get(0));
        StarBoard board = state.getStarBoard();
        assertTrue(fm.computeAvailableActions(state).stream().map(a -> (MovePeg) a)
                .anyMatch(m -> m.getFrom() == 6 && board.getBaseColour(m.getTo()) == Peg.Colour.neutral));
    }

    @Test
    public void aPegEnteringItsDestinationHasReachedIt() {
        // purple's destination is the purple nodes 111 to 120
        StarBoard board = state.getStarBoard();
        int outside = -1;
        for (var h : board.getPosition(111).neighbours())
            if (board.getNode(h) >= 0 && board.getBaseColour(board.getNode(h)) == Peg.Colour.neutral)
                outside = board.getNode(h);
        board.setPeg(outside, new Peg(Peg.Colour.purple, false));
        fm.next(state, new MovePeg(outside, 111));
        assertEquals(new Peg(Peg.Colour.purple, true), board.getPeg(111));
    }
}

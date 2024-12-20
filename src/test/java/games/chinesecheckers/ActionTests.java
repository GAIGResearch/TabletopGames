package games.chinesecheckers;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.chinesecheckers.actions.MovePeg;
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
        assertEquals("Expected a purple peg at position 14", "purple", state.getStarBoard().getBoardNodes().get(14).getOccupiedPeg().getColour().name());
        assertFalse("Expected position 6 to be empty", state.getStarBoard().getBoardNodes().get(6).isNodeOccupied());

        fm.computeAvailableActions(state);
        fm.next(state, movePeg_);
        assertEquals("Expected a purple peg at position 90", "purple", state.getStarBoard().getBoardNodes().get(90).getOccupiedPeg().getColour().name());
        assertFalse("Expected position 14 to be empty", state.getStarBoard().getBoardNodes().get(14).isNodeOccupied());
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
}

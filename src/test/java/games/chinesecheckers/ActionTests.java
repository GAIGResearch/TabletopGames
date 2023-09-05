package games.chinesecheckers;

import core.Game;
import games.GameType;
import games.chinesecheckers.actions.MovePeg;
import org.junit.Test;

import static org.junit.Assert.*;

public class ActionTests {
    CCForwardModel fm = new CCForwardModel();
    Game game = GameType.ChineseCheckers.createGameInstance(2, new CCParameters(0));
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
}

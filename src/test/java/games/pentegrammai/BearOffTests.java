package games.pentegrammai;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Token;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BearOffTests {

    PenteGameState state;
    PenteParameters params;
    PenteForwardModel fm;

    @Before
    public void setUp() {
        params = new PenteParameters();
        params.setParameterValue("startOffBoard", false);
        params.setParameterValue("blotRuleActive", true);
        params.setParameterValue("onePieceLimitOffSacredLine", false);
        params.setParameterValue("slideToMiddleOnSacredLine", true);
        params.setParameterValue("mustMoveFromSacredLine", true);
        state = new PenteGameState(params, 2);
        fm = new PenteForwardModel();
        fm.setup(state);
    }

    @Test
    public void testBearOffAdditionalRule() {
        // if we move a piece onto the latter part of the Holy Line, then it is removed (hence not blocking others)
        state.setDieValue(5);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new PenteMoveAction(2, 7)));
        // this will blot the opponent, and bear off
        fm.next(state, new PenteMoveAction(2, 7));
        assertEquals(1, state.getCurrentPlayer());
        assertEquals(0, state.getPiecesAt(7, 0));
        assertEquals(0, state.getPiecesAt(7, 1));
        assertEquals(1, state.getOffBoard(1));
        assertEquals(0, state.getPiecesAtGoal(1));
        assertEquals(1, state.getGameScore(0), 0.001);
        assertEquals(0, state.getGameScore(1), 0.001);
    }

    @Test
    public void testCanReEnterAfterBearingOffAndNotPassIfOtherOption() {
        params.setParameterValue("slideToMiddleOnSacredLine", true);
        params.setParameterValue("mustMoveFromSacredLine", false);
        params.setParameterValue("canMovePiecesBackOnToBoardAfterRemoval", true);
        params.setParameterValue("blotRuleActive", false);
        params.setParameterValue("startOffBoard", true);
        fm.setup(state);
        // The plan here is to move a piece off the board with slide activated
        // and confirm we have options to move from 'off' to on, but starting from the Holy Line target space

        // we set one piece off
        state.tokensBorneOff.add(state.tokensToStart.getFirst());
        assertEquals(1, state.getGameScore(0), 0.01);
        assertEquals(0, state.getGameScore(1), 0.01);
        state.board.get(1).add(state.tokensToStart.removeLast());  // block the 2 position onto the board
        state.board.get(2).add(state.tokensToStart.removeLast());  // block the 3 position onto the board

        state.setDieValue(2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new DoNothing(), actions.getFirst());
        // the above checks that we cannot move off the Holy Line (or any piece), until we have moved all pieces onto the board

        // we also need to move all of the other pieces on to the board, else we *must* play them
        for (int i = 0; i < 5; i++) {
            state.board.get(0).add(state.tokensToStart.removeFirst());
        }

        actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new PenteMoveAction(0, 2), actions.get(0));   // we do not block sharing on the sacred line with these rules
        assertEquals(new PenteMoveAction(10, 9), actions.get(1));
    }


    @Test
    public void testCanReEnterAfterBearingOffOrPassIfNoOtherMove() {
        params.setParameterValue("slideToMiddleOnSacredLine", true);
        params.setParameterValue("mustMoveFromSacredLine", false);
        params.setParameterValue("canMovePiecesBackOnToBoardAfterRemoval", true);
        params.setParameterValue("blotRuleActive", false);
        params.setParameterValue("startOffBoard", true);
        fm.setup(state);
        // The plan here is to move a piece off the board with slide activated
        // and confirm we have options to move from 'off' to on, but starting from the Holy Line target space

        // we set one piece off
        state.tokensBorneOff.add(state.tokensToStart.getFirst());
        assertEquals(1, state.getGameScore(0), 0.01);
        assertEquals(0, state.getGameScore(1), 0.01);
        state.board.get(0).add(state.tokensToStart.removeLast());  // block the 1 position onto the board
        state.board.get(1).add(state.tokensToStart.removeLast());  // block the 2 position onto the board

        state.setDieValue(1);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new DoNothing(), actions.getFirst());
        // the above checks that we cannot move off the Holy Line (or any piece), until we have moved all pieces onto the board

        // we also need to move all of the other pieces on to the board, else we *must* play them
        for (int i = 0; i < 5; i++) {
            state.board.get(0).add(state.tokensToStart.removeFirst());
        }

        actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new PenteMoveAction(10, 8), actions.get(0));   // we do not block sharing on the sacred line with these rules
        assertEquals(new DoNothing(), actions.get(1));
    }

    @Test
    public void testMustReEnterAfterBearingOff() {
        params.setParameterValue("slideToMiddleOnSacredLine", true);
        params.setParameterValue("mustMoveFromSacredLine", true);
        params.setParameterValue("canMovePiecesBackOnToBoardAfterRemoval", true);
        params.setParameterValue("blotRuleActive", false);
        params.setParameterValue("startOffBoard", true);
        fm.setup(state);

        // we set one piece off
        state.tokensBorneOff.add(state.tokensToStart.getFirst());
        assertEquals(1, state.getGameScore(0), 0.01);
        assertEquals(0, state.getGameScore(1), 0.01);
        state.board.get(1).add(state.tokensToStart.removeLast());  // block the 2 position onto the board
        state.board.get(2).add(state.tokensToStart.removeLast());  // block the 3 position onto the board

        state.setDieValue(2);
        // we also need to move all of the other pieces on to the board, else we *must* play them
        for (int i = 0; i < 5; i++) {
            state.board.get(0).add(state.tokensToStart.removeFirst());
        }

        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new PenteMoveAction(0, 2), actions.get(0));   // we do not block sharing on the sacred line with these rules
        assertEquals(new PenteMoveAction(10, 9), actions.get(1));
    }

    @Test
    public void testAbleToPassWithNoZugZwang() {
        params.setParameterValue("slideToMiddleOnSacredLine", false);
        params.setParameterValue("mustMoveFromSacredLine", false);
        params.setParameterValue("blotRuleActive", false);
        // this is like bearing off...but blocks the space.
        fm.setup(state);

        // we then repeat the set up for the Schaedler ZugZwang test
        // If the only move is off the sacred line, then so be it
        // we move all but one of player 0's pieces to (7)
        // we leave the remining one at (3)
        (new PenteMoveAction(0, 7)).execute(state);
        (new PenteMoveAction(1, 7)).execute(state);
        (new PenteMoveAction(2, 7)).execute(state);
        (new PenteMoveAction(4, 7)).execute(state);

        state.setDieValue(3);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(2, actions.size());
        assertEquals(new PenteMoveAction(7, 0), actions.get(0));
        assertEquals(new DoNothing(), actions.get(1));

        // then check that DoNothing moves on turn
        fm.next(state, new DoNothing());
        assertEquals(1, state.getCurrentPlayer());
    }

}

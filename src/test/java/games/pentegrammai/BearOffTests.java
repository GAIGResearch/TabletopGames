package games.pentegrammai;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
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
        params.setParameterValue("bearOffFromSacredLine", true);
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
        assertEquals(1, state.getBorneOff(0));
        assertEquals(0, state.getPiecesAtGoal(1));
        assertEquals(1, state.getGameScore(0), 0.001);
        assertEquals(0, state.getGameScore(1), 0.001);
    }

    @Test
    public void testAbleToPassWithNoZugZwang() {
        params.setParameterValue("bearOffFromSacredLine", false);
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
    }

}

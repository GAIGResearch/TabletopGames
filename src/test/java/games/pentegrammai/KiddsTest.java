package games.pentegrammai;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Token;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class KiddsTest {

    PenteGameState state;
    PenteParameters params;
    PenteForwardModel fm;

    @Before
    public void setUp() {
        params = new PenteParameters();
        params.setParameterValue("kiddsVariant", true);
        state = new PenteGameState(params, 2);
        fm = new PenteForwardModel();
        fm.setup(state);
    }

    @Test
    public void testInitialSetupHasCorrectPiecesAndOwners() {
        int piecesPerPlayer = params.boardSize / 2;
        assertEquals(piecesPerPlayer, state.getOffBoard(0));
        assertEquals(piecesPerPlayer, state.getOffBoard(1));
        // All other points should be empty
        for (int i = 0; i < params.boardSize; i++) {
            assertTrue("Point " + i + " should be empty", state.board.get(i).isEmpty());
        }
    }

    @Test
    public void testAvailableMovesForAllDieResultsFirstPlayer() {
        for (int die = 1; die <= 6; die++) {
            state.setDieValue(die);
            assertEquals(0, state.getCurrentPlayer());
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(1, actions.size());
            assertEquals(new PenteMoveAction(-1, die - 1), actions.get(0));
        }
    }

    @Test
    public void testAvailableMovesForAllDieResultsSecondPlayer() {
        // First, move player 0's token from 0 to 6 with a roll of 6
        state.setDieValue(6);
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0));

        // Now it's player 1's turn, all their tokens are off the board, and enter at 5
        assertEquals(1, state.getCurrentPlayer());
        for (int die = 1; die <= 6; die++) {
            state.setDieValue(die);
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            assertEquals(1, actions.size());
            assertEquals(new PenteMoveAction(-1, (5 + die - 1) % params.boardSize), actions.get(0));
        }
    }

    @Test
    public void testBlotWorks() {
        // to set this up we first move all player 1's tokens to 5
        for (int i = 0; i < params.boardSize / 2; i++) {
            List<Token> tokens = state.offBoard.stream().filter(t -> t.getOwnerId() == 1).toList();
            state.offBoard.removeAll(tokens);
            for (Token t : tokens) {
                state.board.get(5).add(t);
            }
        }

        // Player 1 (player 0) moves to 1 with a roll of 2
        state.setDieValue(2);
        assertEquals(0, state.getCurrentPlayer());
        fm.next(state, fm.computeAvailableActions(state).get(0));
        assertEquals(4, state.getOffBoard(0)); // Player 0 should have one token off the board
        assertEquals(0, state.getPiecesAt(1, 1));
        assertEquals(1, state.getPiecesAt(1, 0));

        // Now it's player 1's turn, all their tokens are at 5
        assertEquals(1, state.getCurrentPlayer());

        // Player 1 (player 1) rolls a 6, moves from 5 to 1 (where player 0 has a single token)
        state.setDieValue(6);
        List<core.actions.AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new PenteMoveAction(5, 1), actions.get(0));
        fm.next(state, actions.get(0));

        // And player 1 should have one token at 1
        assertEquals(1, state.getPiecesAt(1, 1));
        assertEquals(0, state.getPiecesAt(1, 0));

        assertEquals(5, state.getOffBoard(0)); // Player 0 should have one token off the board
        assertEquals(0, state.getOffBoard(1));
    }

    @Test
    public void mustMovePiecesOffBoardFirst() {
        // Player 0 has one token off the board, player 1 has none
        Token token = new Token("NewToken");
        token.setOwnerId(0);
        state.board.get(3).add(token);

        // Player 1 tries to move from 5 to 6 with a roll of 1
        state.setDieValue(2);
        assertEquals(0, state.getCurrentPlayer());
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertEquals(1, actions.size());
        assertEquals(new PenteMoveAction(-1, 1), actions.get(0));
    }

    @Test
    public void testWinningCondition() {
        // Play 5 moves for each player at random
        for (int i = 0; i < 10; i++) {
            List<AbstractAction> actions = fm.computeAvailableActions(state);
            // Randomly select an action
            AbstractAction action = actions.get(state.getRnd().nextInt(actions.size()));
            fm.next(state, action);
        }

        // Physically move all of player 0's pieces to sacred spot 7
        for (int i = 0; i < params.boardSize; i++) {
            state.board.get(i).removeIf(t -> t.getOwnerId() == 0);
        }
        state.offBoard.removeIf(t -> t.getOwnerId() == 0);
        for (int i = 0; i < params.boardSize / 2; i++) {
            Token t = new Token("P0_T" + i);
            t.setOwnerId(0);
            state.board.get(7).add(t);
        }

        // Move one of player 0's tokens from 7 to 5
        if (!state.canPlace(5))
            state.board.get(5).clear(); // Ensure 5 is empty
        new PenteMoveAction(7, 5).execute(state);

        assertEquals(0, state.getCurrentPlayer());

        state.setDieValue(2);
        List<AbstractAction> actions = fm.computeAvailableActions(state);
        assertTrue(actions.contains(new PenteMoveAction(5, 7)));

        // Execute the move
        fm.next(state, new PenteMoveAction(5, 7));

        // Now all player 0's tokens are at 7, so player 0 should win and game should be over
        assertFalse(state.isNotTerminal());
        assertEquals(1.0, state.getPlayerResults()[0].value, 0.001);
        assertEquals(-1.0, state.getPlayerResults()[1].value, 0.001);
    }
}

package games.pentegrammai;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Token;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class PenteTests {

    PenteGameState gameState;
    PenteParameters parameters;
    PenteForwardModel forwardModel;

    @Before
    public void setUp() {
        parameters = new PenteParameters();
        gameState = new PenteGameState(parameters, 2);
        forwardModel = new PenteForwardModel();
        forwardModel.setup(gameState);
    }

    @Test
    public void testInitialSetupHasCorrectPiecesAndOwners() {
        // There should be 10 spaces on the board
        assertEquals(parameters.boardSize, gameState.board.size());
        // Each space should have exactly one token, and the owner should be correct
        for (int i = 0; i < parameters.boardSize; i++) {
            List<Token> tokens = gameState.board.get(i);
            assertEquals("Space " + i + " should have exactly one token", 1, tokens.size());
            Token t = tokens.get(0);
            int expectedOwner = (i < parameters.boardSize / 2) ? 0 : 1;
            assertEquals("Token on space " + i + " should have correct owner", expectedOwner, t.getOwnerId());
        }
    }

    @Test
    public void testAvailableMovesForAllDieResultsFirstPlayer() {
        for (int die = 1; die <= 6; die++) {
            gameState.setDieValue(die);
            assertEquals(0, gameState.getCurrentPlayer());

            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);

            if (die == 1) {
                // Only move from 1 to 2 should be possible
                assertTrue(actions.contains(new PenteMoveAction(1, 2)));
                assertEquals(1, actions.size());
            } else if (die == 2) {
                // Only move from 0 to 2 should be possible
                assertTrue(actions.contains(new PenteMoveAction(0, 2)));
                assertEquals(1, actions.size());
            } else if (die == 3) {
                // Only move from 4 to 7 should be possible
                assertTrue(actions.contains(new PenteMoveAction(4, 7)));
                assertEquals(1, actions.size());
            } else if (die == 4) {
                // Only move from 3 to 7 should be possible
                assertTrue(actions.contains(new PenteMoveAction(3, 7)));
                assertEquals(1, actions.size());
            } else if (die == 5) {
                // Only move from 2 to 7 should be possible
                assertTrue(actions.contains(new PenteMoveAction(2, 7)));
                assertEquals(1, actions.size());
            } else {
                // Only move from 1 to 7 should be possible
                assertTrue(actions.contains(new PenteMoveAction(1, 7)));
                assertEquals(1, actions.size());
            }
        }
    }

    @Test
    public void testAvailableMovesForAllDieResultsSecondPlayer() {
        // First, player 1 (player 0) moves from 1 to 2 with a roll of 1
        gameState.setDieValue(1);
        forwardModel.next(gameState, new PenteMoveAction(1, 2));
        // Now it's player 2's (player 1) turn
        assertEquals(1, gameState.getCurrentPlayer());

        // For each die roll, check the available moves for player 1
        for (int die = 1; die <= 6; die++) {
            gameState.setDieValue(die);
            assertEquals(1, gameState.getCurrentPlayer());
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);

            if (die == 1) {
                // Only move from 6 to 7 should be possible
                assertTrue(actions.contains(new PenteMoveAction(6, 7)));
                assertEquals(1, actions.size());
            } else if (die == 2) {
                // Move from 5 to 7 and from 9 to 1 (now vacant)
                assertTrue(actions.contains(new PenteMoveAction(5, 7)));
                assertTrue(actions.contains(new PenteMoveAction(9, 1)));
                assertEquals(2, actions.size());
            } else if (die == 3) {
                // Move from 9 to 2 (goal, wraps), and from 8 to 1 (now vacant)
                assertTrue(actions.contains(new PenteMoveAction(9, 2)));
                assertTrue(actions.contains(new PenteMoveAction(8, 1)));
                assertEquals(2, actions.size());
            } else if (die == 4) {
                // Move from 8 to 2 (goal), and from 7 to 1 (now vacant)
                assertTrue(actions.contains(new PenteMoveAction(8, 2)));
                assertTrue(actions.contains(new PenteMoveAction(7, 1)));
                assertEquals(2, actions.size());
            } else if (die == 5) {
                // Move from 7 to 2 (goal), and from 6 to 1 (now vacant)
                assertTrue(actions.contains(new PenteMoveAction(7, 2)));
                assertTrue(actions.contains(new PenteMoveAction(6, 1)));
                assertEquals(2, actions.size());
            } else {
                // die == 6: Move from 6 to 2 (goal), and from 5 to 1 (now vacant)
                assertTrue(actions.contains(new PenteMoveAction(6, 2)));
                assertTrue(actions.contains(new PenteMoveAction(5, 1)));
                assertEquals(2, actions.size());
                forwardModel.next(gameState, actions.get(0));
                assertEquals(0, gameState.getCurrentPlayer());
            }
        }
    }

    @Test
    public void testNoMovesAvailableResultsInDoNothing() {
        // Move player 0's token from 1 to 2, so 1 is empty and 2 has two tokens
        gameState.setDieValue(1);
        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, actions.size());
        assertEquals(new PenteMoveAction(1, 2), actions.get(0));

        new PenteMoveAction(1, 2).execute(gameState);
        actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, actions.size());
        assertEquals(new PenteMoveAction(0, 1), actions.get(0));

        new PenteMoveAction(0, 2).execute(gameState);
        actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, actions.size());
        assertEquals(new DoNothing(), actions.get(0));
        gameState.setDieValue(1);


    }

    @Test
    public void testWinningCondition() {
        // Play 5 moves for each player at random
        for (int i = 0; i < 10; i++) {
            List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
            // Randomly select an action
            AbstractAction action = actions.get(gameState.getRnd().nextInt(actions.size()));
            forwardModel.next(gameState, action);
        }

        // Physically move all of player 0's pieces to sacred spot 7
        for (int i = 0; i < parameters.boardSize; i++) {
            gameState.board.get(i).removeIf(t -> t.getOwnerId() == 0);
        }
        for (int i = 0; i < parameters.boardSize / 2; i++) {
            Token t = new Token("P0_T" + i);
            t.setOwnerId(0);
            gameState.board.get(7).add(t);
        }

        // Move one of player 0's tokens from 7 to 5
        if (!gameState.canPlace(5))
            gameState.board.get(5).clear(); // Ensure 5 is empty
        new PenteMoveAction(7, 5).execute(gameState);

        assertEquals(0, gameState.getCurrentPlayer());

        gameState.setDieValue(2);
        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, actions.size());
        assertEquals(new PenteMoveAction(5, 7), actions.get(0));

        // Execute the move
        forwardModel.next(gameState, actions.get(0));

        // Now all player 0's tokens are at 7, so player 0 should win and game should be over
        assertFalse(gameState.isNotTerminal());
        assertEquals(1.0, gameState.getPlayerResults()[0].value, 0.001);
        assertEquals(-1.0, gameState.getPlayerResults()[1].value, 0.001);
    }
}

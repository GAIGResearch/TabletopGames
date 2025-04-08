package games.backgammon;


import core.actions.AbstractAction;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class BasicMoves {

    BGGameState gameState;
    BGParameters parameters;
    BGForwardModel forwardModel;

    @Before
    public void setUp() {
        parameters = new BGParameters();
        gameState = new BGGameState(parameters, 2);
        forwardModel = new BGForwardModel();
        forwardModel.setup(gameState);
    }

    @Test
    public void testInitialSetup() {
        assertEquals(24, gameState.piecesPerPoint[0].length);
        for (int i = 0; i < 24; i++) {
            //          System.out.println("piecesPerPoint[*][" + i + "] = " + gameState.piecesPerPoint[0][i]+ ", " + gameState.piecesPerPoint[1][i]);
            switch (i) {
                case 5:
                    assertEquals(parameters.startingAt6, gameState.piecesPerPoint[0][i]);
                    assertEquals(parameters.startingAt6, gameState.piecesPerPoint[1][i]);
                    break;
                case 7:
                    assertEquals(parameters.startingAt8, gameState.piecesPerPoint[0][i]);
                    assertEquals(parameters.startingAt8, gameState.piecesPerPoint[1][i]);
                    break;
                case 12:
                    assertEquals(parameters.startingAt13, gameState.piecesPerPoint[0][i]);
                    assertEquals(parameters.startingAt13, gameState.piecesPerPoint[1][i]);
                    break;
                case 23:
                    assertEquals(parameters.startingAt24, gameState.piecesPerPoint[0][i]);
                    assertEquals(parameters.startingAt24, gameState.piecesPerPoint[1][i]);
                    break;
                default:
                    assertEquals(0, gameState.piecesPerPoint[0][i]);
                    assertEquals(0, gameState.piecesPerPoint[1][i]);
            }
        }
        assertEquals(0, gameState.piecesOnBar[0]);
        assertEquals(0, gameState.piecesOnBar[1]);
        assertEquals(0, gameState.piecesBorneOff[0]);
        assertEquals(0, gameState.piecesBorneOff[1]);
    }

    @Test
    public void testInitialMoves() {
        gameState.setDiceValues(new int[]{1, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);

        // Add assertions to check the expected moves
        // for each of the two dice values, player 0 should be able to move their pieces from any starting point
        // to any point that is not occupied by two or more opponent pieces

        assertTrue(availableActions.contains(new MovePiece(5, 4)));
        assertTrue(availableActions.contains(new MovePiece(7, 6)));
        assertFalse(availableActions.contains(new MovePiece(12, 11)));  // due to black pieces
        assertTrue(availableActions.contains(new MovePiece(23, 22)));
        assertTrue(availableActions.contains(new MovePiece(5, 1)));
        assertTrue(availableActions.contains(new MovePiece(7, 3)));
        assertTrue(availableActions.contains(new MovePiece(12, 8)));
        assertTrue(availableActions.contains(new MovePiece(23, 19)));
        assertEquals(7, availableActions.size());
    }

    @Test
    public void testInitialMovesToOwnStacks() {
        gameState.setDiceValues(new int[]{2, 2});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(5, 3)));
        assertTrue(availableActions.contains(new MovePiece(7, 5)));
        assertTrue(availableActions.contains(new MovePiece(12, 10)));
        assertTrue(availableActions.contains(new MovePiece(23, 21)));
        assertEquals(4, availableActions.size());
    }

    @Test
    public void testCannotBearOffAtStart() {
        gameState.setDiceValues(new int[]{5, 6});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertFalse(availableActions.contains(new MovePiece(5, 0)));  // black pieces in the way
        assertTrue(availableActions.contains(new MovePiece(7, 2)));
        assertTrue(availableActions.contains(new MovePiece(12, 7)));
        assertFalse(availableActions.contains(new MovePiece(23, 18))); // black pieces in the way
        assertFalse(availableActions.contains(new MovePiece(5, -1)));  // no bearing off
        assertTrue(availableActions.contains(new MovePiece(7, 1)));
        assertTrue(availableActions.contains(new MovePiece(12, 6)));
        assertTrue(availableActions.contains(new MovePiece(23, 17)));
        assertEquals(5, availableActions.size());
    }

    @Test
    public void testMovePieceActionDoes() {
        gameState.setDiceValues(new int[]{1, 4});
        assertEquals(5, gameState.piecesPerPoint[0][5]);
        assertEquals(0, gameState.piecesPerPoint[0][4]);
        forwardModel.next(gameState, new MovePiece(5, 4));
        assertEquals(4, gameState.piecesPerPoint[0][5]);
        assertEquals(1, gameState.piecesPerPoint[0][4]);
        // and only one dice value available
        assertEquals(4, gameState.getAvailableDiceValues()[0]);
        assertEquals(1, gameState.getAvailableDiceValues().length);
        assertEquals(0, gameState.getCurrentPlayer());
    }

    @Test
    public void testHitOnOpponentPiece() {
        gameState.setDiceValues(new int[]{1, 4});
        forwardModel.next(gameState, new MovePiece(5, 4));
        assertEquals(0, gameState.getCurrentPlayer());
        forwardModel.next(gameState, new MovePiece(7, 3));
        assertEquals(1, gameState.getCurrentPlayer());
        assertEquals(1, gameState.getTurnCounter());
        assertEquals(2, gameState.getAvailableDiceValues().length);

        gameState.setDiceValues(new int[]{1, 4});
        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        assertTrue(actions.contains(new MovePiece(5, 4)));
        assertTrue(actions.contains(new MovePiece(7, 6)));
        assertFalse(actions.contains(new MovePiece(12, 11))); // due to white pieces
        assertTrue(actions.contains(new MovePiece(23, 22)));
        assertTrue(actions.contains(new MovePiece(5, 1)));
        assertTrue(actions.contains(new MovePiece(7, 3)));
        assertTrue(actions.contains(new MovePiece(12, 8)));
        assertTrue(actions.contains(new MovePiece(23, 19)));  // this should hit
        assertEquals(7, actions.size());

        // Now we take the hit
        forwardModel.next(gameState, new MovePiece(23, 19));
        assertEquals(0, gameState.getPiecesOnPoint(0, 4));
        assertEquals(1, gameState.getPiecesOnPoint(1, 19));
        assertEquals(1, gameState.getPiecesOnBar(0));
        assertEquals(1, gameState.blots[0]);
        assertEquals(0, gameState.blots[1]);
    }

    @Test
    public void mustMoveFromBarFirst() {
        // set a piece on the bar  (technically this means we have 16 pieces)
        gameState.piecesOnBar[0] = 1;
        gameState.setDiceValues(new int[]{2, 3});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(-1, 1)));
        assertTrue(availableActions.contains(new MovePiece(-1, 2)));
        assertEquals(2, availableActions.size());

        gameState.movePiece(1, 12, 22);
        assertEquals(1, gameState.getPiecesOnPoint(1, 22));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(-1, 1)));
        assertTrue(availableActions.contains(new MovePiece(-1, 2)));
        assertEquals(2, availableActions.size());

        gameState.movePiece(1, 12, 22);
        assertEquals(2, gameState.getPiecesOnPoint(1, 22));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertFalse(availableActions.contains(new MovePiece(-1, 1)));
        assertTrue(availableActions.contains(new MovePiece(-1, 2)));
        assertEquals(1, availableActions.size());
    }

    @Test
    public void turnSkippedIfPiecesOnBarAndNoMovePossible() {
        fail("Not yet implemented");
    }

    @Test
    public void mayOnlyBearOffOnceAllPiecesInHomeBoard() {
        fail("Not yet implemented");
    }

    @Test
    public void gameEndsOnceAllPiecesBorneOff() {
        fail("Not yet implemented");
    }
}


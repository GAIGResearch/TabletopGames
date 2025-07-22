package games.backgammon;

import games.XIIScripta.XIIGameState;
import games.XIIScripta.XIIParameters;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XIITests {

    XIIGameState gameState;
    XIIParameters parameters;
    BGForwardModel forwardModel;

    @Before
    public void setUp() {
        parameters = new XIIParameters();
        gameState = new XIIGameState(parameters, 2);
        forwardModel = new BGForwardModel();
        forwardModel.setup(gameState);
    }

    @Test
    public void testPhysicalSpacesMatchTracker() {
        assertEquals(37, gameState.counters.size());
        assertEquals(36, gameState.playerTrackMapping[0].length);
        for (int i = 0; i < 36; i++) {
            assertEquals(36 - i, gameState.getPhysicalSpace(0, i));
            assertEquals(36 - i, gameState.getPhysicalSpace(1, i));
        }
    }

    @Test
    public void testInitialSetupByClassicNumbering() {
        for (int i = 1; i <= 36; i++) {
            System.out.printf("Checking point %d, P0: %d, P1: %d%n", i,
                    gameState.getPiecesOnPoint(0, i),
                    gameState.getPiecesOnPoint(1, i)
            );
            switch (i) {
                case 0:
                    assertEquals(parameters.startingAtBar, gameState.getPiecesOnPoint(0, i));
                    assertEquals(parameters.startingAtBar, gameState.getPiecesOnPoint(1, i));
                    break;
                default:
                    // all other points should have no pieces
                    assertEquals(0, gameState.getPiecesOnPoint(0, i));
                    assertEquals(0, gameState.getPiecesOnPoint(1, i));
            }
        }
        assertEquals(15, gameState.getPiecesOnBar(0));
        assertEquals(15, gameState.getPiecesOnBar(1));
        assertEquals(0, gameState.piecesBorneOff[0]);
        assertEquals(0, gameState.piecesBorneOff[1]);
    }

    @Test
    public void testInitialMoves() {
        gameState.setDiceValues(new int[]{1, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);

        assertTrue(availableActions.contains(new MovePiece(0, 36)));
        assertTrue(availableActions.contains(new MovePiece(0, 33)));
        assertEquals(2, availableActions.size());
    }

    @Test
    public void testSecondPlayerCanBlotFirstMoves() {
        gameState.setDiceValues(new int[]{2, 3});
        forwardModel.next(gameState, new MovePiece(0, 35));
        forwardModel.next(gameState, new MovePiece(0, 34));
        assertEquals(1, gameState.getCurrentPlayer());
        gameState.setDiceValues(new int[]{3, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 34)));
        assertTrue(availableActions.contains(new MovePiece(0, 33)));
        assertEquals(2, availableActions.size());

        forwardModel.next(gameState, new MovePiece(0, 34));
        assertEquals(0, gameState.getPiecesOnPoint(0, 34));
        assertEquals(1, gameState.getPiecesOnPoint(1, 34));
        assertEquals(1, gameState.blots[0]);
    }

    @Test
    public void testSecondPlayerCannotBlotStacks() {
        gameState.setDiceValues(new int[]{3, 3});
        forwardModel.next(gameState, new MovePiece(0, 34));
        forwardModel.next(gameState, new MovePiece(0, 34));
        assertEquals(1, gameState.getCurrentPlayer());
        gameState.setDiceValues(new int[]{3, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 33)));
        assertEquals(1, availableActions.size());

        forwardModel.next(gameState, new MovePiece(0, 33));
        assertEquals(1, gameState.getCurrentPlayer());
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(33, 30)));
        assertEquals(1, availableActions.size());
    }


    @Test
    public void mustMoveAllOntoRegionAFirst() {
        // first we move all pieces except 1 to region A
        int piecesOnBar = gameState.getPiecesOnBar(0);
        for (int i = 0; i < piecesOnBar - 1; i++) {
            gameState.movePiece(0, 0, 28);
        }
        gameState.setDiceValues(new int[]{2, 3});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 35)));
        assertTrue(availableActions.contains(new MovePiece(0, 34)));
        assertTrue(availableActions.contains(new MovePiece(28, 26)));
        assertTrue(availableActions.contains(new MovePiece(28, 25)));
        assertEquals(4, availableActions.size());

        gameState.setDiceValues(new int[]{5, 5});
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 32)));
        assertEquals(1, availableActions.size());

        gameState.movePiece(0, 0, 32);
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(32, 27)));
        assertTrue(availableActions.contains(new MovePiece(28, 23)));
        assertEquals(2, availableActions.size());
    }


}

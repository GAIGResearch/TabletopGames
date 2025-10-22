package games.backgammon;

import games.XIIScripta.XIIParameters;
import org.junit.Before;
import org.junit.Test;

import static games.backgammon.BGParameters.EntryRule.Bar;
import static games.backgammon.BGParameters.Route.CommonHalfA;
import static org.junit.Assert.*;

public class XIITests {

    BGGameState gameState;
    XIIParameters parameters;
    BGForwardModel forwardModel;

    @Before
    public void setUp() {
        parameters = new XIIParameters();
        gameState = new BGGameState(parameters, 2);
        forwardModel = new BGForwardModel();
        forwardModel.setup(gameState);
    }

    public void initialiseHalfA() {
        parameters.setParameterValue("route", CommonHalfA);
        parameters.setParameterValue("entryRule", Bar);
        parameters.setParameterValue("entryBoardSize", 6);
        gameState = new BGGameState(parameters, 2);
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
    public void testPhysicalSpacesMatchTrackerHalfA() {
        initialiseHalfA();
        assertEquals(37, gameState.counters.size());
        assertEquals(30, gameState.playerTrackMapping[0].length);
        assertEquals(30, gameState.playerTrackMapping[1].length);
        for (int i = 0; i < 36; i++) {
            switch (i) {
                case 0, 1, 2, 3, 4, 5:
                    assertEquals(36 - i, gameState.getPhysicalSpace(0, i));
                    assertEquals(30 - i, gameState.getPhysicalSpace(1, i));
                    break;
                case 30, 31, 32, 33, 34, 35:
                    int finalI = i;
                    assertThrows(ArrayIndexOutOfBoundsException.class, () -> gameState.getPhysicalSpace(0, finalI));
                    assertThrows(ArrayIndexOutOfBoundsException.class, () -> gameState.getPhysicalSpace(1, finalI));
                    break;
                default:
                    assertEquals(30 - i, gameState.getPhysicalSpace(0, i));
                    assertEquals(30 - i, gameState.getPhysicalSpace(1, i));
            }
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
    public void testDoubleMoveToMoveOnToBoard() {
        gameState.setDiceValues(new int[]{1, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);

        assertTrue(availableActions.contains(new MovePiece(0, 36)));
        assertTrue(availableActions.contains(new MovePiece(0, 33)));
        assertEquals(2, availableActions.size());

        // if we then move one piece, we can continue to move that piece
        forwardModel.next(gameState, new MovePiece(0, 36));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(0, gameState.getCurrentPlayer());
        assertTrue(availableActions.contains(new MovePiece(0, 33)));
        assertTrue(availableActions.contains(new MovePiece(36, 32)));
    }

    @Test
    public void testNoDoubleMoveToMoveOnToBoardWithHalfA() {
        initialiseHalfA();
        gameState.setDiceValues(new int[]{1, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);

        assertTrue(availableActions.contains(new MovePiece(0, 36)));
        assertTrue(availableActions.contains(new MovePiece(0, 33)));
        assertEquals(2, availableActions.size());

        // if we then move one piece, we cannot continue to move that piece
        forwardModel.next(gameState, new MovePiece(0, 36));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(0, gameState.getCurrentPlayer());
        assertTrue(availableActions.contains(new MovePiece(0, 33)));
        assertEquals(1, availableActions.size());
    }

    @Test
    public void firstPlayerJumpsOverOtherPlayersHomeBoardWithHalfA() {
        initialiseHalfA();
        do {
            gameState.movePiece(0, 0, 31);
        } while (gameState.getPiecesOnBar(0) > 0);

        gameState.setDiceValues(new int[]{4, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(31, 21)));
        assertEquals(1, availableActions.size());
        forwardModel.next(gameState, new MovePiece(31, 21));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(0, gameState.getCurrentPlayer());
        assertTrue(availableActions.contains(new MovePiece(21, 17)));
        assertTrue(availableActions.contains(new MovePiece(31, 21)));
        assertEquals(2, availableActions.size());
    }

    @Test
    public void secondPlayerMovesToOwnHalfBoardWithHalfA() {
        initialiseHalfA();
        do {
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
        } while (gameState.getCurrentPlayer() == 0);

        gameState.setDiceValues(new int[]{1, 4});
        var availableActions = forwardModel.computeAvailableActions(gameState);

        assertTrue(availableActions.contains(new MovePiece(0, 30)));
        assertTrue(availableActions.contains(new MovePiece(0, 27)));
        assertEquals(2, availableActions.size());

        // if we then move one piece, we cannot continue to move that piece
        forwardModel.next(gameState, new MovePiece(0, 30));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, gameState.getCurrentPlayer());
        assertTrue(availableActions.contains(new MovePiece(0, 27)));
        assertEquals(1, availableActions.size());
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
        gameState.movedThisTurn.clear();
        gameState.setDiceValues(new int[]{2, 3});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 35)));
        assertTrue(availableActions.contains(new MovePiece(0, 34)));
        assertEquals(2, availableActions.size());

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

    @Test
    public void canOnlyApplyLaterDieToPieceAlreadyMovedIfStuffOnBar() {
        // we first move a number of pieces onto the board
        gameState.movePiece(0, 0, 36);
        gameState.movePiece(0, 0, 31);
        gameState.movePiece(0, 0, 31);
        gameState.movedThisTurn.clear();

        gameState.setDiceValues(new int[]{2, 3});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 34)));
        assertTrue(availableActions.contains(new MovePiece(0, 35)));
        assertEquals(2, availableActions.size());

        // if we then move one piece, we can continue to move that piece
        forwardModel.next(gameState, new MovePiece(0, 34));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(0, gameState.getCurrentPlayer());
        assertTrue(availableActions.contains(new MovePiece(0, 35)));
        assertTrue(availableActions.contains(new MovePiece(34, 32)));
        assertEquals(2, availableActions.size());
    }

    @Test
    public void canOnlyApplyLaterDieToPieceAlreadyMovedIfStuffOnBarWithDoubles() {
        // and if we also have doubles, we can move the same piece multiple times as long as it does not leave the entry board
        gameState.movePiece(0, 0, 36);
        gameState.movePiece(0, 0, 31);
        gameState.movePiece(0, 0, 31);
        gameState.movedThisTurn.clear();

        gameState.setDiceValues(new int[]{5, 5});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 32)));
        assertEquals(1, availableActions.size());

        forwardModel.next(gameState, new MovePiece(0, 32));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(0, gameState.getCurrentPlayer());
        assertTrue(availableActions.contains(new MovePiece(0, 32)));
        assertTrue(availableActions.contains(new MovePiece(32, 27)));
        assertEquals(2, availableActions.size());

        forwardModel.next(gameState, new MovePiece(32, 27));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(0, gameState.getCurrentPlayer());
        assertFalse(availableActions.contains(new MovePiece(27, 22))); // this would take us off the entry board
        assertTrue(availableActions.contains(new MovePiece(0, 32)));
        assertEquals(1, availableActions.size());
    }

    @Test
    public void allOnHomeBoardToBearOffWithHalfA() {
        initialiseHalfA();
        // first we move all pieces except 1 to region A
        int piecesOnBar = gameState.getPiecesOnBar(0);
        for (int i = 0; i < piecesOnBar - 1; i++) {
            gameState.movePiece(0, 0, 3);
        }
        gameState.movePiece(0, 0, 7);
        gameState.movedThisTurn.clear();
        gameState.setDiceValues(new int[]{5, 6});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(7, 1)));
        assertTrue(availableActions.contains(new MovePiece(7, 2)));
        assertEquals(2, availableActions.size());

        forwardModel.next(gameState, new MovePiece(7, 2));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(0, gameState.getCurrentPlayer());
        assertTrue(availableActions.contains(new MovePiece(2, -1)));
        assertTrue(availableActions.contains(new MovePiece(3, -1)));
        assertEquals(2, availableActions.size());

    }

    @Test
    public void testBackgammonWithCustomDice() {
        BGParameters params = new BGParameters();
        params.setParameterValue("diceJSON", "src/test/resources/custom_d6.json");
        params.setParameterValue("doubleActions", false);
        BGGameState gameState = new BGGameState(params, 2);
        forwardModel.setup(gameState);

        int[] countsDie1 = new int[6];
        int[] countsDie2 = new int[6];

        for (int i = 0; i < 100; i++) {
            gameState.rollDice();
            int[] values = gameState.getDiceValues();
            assertEquals(2, values.length);
            assertTrue(values[0] >= 1 && values[0] <= 6);
            assertTrue(values[1] >= 1 && values[1] <= 6);
            countsDie1[values[0] - 1]++;
            countsDie2[values[1] - 1]++;
        }

        int[] expected = new int[] {5, 10, 10, 20, 25, 30};
        for (int i = 0; i < 6; i++) {
            assertEquals("Die 1 side " + (i+1) + " count mismatch", expected[i], countsDie1[i], 8);
            assertEquals("Die 2 side " + (i+1) + " count mismatch", expected[i], countsDie2[i], 8);
        }
    }

}

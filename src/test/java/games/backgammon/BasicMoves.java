package games.backgammon;


import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DoNothing;
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
        assertTrue(availableActions.contains(new MovePiece(-1, 22)));
        assertTrue(availableActions.contains(new MovePiece(-1, 21)));
        assertEquals(2, availableActions.size());

        gameState.movePiece(1, 12, 2);
        assertEquals(1, gameState.getPiecesOnPoint(1, 2));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(-1, 22)));
        assertTrue(availableActions.contains(new MovePiece(-1, 21)));
        assertEquals(2, availableActions.size());

        gameState.movePiece(1, 12, 2);
        assertEquals(2, gameState.getPiecesOnPoint(1, 2));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertFalse(availableActions.contains(new MovePiece(-1, 21)));
        assertTrue(availableActions.contains(new MovePiece(-1, 22)));
        assertEquals(1, availableActions.size());
    }

    @Test
    public void turnSkippedIfPiecesOnBarAndNoMovePossible() {
        gameState.piecesOnBar[0] = 2;
        gameState.setDiceValues(new int[]{2, 3});

        gameState.movePiece(1, 12, 1);
        gameState.movePiece(1, 12, 1);
        assertEquals(2, gameState.getPiecesOnPoint(1, 1));

        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertFalse(availableActions.contains(new MovePiece(-1, 22)));
        assertTrue(availableActions.contains(new MovePiece(-1, 21)));
        assertEquals(1, availableActions.size());

        forwardModel.next(gameState, availableActions.get(0));
        assertEquals(1, gameState.getPiecesOnPoint(0, 21));
        assertEquals(1, gameState.getPiecesOnBar(0));
        assertEquals(1, gameState.getCurrentPlayer());
        assertEquals(2, gameState.getAvailableDiceValues().length);
    }

    @Test
    public void mayOnlyBearOffOnceAllPiecesInHomeBoard() {
        // first we move all pieces to the homeboard of player 1
        for (int pos = 6; pos < 24; pos++) {
            for (int i = gameState.getPiecesOnPoint(1, pos); i > 0; i--)
                gameState.movePiece(1, pos, 4);  // a pretty random point in the home board
        }
        // take two actions for player 0
        gameState.setDiceValues(new int[]{5, 6});
        do {
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
        } while (gameState.getCurrentPlayer() == 0);
        gameState.movePiece(1, 4, 20);

        gameState.setDiceValues(new int[]{5, 6});
        assertEquals(1, gameState.getCurrentPlayer());
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(2, availableActions.size());
        assertTrue(availableActions.contains(new MovePiece(20, 14)));
        assertTrue(availableActions.contains(new MovePiece(20, 15)));

        gameState.movePiece(1, 20, 5);
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(2, availableActions.size());
        assertTrue(availableActions.contains(new MovePiece(4, -1)));
        assertTrue(availableActions.contains(new MovePiece(5, -1)));

        forwardModel.next(gameState, new MovePiece(5, -1));
        assertEquals(1, gameState.getPiecesBorneOff(1));
        assertEquals(1, gameState.getGameScore(1), 0.01);
        assertEquals(0, gameState.getGameScore(0), 0.01);
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, availableActions.size());
        assertTrue(availableActions.contains(new MovePiece(4, -1)));
    }

    @Test
    public void bearingOffUsesLowestDie() {
        // first we move all pieces to the homeboard of player 1
        for (int pos = 5; pos < 24; pos++) {
            for (int i = gameState.getPiecesOnPoint(1, pos); i > 0; i--)
                gameState.movePiece(1, pos, 1); // just needs a 2
        }
        // take two actions for player 0
        gameState.setDiceValues(new int[]{2, 3});
        do {
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
        } while (gameState.getCurrentPlayer() == 0);

        gameState.setDiceValues(new int[]{2, 4});
        assertEquals(1, gameState.getCurrentPlayer());
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, availableActions.size());
        assertTrue(availableActions.contains(new MovePiece(1, -1)));

        forwardModel.next(gameState, availableActions.get(0));
        assertEquals(1, gameState.getPiecesBorneOff(1));
        assertEquals(4, gameState.getAvailableDiceValues()[0]);
    }

    @Test
    public void gameEndsOnceAllPiecesBorneOff() {
        mayOnlyBearOffOnceAllPiecesInHomeBoard();
        // this sets up player 1 to just bear off
        do {
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
        } while (gameState.isNotTerminal());
        assertEquals(15, gameState.getPiecesBorneOff(1));
        assertEquals(15, gameState.getGameScore(1), 0.01);
        assertEquals(CoreConstants.GameResult.WIN_GAME, gameState.getPlayerResults()[1]);
        assertEquals(CoreConstants.GameResult.LOSE_GAME, gameState.getPlayerResults()[0]);
        assertTrue(gameState.getRoundCounter() > 6);
    }

    @Test
    public void passActionIfNoMovesPossibleAtStartOfPlayersTurn() {
        // first we move all pieces of player 0 to position 12 (point 13)
        for (int pos = 0; pos < 24; pos++) {
            if (pos == 12) continue;
            for (int i = gameState.getPiecesOnPoint(0, pos); i > 0; i--)
                gameState.movePiece(0, pos, 12);  // a pretty random point in the home board
        }
        gameState.setDiceValues(new int[]{1, 1});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, availableActions.size());
        assertEquals(new DoNothing(), availableActions.get(0));
    }

    @Test
    public void twoMovesPerTurn() {
        for (int t = 0; t < 10; t++) {
            assertEquals(t % 2, gameState.getTurnCounter());
            assertEquals(t % 2, gameState.getCurrentPlayer());
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
            assertEquals(1, gameState.getAvailableDiceValues().length);
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
            assertEquals(2, gameState.getAvailableDiceValues().length);
        }
    }

    @Test
    public void twoTurnsPerRound() {
        for (int t = 0; t < 10; t++) {
            assertEquals(t / 2, gameState.getRoundCounter());
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
            assertEquals(1, gameState.getAvailableDiceValues().length);
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
            assertEquals(2, gameState.getAvailableDiceValues().length);
        }
    }
}


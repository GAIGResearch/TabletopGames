package games.backgammon;


import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Token;
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
        assertEquals(25, gameState.counters.size());
        for (int i = 0; i < 24; i++) {
            int physicalRefP0 = gameState.getPhysicalSpace(0, i);
            int physicalRefP1 = gameState.getPhysicalSpace(1, i);
            System.out.printf("Checking point %d, P0: %d/%d, P1: %d/%d\n", i,
                    physicalRefP0, gameState.getPiecesOnPoint(0, physicalRefP0),
                    physicalRefP1, gameState.getPiecesOnPoint(1, physicalRefP1)
            );
            switch (i) {
                case 5:
                    assertEquals(parameters.startingAt6, gameState.getPiecesOnPoint(0, physicalRefP0));
                    assertEquals(parameters.startingAt6, gameState.getPiecesOnPoint(1, physicalRefP1));
                    break;
                case 7:
                    assertEquals(parameters.startingAt8, gameState.getPiecesOnPoint(0, physicalRefP0));
                    assertEquals(parameters.startingAt8, gameState.getPiecesOnPoint(1, physicalRefP1));
                    break;
                case 12:
                    assertEquals(parameters.startingAt13, gameState.getPiecesOnPoint(0, physicalRefP0));
                    assertEquals(parameters.startingAt13, gameState.getPiecesOnPoint(1, physicalRefP1));
                    break;
                case 23:
                    assertEquals(parameters.startingAt24, gameState.getPiecesOnPoint(0, physicalRefP0));
                    assertEquals(parameters.startingAt24, gameState.getPiecesOnPoint(1, physicalRefP1));
                    break;
                default:
                    // all other points should have no pieces
                    assertEquals(0, gameState.getPiecesOnPoint(0, physicalRefP0));
                    assertEquals(0, gameState.getPiecesOnPoint(1, physicalRefP1));
            }
        }
        assertEquals(0, gameState.getPiecesOnBar(0));
        assertEquals(0, gameState.getPiecesOnBar(1));
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

        assertTrue(availableActions.contains(new MovePiece(24, 23)));
        assertFalse(availableActions.contains(new MovePiece(13, 12)));  // due to black pieces
        assertTrue(availableActions.contains(new MovePiece(8, 7)));
        assertTrue(availableActions.contains(new MovePiece(6, 5)));
        assertTrue(availableActions.contains(new MovePiece(24, 20)));
        assertTrue(availableActions.contains(new MovePiece(13, 9)));
        assertTrue(availableActions.contains(new MovePiece(8, 4)));
        assertTrue(availableActions.contains(new MovePiece(6, 2)));
        assertEquals(7, availableActions.size());
    }

    @Test
    public void testInitialMovesToOwnStacks() {
        gameState.setDiceValues(new int[]{2, 2});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(6, 4)));
        assertTrue(availableActions.contains(new MovePiece(8, 6)));
        assertTrue(availableActions.contains(new MovePiece(13, 11)));
        assertTrue(availableActions.contains(new MovePiece(24, 22)));
        assertEquals(4, availableActions.size());
    }

    @Test
    public void testCannotBearOffAtStart() {
        gameState.setDiceValues(new int[]{5, 6});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(13, 8)));
        assertTrue(availableActions.contains(new MovePiece(8, 3)));
        assertTrue(availableActions.contains(new MovePiece(13, 7)));
        assertFalse(availableActions.contains(new MovePiece(24, 19))); // black pieces in the way
        assertFalse(availableActions.contains(new MovePiece(6, -1)));  // no bearing off
        assertTrue(availableActions.contains(new MovePiece(8, 2)));
        assertTrue(availableActions.contains(new MovePiece(13, 7)));
        assertTrue(availableActions.contains(new MovePiece(24, 18)));
        assertEquals(5, availableActions.size());
    }

    @Test
    public void testMovePieceActionDoes() {
        gameState.setDiceValues(new int[]{1, 4});
        assertEquals(5, gameState.getPiecesOnPoint(0, 6));
        assertEquals(0, gameState.getPiecesOnPoint(0, 5));
        forwardModel.next(gameState, new MovePiece(6, 5));
        assertEquals(4, gameState.getPiecesOnPoint(0, 6));
        assertEquals(1, gameState.getPiecesOnPoint(0, 5));
        // and only one dice value available
        assertEquals(4, gameState.getAvailableDiceValues()[0]);
        assertEquals(1, gameState.getAvailableDiceValues().length);
        assertEquals(0, gameState.getCurrentPlayer());
    }

    @Test
    public void testHitOnOpponentPiece() {
        gameState.setDiceValues(new int[]{1, 4});
        forwardModel.next(gameState, new MovePiece(6, 5));
        assertEquals(0, gameState.getCurrentPlayer());
        forwardModel.next(gameState, new MovePiece(8, 4));
        assertEquals(1, gameState.getCurrentPlayer());
        assertEquals(1, gameState.getTurnCounter());
        assertEquals(2, gameState.getAvailableDiceValues().length);

        // now black's turn
        gameState.setDiceValues(new int[]{1, 4});
        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        assertTrue(actions.contains(new MovePiece(1, 2)));
        assertTrue(actions.contains(new MovePiece(1, 5)));  // this should hit
        assertFalse(actions.contains(new MovePiece(12, 13))); // due to white pieces
        assertTrue(actions.contains(new MovePiece(12, 16)));
        assertTrue(actions.contains(new MovePiece(17, 18)));
        assertTrue(actions.contains(new MovePiece(17, 21)));
        assertTrue(actions.contains(new MovePiece(19, 20)));
        assertTrue(actions.contains(new MovePiece(19, 23)));
        assertEquals(7, actions.size());

        // Now we take the hit
        forwardModel.next(gameState, new MovePiece(1, 5));
        assertEquals(0, gameState.getPiecesOnPoint(0, 5));
        assertEquals(1, gameState.getPiecesOnPoint(1, 5));
        assertEquals(1, gameState.getPiecesOnBar(0));
        assertEquals(1, gameState.blots[0]);
        assertEquals(0, gameState.blots[1]);
    }

    @Test
    public void mustMoveFromBarFirst() {
        // set a piece on the bar (technically this means we have 16 pieces)
        gameState.counters.get(0).add(new Token("White"));
        gameState.counters.get(0).get(0).setOwnerId(0);
        gameState.setDiceValues(new int[]{2, 3});
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 23)));
        assertTrue(availableActions.contains(new MovePiece(0, 22)));
        assertEquals(2, availableActions.size());

        gameState.movePiece(1, 12, 22);
        assertEquals(1, gameState.getPiecesOnPoint(1, 22));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertTrue(availableActions.contains(new MovePiece(0, 23)));
        assertTrue(availableActions.contains(new MovePiece(0, 22)));
        assertEquals(2, availableActions.size());

        gameState.movePiece(1, 12, 22);
        assertEquals(2, gameState.getPiecesOnPoint(1, 22));
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertFalse(availableActions.contains(new MovePiece(0, 22)));
        assertTrue(availableActions.contains(new MovePiece(0, 23)));
        assertEquals(1, availableActions.size());
    }

    @Test
    public void turnSkippedIfPiecesOnBarAndNoMovePossible() {
        gameState.counters.get(0).add(new Token("White"));
        gameState.counters.get(0).get(0).setOwnerId(0);
        gameState.counters.get(0).add(new Token("White"));
        gameState.counters.get(0).get(1).setOwnerId(0);
        gameState.setDiceValues(new int[]{2, 3});

        gameState.movePiece(1, 12, 22);
        gameState.movePiece(1, 12, 22);
        assertEquals(2, gameState.getPiecesOnPoint(1, 22));

        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertFalse(availableActions.contains(new MovePiece(0, 22)));
        assertTrue(availableActions.contains(new MovePiece(0, 23)));
        assertEquals(1, availableActions.size());

        forwardModel.next(gameState, availableActions.get(0));
        assertEquals(1, gameState.getPiecesOnPoint(0, 23));
        assertEquals(1, gameState.getPiecesOnBar(0));
        assertEquals(1, gameState.getCurrentPlayer());
        assertEquals(2, gameState.getAvailableDiceValues().length);
    }

    @Test
    public void mayOnlyBearOffOnceAllPiecesInHomeBoard() {
        // first we move all pieces to the homeboard of player 1
        for (int pos = 1; pos < 19; pos++) {
            for (int i = gameState.getPiecesOnPoint(1, pos); i > 0; i--)
                gameState.movePiece(1, pos, 21);  // a pretty random point in the home board
        }
        // take two actions for player 0
        gameState.setDiceValues(new int[]{5, 6});
        do {
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).get(0));
        } while (gameState.getCurrentPlayer() == 0);
        gameState.movePiece(1, 21, 5);

        gameState.setDiceValues(new int[]{5, 6});
        assertEquals(1, gameState.getCurrentPlayer());
        var availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(3, availableActions.size());
        assertTrue(availableActions.contains(new MovePiece(5, 10)));
        assertTrue(availableActions.contains(new MovePiece(5, 11)));
        assertTrue(availableActions.contains(new MovePiece(19, 24)));

        gameState.movePiece(1, 5, 21);
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(3, availableActions.size());
        assertTrue(availableActions.contains(new MovePiece(21, -1)));
        assertTrue(availableActions.contains(new MovePiece(19, -1)));
        assertTrue(availableActions.contains(new MovePiece(19, 24)));

        forwardModel.next(gameState, new MovePiece(21, -1));
        assertEquals(1, gameState.getPiecesBorneOff(1));
        assertEquals(1, gameState.getGameScore(1), 0.01);
        assertEquals(0, gameState.getGameScore(0), 0.01);
        assertEquals(6, gameState.getAvailableDiceValues()[0]);
        assertEquals(1, gameState.getAvailableDiceValues().length);
        availableActions = forwardModel.computeAvailableActions(gameState);
        assertEquals(1, availableActions.size());
        assertTrue(availableActions.contains(new MovePiece(19, -1)));
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


package players.search;

import core.AbstractPlayer;
import core.actions.AbstractAction;
import games.connect4.Connect4ForwardModel;
import games.connect4.Connect4GameParameters;
import games.connect4.Connect4GameState;
import org.junit.Test;
import players.PlayerConstants;
import players.search.MaxNSearchPlayer.SearchResult;

import static org.junit.Assert.*;

public class AlphaBetaPruningTests {

    Connect4ForwardModel forwardModel = new Connect4ForwardModel();

    @Test
    public void connect4AlphaBeta() {
        // the intention here is to run a game of Connect4 from start to finish and confirm that
        // two MaxNSearchPlayer with alphaBetaPruning set to true and false respectively will make the same moves
        // (or moves which have identical values with fixed depth search)
        // except that the player with pruning should take less time

        // create a game of Connect4
        Connect4GameState gameState = new Connect4GameState(new Connect4GameParameters(), 2);
        forwardModel.setup(gameState);

        // create a MaxNSearchPlayer with alphaBetaPruning set to false
        MaxNSearchParameters paramsOne = new MaxNSearchParameters();
        paramsOne.alphaBetaPruning = false;
        paramsOne.budget = Integer.MAX_VALUE;
        paramsOne.budgetType = PlayerConstants.BUDGET_TIME;
        paramsOne.paranoid = true;
        paramsOne.searchDepth = 4;
        MaxNSearchPlayer player1 = new MaxNSearchPlayer(paramsOne);
        player1.setForwardModel(forwardModel);

        // create a MaxNSearchPlayer with alphaBetaPruning set to true
        MaxNSearchParameters paramsTwo = new MaxNSearchParameters();
        paramsTwo.alphaBetaPruning = true;
        paramsTwo.budget = Integer.MAX_VALUE;
        paramsTwo.budgetType = PlayerConstants.BUDGET_TIME;
        paramsTwo.paranoid = true;
        paramsTwo.searchDepth = 4;
        MaxNSearchPlayer player2 = new MaxNSearchPlayer(paramsTwo);
        player2.setForwardModel(forwardModel);

        runGame(gameState, player1, player2, false, true);
    }


    @Test
    public void connect4IterativeDeepening() {
        // and with the base agent against iterative deepening

        // create a game of Connect4
        Connect4GameState gameState = new Connect4GameState(new Connect4GameParameters(), 2);
        forwardModel.setup(gameState);

        // create a MaxNSearchPlayer with iterativeDeepening set to true
        MaxNSearchParameters paramsOne = new MaxNSearchParameters();
        paramsOne.iterativeDeepening = true;
        paramsOne.budget = 1000;
        paramsOne.paranoid = false;
        paramsOne.searchDepth = 4;
        MaxNSearchPlayer player1 = new MaxNSearchPlayer(paramsOne);
        player1.setForwardModel(forwardModel);

        // create a MaxNSearchPlayer with iterativeDeepening set to false
        MaxNSearchParameters paramsTwo = new MaxNSearchParameters();
        paramsOne.iterativeDeepening = false;
        paramsTwo.budget = 1000;
        paramsTwo.paranoid = false;
        paramsTwo.searchDepth = 4;
        MaxNSearchPlayer player2 = new MaxNSearchPlayer(paramsTwo);
        player2.setForwardModel(forwardModel);

        runGame(gameState, player1, player2, true, false);
    }

    @Test
    public void connect4NonRandomExpansionOrder() {
        // and now with move expansion broken by value function (with alphaBetaPruning)

        // create a game of Connect4
        Connect4GameState gameState = new Connect4GameState(new Connect4GameParameters(), 2);
        forwardModel.setup(gameState);

        // create a MaxNSearchPlayer with estimated expansion set to false
        MaxNSearchParameters paramsOne = new MaxNSearchParameters();
        paramsOne.alphaBetaPruning = true;
        paramsOne.expandByEstimatedValue = false;
        paramsOne.budget = 1000;
        paramsOne.paranoid = true;
        paramsOne.searchDepth = 4;
        MaxNSearchPlayer player1 = new MaxNSearchPlayer(paramsOne);
        player1.setForwardModel(forwardModel);

        // create a MaxNSearchPlayer with estimated expansion set to true
        MaxNSearchParameters paramsTwo = new MaxNSearchParameters();
        paramsTwo.alphaBetaPruning = true;
        paramsTwo.budget = 1000;
        paramsTwo.expandByEstimatedValue = true;
        paramsTwo.paranoid = true;
        paramsTwo.searchDepth = 4;
        MaxNSearchPlayer player2 = new MaxNSearchPlayer(paramsTwo);
        player2.setForwardModel(forwardModel);

        runGame(gameState, player1, player2, false, true);
    }

    @Test
    public void connect4IterativeNonRandomExpansionOrder() {
        // iterative deepening with non-random expansion order

        // create a game of Connect4
        Connect4GameState gameState = new Connect4GameState(new Connect4GameParameters(), 2);
        forwardModel.setup(gameState);

        // create a MaxNSearchPlayer with iterativeDeepening set to false
        MaxNSearchParameters paramsOne = new MaxNSearchParameters();
        paramsOne.iterativeDeepening = false;
        paramsOne.alphaBetaPruning = true;
        paramsOne.budget = 1000;
        paramsOne.expandByEstimatedValue = false;
        paramsOne.paranoid = true;
        paramsOne.searchDepth = 4;
        MaxNSearchPlayer player1 = new MaxNSearchPlayer(paramsOne);
        player1.setForwardModel(forwardModel);

        // create a MaxNSearchPlayer with iterativeDeepening set to true
        MaxNSearchParameters paramsTwo = new MaxNSearchParameters();
        paramsTwo.iterativeDeepening = true;
        paramsTwo.alphaBetaPruning = true;
        paramsTwo.budget = 1000;
        paramsTwo.expandByEstimatedValue = true;
        paramsTwo.paranoid = true;
        paramsTwo.searchDepth = 4;
        MaxNSearchPlayer player2 = new MaxNSearchPlayer(paramsTwo);
        player2.setForwardModel(forwardModel);

        runGame(gameState, player1, player2, false, true);
    }


    // should be called so that the expected faster agent is player2
    private void runGame(Connect4GameState gameState, MaxNSearchPlayer player1, MaxNSearchPlayer player2,
                         boolean checkIdenticalMoves, boolean checkPlayerOneSlower) {

        long playerOneTime = 0;
        long playerTwoTime = 0;
        int identicalActions = 0;
        int totalActions = 0;
        do {
            // player 1's turn
            long start = System.currentTimeMillis();
            AbstractAction actionOne = player1.getAction(gameState, forwardModel.computeAvailableActions(gameState));
            long timeOne = System.currentTimeMillis() - start;
            AbstractAction actionTwo = player2.getAction(gameState, forwardModel.computeAvailableActions(gameState));
            long timeTwo = System.currentTimeMillis() - start - timeOne;

            totalActions++;
            if (actionOne.equals(actionTwo))
                identicalActions++;
            else {
                assertArrayEquals(player1.getRootResult().value(), player2.getRootResult().value(), 0.000001);
                // we also check that the action chosen by Player2 (the faster one) has the same value for Player 1 (even if not chosen)
                SearchResult player1Result = player1.getRootResult();
                SearchResult player2Result = player2.getRootResult();
                double[] p1ValueForP2Action = player1Result.allActionValues().get(actionTwo);
                assertArrayEquals(p1ValueForP2Action, player2Result.value(), 0.000001);
            }

            forwardModel.next(gameState, actionOne);

            if (gameState.getGameTick() > 4) {
                // skip the first few moves for JVM warmup
                playerOneTime += timeOne;
                playerTwoTime += timeTwo;
            }
        } while (gameState.isNotTerminal());

        System.out.println("Player 1 took " + playerOneTime + "ms, Player 2 took " + playerTwoTime + "ms");
        System.out.println("Identical actions : " + identicalActions + " / " + totalActions);
        if (checkPlayerOneSlower)
            assertTrue(playerOneTime > playerTwoTime);
        if (checkIdenticalMoves)
            assertEquals(totalActions, identicalActions);
        else
            assertNotEquals(totalActions, identicalActions);
    }

}

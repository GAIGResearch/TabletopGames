package test.players.mcts;

import core.*;
import core.actions.*;
import core.interfaces.IStatisticLogger;
import games.dominion.DominionGame;
import games.dominion.DominionParameters;
import org.junit.*;
import players.PlayerConstants;
import players.mcts.*;
import players.simple.RandomPlayer;
import utilities.SummaryLogger;

import java.util.*;

import static org.junit.Assert.*;

public class MCTSNodesAndVisitsTests {

    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 20;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.K = 1.0;
    }

    public Game createGame(MCTSParams params) {
        mctsPlayer = new TestMCTSPlayer(params);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));
        return new DominionGame(players, DominionParameters.firstGame(330245));
    }

    @Test
    public void selfOnly() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        Game game = createGame(params);
        int[] expectedNodes = {200, 200, 200, 200};
        int[] errorMargin = {10, 10, 10, 10};
        runGame(game, 4, expectedNodes, errorMargin);
    }

    @Test
    public void paranoid() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.Paranoid;
        Game game = createGame(params);
        int[] expectedNodes = {200, 200, 200, 200};
        int[] errorMargin = {10, 10, 10, 10};
        runGame(game, 4, expectedNodes, errorMargin);
    }

    @Test
    public void maxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params);
        int[] expectedNodes = {200, 200, 200, 200};
        int[] errorMargin = {10, 10, 10, 10};
        runGame(game, 4, expectedNodes, errorMargin);    }

    @Test
    public void RegretMatching() {
        params.treePolicy = MCTSEnums.TreePolicy.RegretMatching;
        Game game = createGame(params);
        int[] expectedNodes = {200, 200, 200, 200};
        int[] errorMargin = {10, 10, 10, 10};
        runGame(game, 4, expectedNodes, errorMargin);
    }

    @Test
    public void EXP3() {
        params.treePolicy = MCTSEnums.TreePolicy.EXP3;
        Game game = createGame(params);
        int[] expectedNodes = {200, 200, 200, 200};
        int[] errorMargin = {10, 10, 10, 10};
        runGame(game, 4, expectedNodes, errorMargin);    }

    @Test
    public void reducedDepth3MaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        params.maxTreeDepth = 3;
        params.information = MCTSEnums.Information.Closed_Loop;
        Game game = createGame(params);
        runGame(game, 4, new int[0], new int[0]);
    }

    @Test
    public void reducedDepth3() {
        params.maxTreeDepth = 3;
        params.information = MCTSEnums.Information.Closed_Loop;
        Game game = createGame(params);
        runGame(game, 4, new int[0], new int[0]);
    }

    private void runGame(Game game, int moves, int[] expectedNodes, int[] errorMargin) {
        int counter = 0;
        AbstractGameState state = game.getGameState();
        AbstractForwardModel forwardModel = game.getForwardModel();
        do {
            IStatisticLogger logger = new SummaryLogger();
            mctsPlayer.setStatsLogger(logger);

            AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                    .getAction(state, forwardModel.computeAvailableActions(state));

            if (state.getCurrentPlayer() == 0) {
                logger.processDataAndFinish();
                TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
                assertEquals(200, mctsPlayer.getRoot(0).getVisits());
                if (params.maxTreeDepth == 3)
                    assertEquals(3, stats.depthReached);
                else {
                    System.out.printf("Move %d has %d versus %d expected nodes", counter, stats.totalNodes, expectedNodes[counter]);
                    assertEquals(expectedNodes[counter], stats.totalNodes, errorMargin[counter]);
                }
                counter++;
            }
            forwardModel.next(state, actionChosen);
        } while (counter < moves);
    }


}

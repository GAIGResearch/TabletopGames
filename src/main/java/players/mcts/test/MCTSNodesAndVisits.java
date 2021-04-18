package players.mcts.test;

import core.*;
import core.actions.*;
import core.interfaces.IStatisticLogger;
import games.loveletter.*;
import org.junit.*;
import players.PlayerConstants;
import players.mcts.*;
import players.simple.RandomPlayer;
import utilities.SummaryLogger;

import java.util.*;

import static org.junit.Assert.*;

public class MCTSNodesAndVisits {

    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        params.redeterminise = true;
        params.openLoop = true;
        params.maxTreeDepth = 10;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.iterationsBudget = 200;
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
        return new LoveLetterGame(players, new LoveLetterParameters(330245));
    }

    @Test
    public void selfOnly() {
        Game game = createGame(params);
        int[] expectedNodes = {201, 201, 201, 201};
        runGame(game, 4, expectedNodes);
    }

    @Test
    public void paranoid() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.Paranoid;
        Game game = createGame(params);
        int[] expectedNodes = {201, 201, 201, 201};
        runGame(game, 4, expectedNodes);
    }

    @Test
    public void maxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params);
        int[] expectedNodes = {201, 201, 201, 201};
        runGame(game, 4, expectedNodes);
    }

    @Test
    public void RegretMatching() {
        params.treePolicy = MCTSEnums.TreePolicy.RegretMatching;
        Game game = createGame(params);
        int[] expectedNodes = {201, 201, 201, 201};
        runGame(game, 4, expectedNodes);
    }

    @Test
    public void EXP3() {
        params.treePolicy = MCTSEnums.TreePolicy.EXP3;
        Game game = createGame(params);
        int[] expectedNodes = {201, 201, 201, 201};
        runGame(game, 4, expectedNodes);
    }

    @Test
    public void reducedDepth3MaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        params.maxTreeDepth = 3;
        params.redeterminise = false;
        params.openLoop = false;
        Game game = createGame(params);
        runGame(game, 4, new int[0]);
    }

    @Test
    public void reducedDepth3() {
        params.maxTreeDepth = 3;
        params.redeterminise = false;
        params.openLoop = false;
        Game game = createGame(params);
        runGame(game, 4, new int[0]);
    }

    private void runGame(Game game, int moves, int[] expectedNodes) {
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
                TreeStatistics stats = new TreeStatistics(mctsPlayer.root);
                assertEquals(200, mctsPlayer.root.getVisits());
                if (params.maxTreeDepth == 3)
                    assertEquals(3, stats.depthReached);
                else {
                    assertEquals(expectedNodes[counter], stats.totalNodes);
                }
                counter++;
            }
            forwardModel.next(state, actionChosen);
        } while (counter < moves);
    }


}

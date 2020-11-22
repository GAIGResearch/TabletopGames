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

public class MCTSLoveLetterTest {

    LoveLetterGame game;
    TestMCTSPlayer mctsPlayer;

    @Before
    public void setup() {
        List<AbstractPlayer> players = new ArrayList<>();
        MCTSParams params = new MCTSParams(9332);
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
        mctsPlayer = new TestMCTSPlayer(params);
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));
        game = new LoveLetterGame(players, new LoveLetterParameters(330245));
    }

    @Test
    public void test1() {

        AbstractGameState state = game.getGameState();
        mctsPlayer.setDebug(true);
        AbstractForwardModel forwardModel = game.getForwardModel();

        for (int move = 0; move < 12; move++) {
            IStatisticLogger logger = new SummaryLogger();
            mctsPlayer.setStatsLogger(logger);

            AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                    .getAction(state, forwardModel.computeAvailableActions(state));

            if (state.getCurrentPlayer() == 0) {
                logger.processDataAndFinish();
                TreeStatistics stats = new TreeStatistics(mctsPlayer.root);
                assertEquals(200, mctsPlayer.root.getVisits());
                assertEquals(201, stats.totalNodes);
            }
            forwardModel.next(state, actionChosen);
        }
    }


}

package players.mcts.test;

import core.*;
import core.actions.*;
import core.interfaces.IStatisticLogger;
import games.GameType;
import games.loveletter.*;
import games.virus.*;
import org.junit.*;
import players.PlayerConstants;
import players.mcts.*;
import players.simple.RandomPlayer;
import utilities.SummaryLogger;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static org.junit.Assert.*;


public class RewardsForParanoia {

    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.redeterminise = true;
        params.openLoop = true;
        params.maxTreeDepth = 10;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.iterationsBudget = 500;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.K = 1.0;
    }

    public Game createGame(MCTSParams params, GameType gameType) {
        mctsPlayer = new TestMCTSPlayer(params);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));
        Game game = gameType.createGameInstance(3, 3302345);
        game.reset(players);
        return game;
    }

    private void runGame(Game game, int moves, Predicate<SingleTreeNode> allMatch, Predicate<SingleTreeNode> anyMatch) {
        int counter = 0;
        boolean anyMatchTriggered = false;
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
                List<SingleTreeNode> allNodes = mctsPlayer.allNodesInTree();
                assertTrue(allNodes.stream().allMatch(allMatch));
                 if (allNodes.stream().anyMatch(anyMatch))
                     anyMatchTriggered = true;
                counter++;
            }
            forwardModel.next(state, actionChosen);
        } while (counter < moves);
        assertTrue(anyMatchTriggered);
    }

    @Test
    public void loveLetterSelfOnly() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        Game game = createGame(params, GameType.LoveLetter);
        Predicate<SingleTreeNode> allMatch = node -> {
            node.getChildren().values().forEach( nodeArray ->
                    assertTrue(nodeArray == null || (nodeArray[1] == null && nodeArray[2] == null)));
            return true;
        };
        runGame(game, 4, allMatch, n -> true);
    }

    @Test
    public void loveLetterParanoid() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.Paranoid;
        Game game = createGame(params, GameType.LoveLetter);
        Predicate<SingleTreeNode> allMatch = node -> {
            assertEquals(node.getTotValue()[0], -node.getTotValue()[1], 0.001);
            assertEquals(node.getTotValue()[0], -node.getTotValue()[2], 0.001);
            return true;
        };
        Predicate<SingleTreeNode> anyMatch = node -> node.getChildren().values().stream()
                .filter(Objects::nonNull)
                .anyMatch(array -> Arrays.stream(array).filter(Objects::nonNull).count() > 1);
        runGame(game, 4, allMatch, anyMatch);
    }
    @Test
    public void loveLetterMaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params, GameType.LoveLetter);
        Predicate<SingleTreeNode> allMatch = node -> {
            // if we have just one, everyone else must have just lost
            if (node.getVisits() == 1 && node.getTotValue()[0] == 1.0) {
                assertEquals(node.getTotValue()[0], -node.getTotValue()[1], 0.001);
                assertEquals(node.getTotValue()[0], -node.getTotValue()[2], 0.001);
            }
            return true;
        };
        Predicate<SingleTreeNode> anyMatch = node -> node.getChildren().values().stream()
                .filter(Objects::nonNull)
                .anyMatch(array -> Arrays.stream(array).filter(Objects::nonNull).count() > 1);
        runGame(game, 4, allMatch, anyMatch);
    }

    @Test
    public void virusSelfOnly() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        Game game = createGame(params, GameType.Virus);
        Predicate<SingleTreeNode> allMatch = node -> {
            node.getChildren().values().forEach( nodeArray ->
                    assertTrue(nodeArray == null || (nodeArray[1] == null && nodeArray[2] == null)));
            return true;
        };
        runGame(game, 4, allMatch, n -> true);
    }

    @Test
    public void virusParanoid() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.Paranoid;
        Game game = createGame(params, GameType.Virus);
        Predicate<SingleTreeNode> allMatch = node -> {
            assertEquals(node.getTotValue()[0], -node.getTotValue()[1], 0.001);
            assertEquals(node.getTotValue()[0], -node.getTotValue()[2], 0.001);
            return true;
        };
        runGame(game, 4, allMatch, n -> true);
    }
    @Test
    public void virusMaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params, GameType.Virus);
        Predicate<SingleTreeNode> allMatch = node -> {
            // if we have just one, everyone else must have just lost
            if (node.getVisits() == 1 && node.getTotValue()[0] == 1.0) {
                assertEquals(node.getTotValue()[0], -node.getTotValue()[1], 0.001);
                assertEquals(node.getTotValue()[0], -node.getTotValue()[2], 0.001);
            }
            return true;
        };
        runGame(game, 4, allMatch, n -> true);
    }

}

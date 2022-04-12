package test.players.mcts;

import core.*;
import core.actions.*;
import core.interfaces.IStatisticLogger;
import games.GameType;
import org.junit.*;
import players.PlayerConstants;
import players.mcts.*;
import players.simple.RandomPlayer;
import utilities.SummaryLogger;
import utilities.Utils;

import java.util.*;
import java.util.function.*;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;


public class RewardsForParanoiaTests {

    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 10;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 1000;
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

    private void runGame(Game game, int moves,
                         Predicate<SingleTreeNode> allMatch,
                         Predicate<SingleTreeNode> anyMatch,
                         Predicate<List<SingleTreeNode>> aggregateCheck) {
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
                TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
                List<SingleTreeNode> allNodes = mctsPlayer.getRoot(0).allNodesInTree();
                List<SingleTreeNode> problemNodes = allNodes.stream().filter(n -> !allMatch.test(n)).collect(toList());
                assertEquals(0, problemNodes.size());
                if (allNodes.stream().anyMatch(anyMatch))
                    anyMatchTriggered = true;
                assertTrue(aggregateCheck.test(allNodes));
                counter++;
            }
            forwardModel.next(state, actionChosen);
        } while (counter < moves);
        assertTrue(anyMatchTriggered);
    }

    Predicate<List<SingleTreeNode>> checkNodesDistributedAcrossAllPlayers = list -> {
        Map<Integer, Long> nodeCountByDecisionMaker = list.stream()
                .map(SingleTreeNode::getActor)
                .collect(groupingBy(Function.identity(), counting()));
        // we then check that all players have som nodes in the tree
        Utils.GameResult[] results = mctsPlayer.getRoot(0).getState().getPlayerResults();
        System.out.println("Nodes by player: " + nodeCountByDecisionMaker);
        if (results[0] == Utils.GameResult.GAME_ONGOING)
            assertTrue(nodeCountByDecisionMaker.get(0) > 10);
        if (results[1] == Utils.GameResult.GAME_ONGOING)
            assertTrue(nodeCountByDecisionMaker.get(1) > 10);
        if (results[2] == Utils.GameResult.GAME_ONGOING)
            assertTrue(nodeCountByDecisionMaker.get(2) > 10);
        return true;
    };
    Predicate<SingleTreeNode> paranoidNodeValues = node -> {
        assertEquals(node.getTotValue()[0], -node.getTotValue()[1], 0.001);
        assertEquals(node.getTotValue()[0], -node.getTotValue()[2], 0.001);
        assertTrue(node.getChildren().size() < 60);
        return true;
    };
    Predicate<SingleTreeNode> maxNNodeValues = node -> {
        // if we have just one, everyone else must have just lost
        if (node.getVisits() == 1 && node.getTotValue()[0] == 1.0) {
            assertEquals(node.getTotValue()[0], -node.getTotValue()[1], 0.001);
            assertEquals(node.getTotValue()[0], -node.getTotValue()[2], 0.001);
        }
        assertTrue(node.getChildren().size() < 60);
        return true;
    };
    Predicate<SingleTreeNode> selfOnlyNodes = node -> {
        boolean passed = node.getChildren().values().stream().allMatch(nodeArray ->
                nodeArray == null || (nodeArray[1] == null && nodeArray[2] == null));
        passed = passed && node.getChildren().size() < 60;
        return passed;
    };
    Predicate<SingleTreeNode> atLeastOneSplitNode = node -> node.getChildren().values().stream()
            .filter(Objects::nonNull)
            .anyMatch(array -> Arrays.stream(array).filter(Objects::nonNull).count() > 1);

    @Test
    public void loveLetterSelfOnly() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        Game game = createGame(params, GameType.LoveLetter);
        runGame(game, 4, selfOnlyNodes, n -> true, n -> true);
    }

    @Test
    public void loveLetterParanoid() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.Paranoid;
        Game game = createGame(params, GameType.LoveLetter);
        runGame(game, 4, paranoidNodeValues, atLeastOneSplitNode, checkNodesDistributedAcrossAllPlayers);
    }

    @Test
    public void loveLetterMaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params, GameType.LoveLetter);
        runGame(game, 4, maxNNodeValues, atLeastOneSplitNode, checkNodesDistributedAcrossAllPlayers);
    }

    @Test
    public void virusSelfOnly() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        Game game = createGame(params, GameType.Virus);
        runGame(game, 4, selfOnlyNodes, n -> true, n -> true);
    }

    @Test
    public void virusParanoid() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.Paranoid;
        Game game = createGame(params, GameType.Virus);
        runGame(game, 4, paranoidNodeValues, n -> true, checkNodesDistributedAcrossAllPlayers);
    }

    @Test
    public void virusMaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params, GameType.Virus);
        runGame(game, 4, maxNNodeValues, n -> true, checkNodesDistributedAcrossAllPlayers);
    }

    @Test
    public void coltExpressMaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params, GameType.ColtExpress);
        runGame(game, 4, maxNNodeValues, n -> true, checkNodesDistributedAcrossAllPlayers);
    }

    @Test
    public void explodingKittensMaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params, GameType.ExplodingKittens);
        runGame(game, 4, maxNNodeValues, n -> true, checkNodesDistributedAcrossAllPlayers);
    }

    @Test
    public void unoMaxN() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MaxN;
        Game game = createGame(params, GameType.Uno);
        runGame(game, 4, maxNNodeValues, n -> true, checkNodesDistributedAcrossAllPlayers);
    }

}

package players.mcts;

import core.AbstractPlayer;
import core.Game;
import evaluation.features.StateKeyFromFeatureVector;
import evaluation.features.TurnAndPlayerOnly;
import games.GameType;
import games.dotsboxes.*;
import games.loveletter.LoveLetterParameters;
import games.loveletter.features.LLHandCards;
import games.loveletter.features.LLStateFeaturesReduced;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MCGSTests {

    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    private final Predicate<SingleTreeNode> actionVisitsAddUp = node ->
            node.getVisits() == node.actionValues.values().stream().mapToInt(s -> s.nVisits).sum();

    private final Predicate<SingleTreeNode> allNodesForPlayerZero = node ->
            node.decisionPlayer == 0 && node.state.getCurrentPlayer() == 0;

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams();
        params.setRandomSeed(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 200;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.maintainMasterState = true;
        params.K = 1.0;
    }


    public Game createDotsAndBoxes(MCTSParams params) {
        mctsPlayer = new TestMCTSPlayer(params, null);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));
        players.add(new RandomPlayer(new Random(-3092345)));
        Game game = GameType.DotsAndBoxes.createGameInstance(players.size());
        game.reset(players);
        return game;
    }

    public Game createLoveLetter(MCTSParams params) {
        mctsPlayer = new TestMCTSPlayer(params, null);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(3024)));
        LoveLetterParameters gameParams = new LoveLetterParameters();
        gameParams.setRandomSeed(3812);
        Game game = GameType.LoveLetter.createGameInstance(players.size(), gameParams);
        game.reset(players);
        return game;
    }

    @Test
    public void SingleFileDotsAndBoxes() {
        // The aim here is to have a minimal (and silly) feature that consists only of the player ID and the Round
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
        params.MCGSStateKey = new StateKeyFromFeatureVector(new TurnAndPlayerOnly());

        Game game = createDotsAndBoxes(params);
        // We now tun one turn of the game
        game.oneAction();
        // We now check that the MCGS tree has the expected number of nodes
        // we just check this is all the way to the end of the game (about 60 ish)
        // and not the 4 we get if we just expand one untried action per iteration
        MCGSNode root = (MCGSNode) mctsPlayer.getRoot(0);
        assertEquals(200, root.getVisits());
        assertEquals(60, root.getTranspositionMap().size(), 10);

        for (int i = 0; i < 51; i++) {
            game.oneAction();
        }
        // We should now have 31 actions at the root
        root = (MCGSNode) mctsPlayer.getRoot(0);
        // We now have fewer nodes, because the game is closer to the end
        assertEquals(20, root.getTranspositionMap().size(), 10);
    }

    @Test
    public void OneIterationHasDepthOne() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
        params.MCGSStateKey = (s, i) -> String.valueOf(s.hashCode());
        params.budget = 1;
        Game game = createDotsAndBoxes(params);
        do {
            int p = game.getGameState().getCurrentPlayer();
            game.oneAction();
            if (p == 0 && game.getTick() < 50) {
                TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
                assertEquals(1, stats.depthReached);
                assertEquals(2, stats.totalNodes);
            }
        } while (game.getGameState().isNotTerminal());
    }

    @Test
    public void OneHundredIterationsHasMaxDepth2() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
        params.MCGSStateKey = new DBEdgeAndScoreKey();
        params.budget = 100;
        Game game = createDotsAndBoxes(params);
        do {
            int p = game.getGameState().getCurrentPlayer();
            game.oneAction();
            if (p == 0 && game.getTick() < 9) {
                TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
                assertEquals(2, stats.depthReached);
                assertEquals(101, stats.totalNodes);
            }
        } while (game.getGameState().isNotTerminal());
    }


    @Test
    public void OneIterationHasDepthOneForMCTS() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        params.budget = 1;
        Game game = createDotsAndBoxes(params);
        do {
            int p = game.getGameState().getCurrentPlayer();
            System.out.println(game.getTick());
            game.oneAction();
            if (p == 0 && game.getTick() < 50) {
                TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
                assertEquals(1, stats.depthReached);
                assertEquals(2, stats.totalNodes);
            }
        } while (game.getGameState().isNotTerminal());
    }

    @Test
    public void DotsAndBoxesFullRunActionVisits() {
        // In this case we run through a whole game, relying on the predicate test
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
        params.MCGSStateKey = new StateKeyFromFeatureVector(new DBStateFeaturesReduced());
        params.budget = 1000;
        Game game = createDotsAndBoxes(params);
        do {
            int p = game.getGameState().getCurrentPlayer();
            game.oneAction();
            if (p == 0) {
                MCGSNode root = (MCGSNode) mctsPlayer.getRoot(0);
                if (root == null) continue;
                if (game.getTick() < 30) // at this point we are at no risk of the game ending during search
                    assertEquals(root.getVisits(), root.getTranspositionMap().size(), 1);
                assertTrue(params.budget + 1 >= root.getTranspositionMap().size());
                List<SingleTreeNode> problemNodes = root.nonMatchingNodes(actionVisitsAddUp);
                assertEquals(0, problemNodes.size());
                assertEquals(0, problemNodes.size());
            }
        } while (game.getGameState().isNotTerminal());
    }

    @Test
    public void DotsAndBoxesFullRunActionVisitsSelfOnly() {
        // In this case we run through a whole game, relying on the predicate test
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGSSelfOnly;
        params.MCGSStateKey = new StateKeyFromFeatureVector(new DBStateFeatures());
        params.budget = 1000;
        Game game = createDotsAndBoxes(params);
        do {
            int p = game.getGameState().getCurrentPlayer();
            game.oneAction();
            if (p == 0) {
                MCGSNode root = (MCGSNode) mctsPlayer.getRoot(0);
                if (root == null) continue;
                if (game.getTick() < 10) // at this point we are at no risk of the game ending during search
                    assertEquals(root.getVisits(), root.getTranspositionMap().size(), 1);
                assertTrue(params.budget + 1 >= root.getTranspositionMap().size());
                assertEquals(0, root.getTranspositionMap().keySet().stream().filter(s -> !((String)s).startsWith("0-")).count());
                List<SingleTreeNode> problemNodes = root.nonMatchingNodes(actionVisitsAddUp);
                assertEquals(0, problemNodes.size());
                problemNodes = root.nonMatchingNodes(allNodesForPlayerZero);
                assertEquals(0, problemNodes.size());
            }
        } while (game.getGameState().isNotTerminal());
    }

    @Test
    public void LoveLetterFullRunActionVisitsSelfOnly() {
        // In this case we run through a whole game, relying on the predicate test
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGSSelfOnly;
        params.MCGSStateKey = new StateKeyFromFeatureVector(new LLStateFeaturesReduced());
        params.budget = 1000;
        Game game = createLoveLetter(params);
        do {
            int p = game.getGameState().getCurrentPlayer();
            game.oneAction();
            if (p == 0) {
                MCGSNode root = (MCGSNode) mctsPlayer.getRoot(0);
                if (root == null) continue;
                assertTrue(params.budget + 1 >= root.getTranspositionMap().size());
                assertEquals(0, root.getTranspositionMap().keySet().stream().filter(s -> !((String)s).startsWith("0-")).count());
                //                        root.getTranspositionMap().get(s).openLoopState.isNotTerminalForPlayer(0)).count());
                List<SingleTreeNode> problemNodes = root.nonMatchingNodes(actionVisitsAddUp);
                assertEquals(0, problemNodes.size());
                problemNodes = root.nonMatchingNodes(allNodesForPlayerZero);
                assertEquals(0, problemNodes.size());
            }
        } while (game.getGameState().isNotTerminal());
    }


    @Test
    public void LoveLetterFullRunActionVisits() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
        params.MCGSStateKey = new StateKeyFromFeatureVector(new LLStateFeaturesReduced());
        params.budget = 1000;
        Game game = createLoveLetter(params);
        do {
            int p = game.getGameState().getCurrentPlayer();
            game.oneAction();
            if (p == 0) {
                MCGSNode root = (MCGSNode) mctsPlayer.getRoot(0);
                if (root == null) continue;
                if (game.getTick() < 10) // at this point we are at no risk of the game ending during search
                    assertEquals(params.budget + 1, root.getTranspositionMap().size(), 1);
                assertTrue(params.budget + 1 >= root.getTranspositionMap().size());
                List<SingleTreeNode> problemNodes = root.nonMatchingNodes(actionVisitsAddUp);
                assertEquals(0, problemNodes.size());
            }
        } while (game.getGameState().isNotTerminal());
    }

    @Test
    public void LoveLetterHandCardsOnlyTest() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGSSelfOnly;
        params.MCGSStateKey = new StateKeyFromFeatureVector(new LLHandCards());
        params.budget = 2000;

        Game game = createLoveLetter(params);
        // We now have a total space of 7 + 6 + 5 + 5 + 4 + 3 + 2 + 1 = 33 states
        game.oneAction();
        MCGSNode root = (MCGSNode) mctsPlayer.getRoot(0);
        assertEquals(0, root.getTranspositionMap().keySet().stream().filter(s -> !((String)s).startsWith("0-")).count());
        assertEquals(33, root.getTranspositionMap().size());
    }
}

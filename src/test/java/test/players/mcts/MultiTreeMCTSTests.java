package test.players.mcts;


import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Token;
import games.loveletter.LoveLetterForwardModel;
import games.loveletter.LoveLetterGame;
import games.loveletter.LoveLetterGameState;
import games.loveletter.LoveLetterParameters;
import games.tictactoe.*;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.SingleTreeNode;
import players.mcts.TreeStatistics;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class MultiTreeMCTSTests {
    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    Token x = TicTacToeConstants.playerMapping.get(0);
    Token o = TicTacToeConstants.playerMapping.get(1);

    AbstractForwardModel fm = new TicTacToeForwardModel();
    private Predicate<SingleTreeNode> childrenVisitsAddUp = node ->
            node.getChildren().isEmpty() ||  // first condition is that this is a terminal node
                    node.getParent() == null || // the root node is different (and is checked above)
                    node.getVisits() == 1 + node.getChildren().values().stream().mapToInt(arr -> {
                                int retValue = 0;
                                if (arr != null)
                                    for (SingleTreeNode singleTreeNode : arr) {
                                        if (singleTreeNode != null)
                                            retValue += singleTreeNode.getVisits();
                                    }
                                return retValue;
                            }
                    ).sum();

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
        params.nodesStoreScoreDelta = false;
        params.K = 1.0;
    }

    public Game createTicTacToe(MCTSParams params, int gridSize) {
        mctsPlayer = new TestMCTSPlayer(params);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        TicTacToeGameParameters gameParams = new TicTacToeGameParameters(3812);
        gameParams.gridSize = gridSize;
        return new TicTacToeGame(players, gameParams);
    }

    public Game createLoveLetter(MCTSParams params) {
        mctsPlayer = new TestMCTSPlayer(params);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(3024)));
        LoveLetterParameters gameParams = new LoveLetterParameters(3812);
        return new LoveLetterGame(players, gameParams);
    }

    @Test
    public void nodeCreatedIfGameOverMaxN() {
        Game ttt = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) ttt.getGameState();
        int board = state.getGridBoard().getComponentID();

        fm.next(state, new SetGridValueAction<>(board, 1, 0, x));
        fm.next(state, new SetGridValueAction<>(board, 0, 0, o));
        fm.next(state, new SetGridValueAction<>(board, 0, 2, x));
        fm.next(state, new SetGridValueAction<>(board, 1, 1, o));

        // o (p1) is now set to win on their next turn

        AbstractAction action = mctsPlayer.getAction(state, fm.computeAvailableActions(state));
        assertEquals(new SetGridValueAction<>(board, 2, 2, x), action);
        // this is the only logical move to prevent the o player winning on their turn

        SingleTreeNode root = mctsPlayer.getRoot(0);
        assertEquals(200, root.getVisits());

        // the invariant then to check is that for each node in the tree, the number of visits is equal to the number of child visits + 1
        List<SingleTreeNode> problemNodes = root.nonMatchingNodes(childrenVisitsAddUp);
        assertEquals(0, problemNodes.size());

        // TicTacToe is easy as we strictly alternate turns
        problemNodes = root.nonMatchingNodes(node -> node.getChildren().isEmpty() || node.getActor() == node.getDepth() % 2);
        assertEquals(0, problemNodes.size());
    }

    @Test
    public void nodeCreatedIfGameOverMultiTree() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        Game ttt = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) ttt.getGameState();
        int board = state.getGridBoard().getComponentID();

        fm.next(state, new SetGridValueAction<>(board, 1, 0, x));
        fm.next(state, new SetGridValueAction<>(board, 0, 0, o));
        fm.next(state, new SetGridValueAction<>(board, 0, 2, x));
        fm.next(state, new SetGridValueAction<>(board, 1, 1, o));

        // o (p1) is now set to win on their next turn

        AbstractAction action = mctsPlayer.getAction(state, fm.computeAvailableActions(state));
        assertEquals(new SetGridValueAction<>(board, 2, 2, x), action);
        // this is the only logical move to prevent the o player winning on their turn

        SingleTreeNode root = mctsPlayer.getRoot(0);
        assertEquals(200, root.getVisits());
        assertEquals(200, mctsPlayer.getRoot(1).getVisits());

        // Now each tree should only have nodes for its player
        List<SingleTreeNode> problemNodes = root.nonMatchingNodes(node -> node.getActor() == 0);
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node -> node.getActor() == 1);
        assertEquals(0, problemNodes.size());

        // And each node should lead to the same player's next decision
        problemNodes = root.nonMatchingNodes(node -> node.getChildren().values().stream().allMatch(arr -> {
                    if (arr == null) return true;
                    return arr[1] == null;
                }
        ));
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node -> node.getChildren().values().stream().allMatch(arr -> {
                    if (arr == null) return true;
                    return arr[0] == null;
                }
        ));
        assertEquals(0, problemNodes.size());
    }

    @Test
    public void multiTreeTest() {
        params.budget = 200;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        Game game = createTicTacToe(params, 4);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
        System.out.println(stats);
        System.out.println(mctsPlayer.getRoot(0));
        assertEquals(params.budget, mctsPlayer.getRoot(0).getVisits());
        assertEquals(params.budget + 1, stats.totalNodes);
        assertEquals(16, mctsPlayer.getRoot(0).getChildren().size());

        stats = new TreeStatistics(mctsPlayer.getRoot(1));
        System.out.println(stats);
        System.out.println(mctsPlayer.getRoot(1));
        assertEquals(params.budget, mctsPlayer.getRoot(1).getVisits());
        assertEquals(params.budget + 1, stats.totalNodes);
        assertEquals(16, mctsPlayer.getRoot(1).getChildren().size());

        // the invariant then to check is that for each node in the tree, the number of visits is equal to the number of child visits + 1
        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(childrenVisitsAddUp);
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(childrenVisitsAddUp);
        assertEquals(0, problemNodes.size());

        // Now each tree should only have nodes for its player
        problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node.getActor() == 0);
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node -> node.getActor() == 1);
        assertEquals(0, problemNodes.size());

        // And each node should lead to the same player's next decision
        problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node.getChildren().values().stream().allMatch(arr -> {
                    if (arr == null) return true;
                    return arr[1] == null;
                }
        ));
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node -> node.getChildren().values().stream().allMatch(arr -> {
                    if (arr == null) return true;
                    return arr[0] == null;
                }
        ));
        assertEquals(0, problemNodes.size());
    }

    @Test
    public void multiTreeTestLoveLetter() {
        params.budget = 2000;
        params.rolloutLength = 20;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        // need to call this to set up root
        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
        System.out.println(stats);
        System.out.println(mctsPlayer.getRoot(0));

        stats = new TreeStatistics(mctsPlayer.getRoot(1));
        System.out.println(stats);
        System.out.println(mctsPlayer.getRoot(1));

        // the invariant then to check is that for each node in the tree, the number of visits is equal to the number of child visits + 1
        // we allow some nodes to break this with 2000 iterations - due to simulation over conditions while still descending tree. The point is it should be small
        // or rollout finishing before we get back?
        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(childrenVisitsAddUp);
        System.out.println("Problem nodes for player 0 : " + problemNodes.size());
        assertEquals(0, problemNodes.size(), 20);
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(childrenVisitsAddUp);
        System.out.println("Problem nodes for player 1 : " + problemNodes.size());
        assertEquals(0, problemNodes.size(), 40);
        problemNodes = mctsPlayer.getRoot(2).nonMatchingNodes(childrenVisitsAddUp);
        System.out.println("Problem nodes for player 2 : " + problemNodes.size());
        assertEquals(0, problemNodes.size(), 80);

        // Now each tree should only have nodes for its player
        problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node.getActor() == 0 || node.getVisits() == 1);
        if (!problemNodes.isEmpty())
            System.out.println(problemNodes.get(0));
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node -> node.getActor() == 1 || node.getVisits() == 1);
        if (!problemNodes.isEmpty())
            System.out.println(problemNodes.get(0));
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(2).nonMatchingNodes(node -> node.getActor() == 2 || node.getVisits() == 1);
        if (!problemNodes.isEmpty())
            System.out.println(problemNodes.get(0));
        assertEquals(0, problemNodes.size());

        // And each node should lead to the same player's next decision
        problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node.getChildren().values().stream().allMatch(arr -> {
                    if (arr == null) return true;
                    return arr[1] == null && arr[2] == null;
                }
        ));
        assertEquals(0, problemNodes.size());
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node -> node.getChildren().values().stream().allMatch(arr -> {
                    if (arr == null) return true;
                    return arr[0] == null && arr[2] == null;
                }
        ));
        assertEquals(0, problemNodes.size());
    }


    @Test
    public void multiTreeTestLoveLetterRewards() {
        params.budget = 1000;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();
        state.addAffectionToken(1);
        state.addAffectionToken(1);
        state.addAffectionToken(1);
        // this puts player 1 in the lead

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));


        // the invariant then to check is that for each node in the tree p1 has a higher score than p0 or p3 for all treea
        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node ->
                node.getVisits() <= 10 ||
                        node.getTotValue()[1] > node.getTotValue()[0] && node.getTotValue()[1] > node.getTotValue()[2]);
        assertEquals(0, problemNodes.size(), 0);
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node ->
                node.getVisits() <= 10 ||
                        node.getTotValue()[1] > node.getTotValue()[0] && node.getTotValue()[1] > node.getTotValue()[2]);
        assertEquals(0, problemNodes.size(), 0);
        problemNodes = mctsPlayer.getRoot(2).nonMatchingNodes(node ->
                node.getVisits() <= 10 ||
                        node.getTotValue()[1] > node.getTotValue()[0] && node.getTotValue()[1] > node.getTotValue()[2]);
        assertEquals(0, problemNodes.size(), 0);

    }

    @Test
    public void multiTreeTestLoveLetterRewardsParanoid() {
        params.budget = 1000;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTreeParanoid;
        fm = new LoveLetterForwardModel();
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();
        state.addAffectionToken(1);
        state.addAffectionToken(1);
        state.addAffectionToken(1);
        // this puts player 1 in the lead

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));


        // the invariant then to check is that for each node in the tree p1 has a higher score than p0 or p3 for all treea
        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node ->
                        node.getTotValue()[0] == -node.getTotValue()[1] && node.getTotValue()[0] == -node.getTotValue()[2]);
        assertEquals(0, problemNodes.size(), 0);
        problemNodes = mctsPlayer.getRoot(1).nonMatchingNodes(node ->
                node.getTotValue()[0] == -node.getTotValue()[1] && node.getTotValue()[0] == -node.getTotValue()[2]);
        assertEquals(0, problemNodes.size(), 0);
        problemNodes = mctsPlayer.getRoot(2).nonMatchingNodes(node ->
                node.getTotValue()[0] == -node.getTotValue()[1] && node.getTotValue()[0] == -node.getTotValue()[2]);
        assertEquals(0, problemNodes.size(), 0);

    }
}

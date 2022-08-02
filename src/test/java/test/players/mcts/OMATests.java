package test.players.mcts;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Token;
import games.tictactoe.*;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.*;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.summingInt;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

public class OMATests {
    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    Token x = TicTacToeConstants.playerMapping.get(0);
    Token o = TicTacToeConstants.playerMapping.get(1);

    AbstractForwardModel fm = new TicTacToeForwardModel();

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 20;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
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

    @Test
    public void omaNodesUsed() {
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
        System.out.println(stats);
        System.out.println(mctsPlayer.getRoot(0));
        assertEquals(params.budget, mctsPlayer.getRoot(0).getVisits());
        assertEquals(9, mctsPlayer.getRoot(0).getChildren().size());

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).checkTree(node -> node instanceof OMATreeNode);
        assertEquals(0, problemNodes.size());

        // and check that we have non-zero values (so that OMA does normal back-prop)
        assertNotEquals(0.0, mctsPlayer.getRoot(0).getTotValue()[0]);
        assertNotEquals(0.0, mctsPlayer.getRoot(0).getTotValue()[1]);
    }

    @Test
    public void omaParentsPopulatedOMA_All() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).checkTree(node -> {
            OMATreeNode n = (OMATreeNode) node;
            return (n.getDepth() <= 1 && !n.getOMAParent().isPresent()) || (n.getDepth() > 1 && n.getOMAParent().isPresent());
        });
        assertEquals(0, problemNodes.size());
        // this should just apply to the root node (depth 0), and all the first opponent moves (depth 1)
        assertFalse(((OMATreeNode) mctsPlayer.getRoot(0)).getOMAParent().isPresent());
    }

    @Test
    public void omaChildrenTieUpWithExpectedActions() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).checkTree(node -> {
            OMATreeNode n = (OMATreeNode) node;
            Set<AbstractAction> children = n.getOMAChildrenActions();
            Token expected = node.getActor() == 0 ? x : o;
            return children.stream().allMatch(c -> ((SetGridValueAction<Token>) c).getValue().equals(expected));
        });
        assertEquals(0, problemNodes.size());
        // this checks that all actions are for the same player as the node player
    }

    @Test
    public void omaParentsPopulatedOMA() {
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).checkTree(node -> node instanceof OMATreeNode);
        assertEquals(0, problemNodes.size());

        problemNodes = mctsPlayer.getRoot(0).checkTree(node -> {
            OMATreeNode n = (OMATreeNode) node;
            if (n.getActor() == 1)
                return !n.getOMAParent().isPresent();
            if (n.getDepth() == 0)
                return !n.getOMAParent().isPresent();
            return n.getOMAParent().isPresent();
        });
        assertEquals(0, problemNodes.size());
    }

    @Test
    public void omaStatisticsCorrectOMA() {
        // Then, for each OMA Parent node, we look at all its grandchildren, and check that
        // their statistics are correctly merged to give the node parent statistics
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, fm.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).checkTree(node -> {
                    OMATreeNode n = (OMATreeNode) node;
                    if (n.getOMAChildrenActions().isEmpty())
                        return true;
                    for (AbstractAction action : n.getChildren().keySet()) {
                        List<SingleTreeNode> grandchildren = n.getChildren().get(action)[1].checkTree(n2 -> n2.getDepth() == n.getDepth() + 2);
                        // We now need to get weighted value of grandchildren stats
                        double totalValue = grandchildren.stream().mapToDouble(gc -> gc.getTotValue()[0]).sum();
                        int totVisits = grandchildren.stream().mapToInt(SingleTreeNode::getVisits).sum();
                        OMATreeNode.OMAStats stats = n.getOMAStats(action);
                        System.out.printf("%s GC: %.2f/%d, OMA: %.2f/%d%n", action, totalValue, totVisits, stats.OMATotValue, stats.OMAVisits);
                        if (Math.abs(totVisits - stats.OMAVisits) > 1) return false;
                        if (Math.abs(totalValue - stats.OMATotValue) > 0.01) return false;
                    }
                    return true;
                }
            );
        assertEquals(0, problemNodes.size());

    }

    @Test
    public void omaStatisticsCorrectOMA_All() {

    }

}

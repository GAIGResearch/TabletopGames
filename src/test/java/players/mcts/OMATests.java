package players.mcts;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.BoardNode;
import core.components.Token;
import games.GameType;
import games.loveletter.*;
import games.tictactoe.*;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

public class OMATests {
    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    BoardNode x = TicTacToeConstants.playerMapping.get(0);
    BoardNode o = TicTacToeConstants.playerMapping.get(1);

    AbstractForwardModel ticTacToeForwardModel = new TicTacToeForwardModel();
    AbstractForwardModel loveLetterForwardModel = new LoveLetterForwardModel();

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams();
        params.setRandomSeed(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 20;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 5000;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.K = 1.0;
        params.omaVisits = 10;
    }

    public Game createTicTacToe(MCTSParams params, int gridSize) {
        mctsPlayer = new TestMCTSPlayer(params, null);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        TicTacToeGameParameters gameParams = new TicTacToeGameParameters();
        gameParams.gridSize = gridSize;
        Game game = GameType.TicTacToe.createGameInstance(2, gameParams);
        game.reset(players);
        return game;
    }

    public Game createLoveLetter(MCTSParams params) {
        mctsPlayer = new TestMCTSPlayer(params, null);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(-36572)));
        LoveLetterParameters gameParams = new LoveLetterParameters();
        gameParams.setRandomSeed(68274);
        Game game = GameType.LoveLetter.createGameInstance(players.size(), gameParams);
        game.reset(players);
        return game;
    }


    @Test
    public void omaNodesUsedTicTacToe() {
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                ._getAction(state, ticTacToeForwardModel.computeAvailableActions(state));

        TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
        System.out.println(stats);
        System.out.println(mctsPlayer.getRoot(0));
        assertEquals(params.budget, mctsPlayer.getRoot(0).getVisits());
        assertEquals(9, mctsPlayer.getRoot(0).getChildren().size());

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node instanceof OMATreeNode);
        assertEquals(0, problemNodes.size());

        // and check that we have non-zero values (so that OMA does normal back-prop)
        assertNotEquals(0.0, mctsPlayer.getRoot(0).nodeValue(0));
        assertNotEquals(0.0, mctsPlayer.getRoot(0).nodeValue(1));
    }

    @Test
    public void omaParentsPopulatedOMA_AllTicTacToe() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                ._getAction(state, ticTacToeForwardModel.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
            OMATreeNode n = (OMATreeNode) node;
            return (n.getDepth() <= 1 && !n.getOMAParent().isPresent()) || (n.getDepth() > 1 && n.getOMAParent().isPresent());
        });
        assertEquals(0, problemNodes.size());
        // this should just apply to the root node (depth 0), and all the first opponent moves (depth 1)
        assertFalse(((OMATreeNode) mctsPlayer.getRoot(0)).getOMAParent().isPresent());
    }

    @Test
    public void omaChildrenTieUpWithExpectedActionsTicTacToe() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                ._getAction(state, ticTacToeForwardModel.computeAvailableActions(state));


        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
            OMATreeNode n = (OMATreeNode) node;
            if (n.getOMAParent().isPresent()) {
                Set<AbstractAction> parentActions = n.getOMAParent().get().getChildren().keySet();
                return parentActions.containsAll(n.getOMAParentActions());
            } else
                return true;
        });
        assertEquals(0, problemNodes.size());


        problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
            OMATreeNode n = (OMATreeNode) node;
            if (n.getOMAParent().isPresent()) {
                OMATreeNode OMAParent = n.getOMAParent().get();
                Set<AbstractAction> parentActions = OMAParent.getChildren().keySet();
                BoardNode expected = node.getActor() == 0 ? x : o;
                return parentActions.stream().flatMap(a -> OMAParent.getOMAChildrenActions(a).stream())
                        .filter(Objects::nonNull)
                        .allMatch(c -> ((SetGridValueAction) c).getValue(state).equals(expected));
            } else return true;
        });
        assertEquals(0, problemNodes.size());
        // this checks that all actions are for the same player as the node player
    }

    @Test
    public void omaParentsPopulatedOMATicTacToe() {
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                ._getAction(state, ticTacToeForwardModel.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node instanceof OMATreeNode);
        assertEquals(0, problemNodes.size());

        problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
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
    public void omaParentsPopulatedOMALoveLetter() {
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                ._getAction(state, loveLetterForwardModel.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node instanceof OMATreeNode);
        assertEquals(0, problemNodes.size());

        List<SingleTreeNode> depthFourNodesWithOMAParents = mctsPlayer.getRoot(0).nonMatchingNodes(
                n -> {
                    OMATreeNode node = (OMATreeNode) n;
                    return !(node.getDepth() == 4 && node.getOMAParent().isPresent());
                }
        );

        problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
            OMATreeNode n = (OMATreeNode) node;
            if (n.getActor() == 1 || n.getActor() == 2)
                return !n.getOMAParent().isPresent();
            if (n.getDepth() == 0)
                return !n.getOMAParent().isPresent();
            return n.getOMAParent().isPresent();
        });
        assertEquals(0, problemNodes.size());
    }


    @Test
    public void omaStatisticsCorrectOMATicTacToe() {
        // Then, for each OMA Parent node, we look at all its grandchildren, and check that
        // their statistics are correctly merged to give the node parent statistics
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                ._getAction(state, ticTacToeForwardModel.computeAvailableActions(state));


        List<SingleTreeNode> problemNodes = checkOMAStats();
        assertEquals(0, problemNodes.size());
    }

    private List<SingleTreeNode> checkOMAStats() {
        return mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
            OMATreeNode n = (OMATreeNode) node;
            int player = n.getActor();
            for (AbstractAction parentAction : n.getChildren().keySet()) {
                if (n.getChildren().get(parentAction) != null) {
                    for (AbstractAction childAction : n.getOMAChildrenActions(parentAction)) {
                        // 'grandchildren' are the nodes from which we take our next action
                        List<SingleTreeNode> grandchildren = Arrays.stream(n.getChildren().get(parentAction))
                                .filter(Objects::nonNull)
                                .flatMap(child -> child.filterTree(
                                                // we want all nodes in the sub-tree that have the same decisionPlayer,
                                                // and where the closest parent for decisionPlayer is this node
                                                n2 -> n2.decisionPlayer == player &&
                                                        n2.matchingParent(y -> y.getActor() == player) == n)
                                        .stream())
                                .collect(toList());
                        // We now need to get weighted value of grandchildren stats
                        double totalValue = grandchildren.stream()
                                .map(gc -> gc.actionValues.get(childAction))
                                .filter(Objects::nonNull)
                                .mapToDouble(s -> s.totValue[player])
                                .sum();
                        int totVisits = grandchildren.stream()
                                .map(gc -> gc.actionValues.get(childAction))
                                .filter(Objects::nonNull)
                                .mapToInt(s -> s.nVisits)
                                .sum();
                        OMATreeNode.OMAStats stats = n.getOMAStats(parentAction, childAction);
                        System.out.printf("%s GC: %.2f/%d, OMA: %.2f/%d%n", childAction, totalValue, totVisits,
                                stats.OMATotValue, stats.OMAVisits);
                        if (Math.abs(totVisits - stats.OMAVisits) > 0) return false;
                        if (Math.abs(totalValue - stats.OMATotValue) > 0.01) return false;
                    }
                }
            }
            return true;
        });
    }

    @Test
    public void omaStatisticsCorrectOMALoveLetter() {
        // Then, for each OMA Parent node, we look at all its grandchildren, and check that
        // their statistics are correctly merged to give the node parent statistics
        Game game = createLoveLetter(params);
        LoveLetterGameState state = (LoveLetterGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                ._getAction(state, loveLetterForwardModel.computeAvailableActions(state));

        List<SingleTreeNode> problemNodes = checkOMAStats();
        assertEquals(0, problemNodes.size());
    }

    @Test
    public void omaStatisticsCorrectOMA_AllTicTacToe() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
        omaStatisticsCorrectOMATicTacToe();

        List<SingleTreeNode> playerTwoNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
                    OMATreeNode n = (OMATreeNode) node;
                    return !(n.getOMAParent().isPresent() && n.getActor() == 1 && !n.getOMAParent().get().getOMAParentActions().isEmpty());
                }
        );
        assertTrue(playerTwoNodes.size() > 5);
    }


    @Test
    public void omaStatisticsCorrectOMA_AllLoveLetter() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
        omaStatisticsCorrectOMALoveLetter();

        List<SingleTreeNode> playerTwoNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> {
                    OMATreeNode n = (OMATreeNode) node;
                    return !(n.getOMAParent().isPresent() && (n.getActor() == 1 || n.getActor() == 2) && !n.getOMAParent().get().getOMAParentActions().isEmpty());
                }
        );
        assertTrue(playerTwoNodes.size() > 100);
    }
}

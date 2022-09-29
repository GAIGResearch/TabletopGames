package test.players.mcts;

import com.sun.xml.bind.v2.runtime.unmarshaller.XsiNilLoader;
import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SetGridValueAction;
import core.components.Token;
import games.loveletter.*;
import games.tictactoe.*;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.*;
import players.simple.RandomPlayer;

import java.util.*;

import static java.util.stream.Collectors.*;
import static org.junit.Assert.*;

public class OMATests {
    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    Token x = TicTacToeConstants.playerMapping.get(0);
    Token o = TicTacToeConstants.playerMapping.get(1);

    AbstractForwardModel ticTacToeForwardModel = new TicTacToeForwardModel();
    AbstractForwardModel loveLetterForwardModel = new LoveLetterForwardModel();

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
        params.budget = 5000;
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

    public Game createLoveLetter(MCTSParams params) {
        mctsPlayer = new TestMCTSPlayer(params);
        mctsPlayer.setDebug(true);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(-36572)));
        return new LoveLetterGame(players, new LoveLetterParameters(68274));
    }


    @Test
    public void omaNodesUsedTicTacToe() {
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, ticTacToeForwardModel.computeAvailableActions(state));

        TreeStatistics stats = new TreeStatistics(mctsPlayer.getRoot(0));
        System.out.println(stats);
        System.out.println(mctsPlayer.getRoot(0));
        assertEquals(params.budget, mctsPlayer.getRoot(0).getVisits());
        assertEquals(9, mctsPlayer.getRoot(0).getChildren().size());

        List<SingleTreeNode> problemNodes = mctsPlayer.getRoot(0).nonMatchingNodes(node -> node instanceof OMATreeNode);
        assertEquals(0, problemNodes.size());

        // and check that we have non-zero values (so that OMA does normal back-prop)
        assertNotEquals(0.0, mctsPlayer.getRoot(0).getTotValue()[0]);
        assertNotEquals(0.0, mctsPlayer.getRoot(0).getTotValue()[1]);
    }

    @Test
    public void omaParentsPopulatedOMA_AllTicTacToe() {
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
        Game game = createTicTacToe(params, 3);
        TicTacToeGameState state = (TicTacToeGameState) game.getGameState();

        AbstractAction actionChosen = game.getPlayers().get(state.getCurrentPlayer())
                .getAction(state, ticTacToeForwardModel.computeAvailableActions(state));

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
                .getAction(state, ticTacToeForwardModel.computeAvailableActions(state));


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
                Token expected = node.getActor() == 0 ? x : o;
                return parentActions.stream().flatMap(a -> OMAParent.getOMAChildrenActions(a).stream())
                        .filter(Objects::nonNull)
                        .allMatch(c -> ((SetGridValueAction<Token>) c).getValue().equals(expected));
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
                .getAction(state, ticTacToeForwardModel.computeAvailableActions(state));

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
                .getAction(state, loveLetterForwardModel.computeAvailableActions(state));

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
                .getAction(state, ticTacToeForwardModel.computeAvailableActions(state));


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
                        // 'grandchildren' are then the nodes reached by childAction, which contain the stats that OMA should summarise
                        List<SingleTreeNode> grandchildren = Arrays.stream(n.getChildren().get(parentAction))
                                .filter(Objects::nonNull)
                                .flatMap(child -> child.filterTree(
                                                // we require this to have the correct childAction to reach, and its grandparent
                                                // measured purely in terms of the acting player needs to be n
                                                n2 -> n2.getActionToReach().equals(childAction) &&
                                                        n2.matchingParent(y -> y.getActor() == player).matchingParent(z -> z.getActor() == player) == n)
                                        .stream())
                                .collect(toList());
                        // We now need to get weighted value of grandchildren stats
                        double totalValue = grandchildren.stream().mapToDouble(gc -> gc.getTotValue()[player]).sum();
                        int totVisits = grandchildren.stream().mapToInt(SingleTreeNode::getVisits).sum();
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
                .getAction(state, loveLetterForwardModel.computeAvailableActions(state));

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
                    return !(n.getOMAParent().isPresent() && (n.getActor() == 1  || n.getActor() == 2) && !n.getOMAParent().get().getOMAParentActions().isEmpty());
                }
        );
        assertTrue(playerTwoNodes.size() > 100);
    }
}

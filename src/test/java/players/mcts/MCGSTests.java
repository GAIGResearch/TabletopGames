package players.mcts;

import core.AbstractForwardModel;
import core.AbstractPlayer;
import core.Game;
import core.components.Token;
import evaluation.features.TurnAndPlayerOnly;
import games.GameType;
import games.loveletter.LoveLetterParameters;
import games.tictactoe.TicTacToeConstants;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.function.Predicate;

import static org.junit.Assert.assertEquals;

public class MCGSTests {

    TestMCTSPlayer mctsPlayer;
    MCTSParams params;

    private final Predicate<SingleTreeNode> childrenVisitsAddUp = node ->
            node.getChildren().isEmpty() ||  // first condition is that this is a terminal node
                    node.getParent() == null || // the root node is different (and is checked elsewhere)
                    node.getChildren().values().stream().flatMap(arr -> {
                                if (arr == null)
                                    return new ArrayList<SingleTreeNode>().stream();
                                return Arrays.stream(arr);
                            }).filter(Objects::nonNull)
                            .anyMatch(n -> n.state != null && n.state.isNotTerminalForPlayer(n.decisionPlayer)) ||
                    node.getVisits() == node.getChildren().values().stream().mapToInt(arr -> {
                                if (arr == null)
                                    return 0;
                                int retValue = 1;
                                for (SingleTreeNode singleTreeNode : arr) {
                                    if (singleTreeNode != null)
                                        retValue += singleTreeNode.getVisits();
                                }
                                return retValue;
                            }
                    ).sum();

    private final Predicate<SingleTreeNode> actionVisitsAddUp = node ->
            node.getVisits() == node.actionValues.values().stream().mapToInt(s -> s.nVisits).sum();

    @Before
    public void setup() {
        // default Parameter settings for later changes
        params = new MCTSParams(9332);
        params.treePolicy = MCTSEnums.TreePolicy.UCB;
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        params.information = MCTSEnums.Information.Information_Set;
        params.maxTreeDepth = 200;
        params.rolloutLength = 10;
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 200;
        params.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        params.nodesStoreScoreDelta = false;
        params.maintainMasterState = false;
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
        LoveLetterParameters gameParams = new LoveLetterParameters(3812);
        Game game = GameType.LoveLetter.createGameInstance(players.size(), gameParams);
        game.reset(players);
        return game;
    }

    @Test
    public void SingleFileDotsAndBoxes() {
        // The aim here is to have a minimal (and silly) feature that consists only of the player ID and the Round
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
        params.MCGSStateFeatureVector = new TurnAndPlayerOnly();

        Game game = createDotsAndBoxes(params);
        // We now tun one turn of the game
        game.oneAction();
        // We now check that the MCGS tree has the expected number of nodes
        MCGSNode root = (MCGSNode) mctsPlayer.getRoot(0);
        assertEquals(200, root.getVisits());
        // We expand *actions* rather than nodes at the moment; so with 82 actions at root; 81 at depth 1; 80 at depth 2
        // we would expect 4 nodes (root; depth 1; depth 2; and depth 3) Depth 1 is created on the first iteration, depth 2 on the 83rd, and depth 3 on the 164th
        assertEquals(4, root.getTranspositionMap().size());

        for (int i = 0; i < 51; i++) {
            game.oneAction();
        }
        // We should now have 31 actions at the root
        root = (MCGSNode) mctsPlayer.getRoot(0);
        // assertEquals(200, root.getVisits());
        // We can get more visits to the root node than iterations if we have a 3-box in play (as this gives us another action without changing the turn)
        assertEquals(8, root.getTranspositionMap().size(), 1);
    }

}

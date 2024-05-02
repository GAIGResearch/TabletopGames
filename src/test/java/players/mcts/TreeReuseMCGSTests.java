package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import evaluation.features.StateKeyFromFeatureVector;
import games.GameType;
import games.dominion.DominionConstants;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.actions.BuyCard;
import games.dominion.cards.CardType;
import games.dominion.metrics.DomStateFeatures;
import games.dominion.metrics.DomStateFeaturesReduced;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameState;
import games.tictactoe.TicTacToeStateVector;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class TreeReuseMCGSTests {

    public TestMCTSPlayer playerOne;
    public TestMCTSPlayer playerTwo;
    public MCTSParams paramsOne, paramsTwo;
    public AbstractGameState state;
    public AbstractForwardModel fm;
    public Game game;


    @Before
    public void setUp() {
        paramsOne = new MCTSParams();
        paramsOne.budget = 200;
        //   paramsOne.discardStateAfterEachIteration = false;
        paramsOne.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        paramsOne.reuseTree = true;
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
        paramsTwo = new MCTSParams();
        paramsTwo.budget = 200;
        paramsTwo.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        //     paramsTwo.discardStateAfterEachIteration = false;
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGS;
    }

    public void initialiseTicTacToe() {
        paramsOne.MCGSStateKey = new StateKeyFromFeatureVector(new TicTacToeStateVector());
        paramsTwo.MCGSStateKey = new StateKeyFromFeatureVector(new TicTacToeStateVector());
        playerOne = new TestMCTSPlayer(paramsOne, MCGSNode::new);
        playerOne.rolloutTest = false;
        playerTwo = new TestMCTSPlayer(paramsTwo, MCGSNode::new);
        playerTwo.rolloutTest = false;
        fm = new TicTacToeForwardModel();
        game = GameType.TicTacToe.createGameInstance(2, 404);
        game.reset(List.of(playerOne, playerTwo));
        state = game.getGameState();
    }

    @Test
    public void treeReuseTest() {
        // TicTacToe may be a good test environment.
        // Run MCGS for 200 iterations.
        // After each action, we want to check that the tree is re-used.
        // This involves checking that the root node is one that used to exist in the old tree.
        // Plus that the number of visits

        // After each action, obtain the node that we should move to next.
        // Take the next action ,and check that this node is the new root of the tree
        // and has had its visit count increased by 100 (and children are also incremented in total by 100)

        // Repeat to the penultimate turn (when there is only one action left)
        initialiseTicTacToe();
        runGame();
    }

    @Test
    public void treeReuseWithEndOfTree() {
        // As previous test, but we set iterations to 20. This means that no node will have been
        // created for the next move until we get to the end of the game.
        paramsOne.budget = 20;
        paramsTwo.budget = 20;
        initialiseTicTacToe();
        runGame();
    }

    @Test
    public void treeReusedWithSelfOnly() {
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MCGSSelfOnly;
        initialiseTicTacToe();
        runGame();
    }

    public void runGame() {
        // For MCGS this should be much simpler than for MCTS
        // We track (copy) the state pre-Action.
        // Before the next decision is taken we work out what the root node will be (as it is based on the current state, and record the number of visits)
        // After the next decision is taken we check that the new root node is not the same as the old one; and that the old root node still exists.
        // And also that the number of visits to this node is 200 plus the number of visits to the old root node....
        // [as we don't prune this until the *next* decision is taken]
        // After the action is taken we confirm that the old root node no longer exists in the tree (i.e. that we do prune states)
        MCGSNode[] oldRoots = new MCGSNode[2];
        String[] oldKeys = new String[]{"", ""};
        String[] oldOldKeys = new String[]{"", ""};
        String[] oldOldOldKeys = new String[]{"", ""};
        List<Map<String, Integer>> oldVisitsMap = new ArrayList<>(2);
        oldVisitsMap.add(new HashMap<>());
        oldVisitsMap.add(new HashMap<>());
        List<Map<String, Integer>> visitsMap = new ArrayList<>(2);
        visitsMap.add(new HashMap<>());
        visitsMap.add(new HashMap<>());
        int[] oldVisits = new int[2];
        do {
            System.out.println("Current Player: " + state.getCurrentPlayer() + ", Turn: " + state.getTurnCounter() + ", Phase: " + state.getGamePhase());
            int currentPlayer = state.getCurrentPlayer();
            // we need to know how many visits there were to the old state before the next decision is taken
            oldOldOldKeys[currentPlayer] = oldOldKeys[currentPlayer];
            oldOldKeys[currentPlayer] = oldKeys[currentPlayer];
            oldKeys[currentPlayer] = paramsOne.MCGSStateKey.getKey(state);
            TestMCTSPlayer player = currentPlayer == 0 ? playerOne : playerTwo;
            oldRoots[currentPlayer] = (MCGSNode) player.root; // root from last action taken
            oldVisitsMap.remove(currentPlayer);
            oldVisitsMap.add(currentPlayer, new HashMap<>(visitsMap.get(currentPlayer)));
            if (player.root != null) {
                oldVisits[currentPlayer] = ((MCGSNode) playerOne.getRoot(0)).getTranspositionMap().getOrDefault(oldKeys[currentPlayer], new MCGSNode()).nVisits;
                visitsMap.remove(currentPlayer);
                visitsMap.add(currentPlayer, ((MCGSNode) playerOne.getRoot(0)).getTranspositionMap().entrySet().stream()
                        .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().nVisits), HashMap::putAll));
            }
            // when we take the next action we should first prune any states that were not updated last time
            // so we check that states in both trees have monotonic increasing visits
            boolean oneAction = fm.computeAvailableActions(state).size() == 1;
            AbstractAction nextAction = game.oneAction();
            System.out.println("Action: " + nextAction.toString());
            // newRoot is the root of the tree just used - i.e. it is rooted at the state before the action was taken
            MCGSNode newRoot = (MCGSNode) (currentPlayer == 0 ? playerOne.getRoot(0) : playerTwo.getRoot(1));
            if (!oneAction) {
                // check tree reuse
                if (oldRoots[currentPlayer] != null) {
                    assertNotEquals(oldRoots[currentPlayer], newRoot);
                    if (currentPlayer == 0) {
                        // old root still contains the starting state key
                        assertTrue(oldRoots[currentPlayer].getTranspositionMap().containsKey(oldOldKeys[currentPlayer]));
                        boolean searchExitedGraph = !oldRoots[currentPlayer].getTranspositionMap().containsKey(oldKeys[currentPlayer]);
                        if (!searchExitedGraph) {
                            assertSame(newRoot, oldRoots[currentPlayer].getTranspositionMap().get(oldKeys[currentPlayer]));
                        }
                        System.out.println("Visits: " + newRoot.getVisits());
                        assertEquals(oldVisits[0] + paramsOne.budget, newRoot.getVisits());
                        // and check older root is no longer in the tree
                        if (!oldOldOldKeys[currentPlayer].isEmpty()) {
                            assertFalse(newRoot.getTranspositionMap().containsKey(oldOldOldKeys[currentPlayer]));
                        }
                        // then for each node that was
                        Map<String, Integer> newVisitsMap = newRoot.getTranspositionMap().entrySet().stream()
                                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().nVisits), HashMap::putAll);
                        for (String key : oldVisitsMap.get(currentPlayer).keySet()) {
                            if (newVisitsMap.containsKey(key)) {
                                assertTrue(newVisitsMap.get(key) > oldVisitsMap.get(currentPlayer).get(key));
                            }
                        }
                    }
                    if (currentPlayer == 1)
                        assertEquals(paramsTwo.budget, newRoot.getVisits());
                }
            }
        } while (state.isNotTerminal());
    }
}

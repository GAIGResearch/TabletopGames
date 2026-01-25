package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import evaluation.features.StateKeyFromFeatureVector;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeStateVector;
import games.toads.ToadForwardModel;
import games.toads.metrics.ToadFeatures001;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;

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

    public void initialiseToads() {
        paramsOne.MCGSStateKey = new ToadFeatures001();
        paramsTwo.MCGSStateKey = new ToadFeatures001();
        playerOne = new TestMCTSPlayer(paramsOne, MCGSNode::new);
        playerOne.rolloutTest = false;
        playerTwo = new TestMCTSPlayer(paramsTwo, MCGSNode::new);
        playerTwo.rolloutTest = false;
        fm = new ToadForwardModel();
        game = GameType.WarOfTheToads.createGameInstance(2, 404);
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

    @Test
    public void treeReuseTestToads() {
        // A test in a more complex game
        initialiseToads();
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
        Map[] oldMaps = new Map[2];
        Object[] oldKeys = new Object[]{null, null};
        Object[] oldOldKeys = new Object[]{null, null};
        Object[] oldOldOldKeys = new Object[]{null, null};
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
            // however we need to copy the transposition map, as this is just passed by reference to the new root node
            if (player.root != null) {
                Map<Object, MCGSNode> mapCopy = new HashMap<>();
                for (Object key : ((MCGSNode) player.root).getTranspositionMap().keySet()) {
                    mapCopy.put(key, ((MCGSNode) player.root).getTranspositionMap().get(key));
                }
                oldMaps[currentPlayer] = mapCopy;
            }
            Map<Object, Integer> visitMapBeforeAction = new HashMap<>();
            Map<Object, Integer> depthMapBeforeAction = new HashMap<>();
            if (player.root != null) {
                MCGSNode oldRoot = (MCGSNode) player.root;
                oldVisits[currentPlayer] = ((MCGSNode) player.getRoot(currentPlayer)).getTranspositionMap().getOrDefault(oldKeys[currentPlayer], new MCGSNode()).nVisits;
                visitMapBeforeAction = oldRoot.getTranspositionMap().entrySet().stream()
                        .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().nVisits), HashMap::putAll);
                depthMapBeforeAction = oldRoot.getTranspositionMap().entrySet().stream()
                        .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().depth), HashMap::putAll);
            }
            // when we take the next action we should first prune any states that were not updated last time
            // so we check that states in both trees have monotonic increasing visits
            // and that the depth has been decreased correctly
            Object newKey = paramsOne.MCGSStateKey.getKey(state);
            int depthChange = depthMapBeforeAction.getOrDefault(newKey, 0);
            boolean oneAction = fm.computeAvailableActions(state).size() == 1;
            AbstractAction nextAction = game.oneAction();
            System.out.println("Action: " + nextAction.toString());
            // newRoot is the root of the tree just used - i.e. it is rooted at the state before the action was taken
            MCGSNode newRoot = (MCGSNode) player.getRoot(currentPlayer);

            // Now remove the nodes that were pruned before the action was taken (because they did not have any visits in the previous search)
            player.recentlyRemovedKeys.forEach(visitMapBeforeAction::remove);
            player.recentlyRemovedKeys.forEach(depthMapBeforeAction::remove);
            if (!oneAction) {
                // check tree reuse
                if (oldRoots[currentPlayer] != null) {
                    assertNotEquals(oldRoots[currentPlayer], newRoot);
                    if (currentPlayer == 0 && newRoot.nVisits > paramsOne.budget) {  // the one with reuseTree = true, and we did re-use some of the nodes
                        // old root still contains the starting state key
                        assertTrue(oldMaps[currentPlayer].containsKey(oldOldKeys[currentPlayer]));
                        boolean searchExitedGraph = !oldMaps[currentPlayer].containsKey(oldKeys[currentPlayer]);
                        if (!searchExitedGraph) {
                            assertSame(newRoot, oldMaps[currentPlayer].get(oldKeys[currentPlayer]));
                        }
                        System.out.println("Visits: " + newRoot.getVisits());
                        assertEquals(oldVisits[0] + paramsOne.budget, newRoot.getVisits());
                        // and check older root is no longer in the tree
                        if (oldOldOldKeys[currentPlayer] != null) {
                            assertFalse(newRoot.getTranspositionMap().containsKey(oldOldOldKeys[currentPlayer]));
                        }
                        // then for each node that was
                        Map<Object, Integer> visitMapAfterAction = newRoot.getTranspositionMap().entrySet().stream()
                                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().nVisits), HashMap::putAll);
                        Map<Object, Integer> depthMapAfterAction = newRoot.getTranspositionMap().entrySet().stream()
                                .collect(HashMap::new, (m, e) -> m.put(e.getKey(), e.getValue().depth), HashMap::putAll);
                        for (Object key : visitMapBeforeAction.keySet()) {
                            if (visitMapAfterAction.containsKey(key)) {
                                assertTrue(visitMapAfterAction.get(key) >= visitMapBeforeAction.get(key));
                                int oldDepth = depthMapBeforeAction.get(key);
                                int newDepth = depthMapAfterAction.get(key);
                                assertEquals(oldDepth - depthChange, newDepth);
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

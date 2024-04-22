package players.mcts;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import org.apache.arrow.vector.complex.impl.SingleListReaderImpl;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class TreeReuseTests {

    public TestMCTSPlayer playerOne;
    public TestMCTSPlayer playerTwo;
    public MCTSParams paramsOne, paramsTwo;
    public TicTacToeGameState state;
    public TicTacToeForwardModel fm;
    public Game game;


    @Before
    public void setUp() {
        paramsOne = new MCTSParams();
        paramsOne.budget = 200;
        paramsOne.discardStateAfterEachIteration = false;
        paramsOne.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        paramsOne.reuseTree = true;
        paramsTwo = new MCTSParams();
        paramsTwo.budget = 200;
        paramsTwo.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        paramsTwo.discardStateAfterEachIteration = false;
    }

    public void initialiseGame() {
        playerOne = new TestMCTSPlayer(paramsOne, STNWithTestInstrumentation::new);
        playerTwo = new TestMCTSPlayer(paramsTwo, STNWithTestInstrumentation::new);
        fm = new TicTacToeForwardModel();
        game = GameType.TicTacToe.createGameInstance(2, 404);
        game.reset(List.of(playerOne, playerTwo));
        state = (TicTacToeGameState) game.getGameState();
    }

    @Test
    public void treeReuseTest() {
        // TicTacToe may be a good test environment.
        // Run MCTS for 100 iterations.
        // After each action, we want to check that the tree is re-used.
        // This involves checking that the root node is one that used to exist in the old tree.
        // Plus that the number of visits

        // After each action, obtain the node that we should move to next.
        // Take the next action ,and check that this node is the new root of the tree
        // and has had its visit count increased by 100 (and children are also incremented in total by 100)

        // Repeat to the penultimate turn (when there is only one action left)
        initialiseGame();
        runGame();
    }

    @Test
    public void treeReuseWithEndOfTree() {
        // As previous test, but we set iterations to 5. This means that no node will have been
        // created for the next move until we get to the end of the game.
        paramsOne.budget = 20;
        paramsTwo.budget = 20;
        initialiseGame();
        runGame();
    }


    public void runGame() {
        AbstractAction[] lastActions = new AbstractAction[2];
        SingleTreeNode[] oldRoots = new SingleTreeNode[2];
        int oldVisits = 0;
        do {
            System.out.println("Current player: " + state.getCurrentPlayer() + ", Turn: " + state.getTurnCounter());
            int currentPlayer = state.getCurrentPlayer();
            AbstractAction nextAction = game.oneAction();
            SingleTreeNode newRoot = currentPlayer == 0 ? playerOne.getRoot(0) : playerTwo.getRoot(0);
            if (currentPlayer == 0) {
                // check tree reuse
                if (oldRoots[0] != null) {
                    assertNotEquals(oldRoots[0], playerOne.getRoot(0));
                    // we apply the last actions for p0 and p1 in sequence
                    SingleTreeNode expectedNewRoot = descendTree(oldRoots[0], new int[]{0, 1, 0}, List.of(lastActions[0], lastActions[1]));
                    if (expectedNewRoot != null) {
                        assertEquals(expectedNewRoot, newRoot);
                    }
                }
                assertEquals(paramsOne.budget + oldVisits, newRoot.getVisits());

            } else {
                // check tree is not reused, but also record the current number of visits on the playerOne node
                if (oldRoots[1] != null) {
                    assertNotEquals(oldRoots[1], playerTwo.getRoot(1));
                }
                assertEquals(paramsTwo.budget, playerTwo.getRoot(1).getVisits());
                SingleTreeNode nextNode = descendTree(oldRoots[0], new int[]{0, 1, 0}, List.of(lastActions[0], nextAction));
                oldVisits = nextNode == null ? 0 : nextNode.getVisits();
            }
            lastActions[currentPlayer] = nextAction;
            oldRoots[currentPlayer] = newRoot;
        } while (state.isNotTerminal() && fm.computeAvailableActions(state).size() > 1);
    }

    private SingleTreeNode descendTree(SingleTreeNode startingNode, int[] actingPlayers, List<AbstractAction> actions) {
        if (actingPlayers.length != actions.size() + 1)
            throw new AssertionError("actingPlayers must be one longer than actions");
        SingleTreeNode currentNode = startingNode;
        for (int i = 0; i < actions.size(); i++) {
            AbstractAction action = actions.get(i);
            SingleTreeNode[] nodeArray = currentNode.getChildren().get(action);
            if (nodeArray != null)
                currentNode = nodeArray[actingPlayers[i+1]];
            else
                currentNode = null;
            if (currentNode == null)
                break;
        }
        return currentNode;
    }


    @Test
    public void treeReusedAfterSingleAction() {
        // if we have a mandatory single action that did not go through MCTSPlayer.getAction()
        // check we still move to the correct place in the tree
    }
}

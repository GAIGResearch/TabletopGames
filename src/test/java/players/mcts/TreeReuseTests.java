package players.mcts;

import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.tictactoe.TicTacToeForwardModel;
import games.tictactoe.TicTacToeGameParameters;
import games.tictactoe.TicTacToeGameState;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;

import java.util.List;

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
        paramsOne.budget = 100;
        paramsOne.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        paramsOne.reuseTree = true;
        paramsTwo = new MCTSParams();
        paramsTwo.budget = 100;
        paramsTwo.budgetType = PlayerConstants.BUDGET_ITERATIONS;
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
        AbstractAction lastAction = null;
        do {
            game.oneAction();
        } while (state.isNotTerminal());

        // After each action, obtain the node that we should move to next.
        // Take the next action ,and check that this node is the new root of the tree
        // and has had its visit count increased by 100 (and children are also incremented in total by 100)

        // Repeat to end of game
    }

    @Test
    public void treeReuseWithEndOfTree() {
        // As previous test, but we set iterations to 5. This means that no node will have been
        // created for the next move until we get to the end of the game.
    }
}

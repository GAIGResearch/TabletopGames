package players.mcts;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IGamePhase;
import games.GameType;
import games.cantstop.CantStopForwardModel;
import games.dominion.DominionConstants;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.actions.BuyCard;
import games.dominion.cards.CardType;
import games.tictactoe.TicTacToeForwardModel;
import org.junit.Before;
import org.junit.Test;
import org.netlib.lapack.Dgetrf;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;

import static org.junit.Assert.*;

public class TreeReuseTests {

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
        paramsOne.discardStateAfterEachIteration = false;
        paramsOne.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        paramsOne.reuseTree = true;
        paramsTwo = new MCTSParams();
        paramsTwo.budget = 200;
        paramsTwo.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        paramsTwo.discardStateAfterEachIteration = false;
    }

    public void initialiseTicTacToe() {
        playerOne = new TestMCTSPlayer(paramsOne, STNWithTestInstrumentation::new);
        playerOne.rolloutTest = false;
        playerTwo = new TestMCTSPlayer(paramsTwo, STNWithTestInstrumentation::new);
        playerTwo.rolloutTest = false;
        fm = new TicTacToeForwardModel();
        game = GameType.TicTacToe.createGameInstance(2, 404);
        game.reset(List.of(playerOne, playerTwo));
        state = game.getGameState();
    }

    public void initialiseDominion() {
        playerOne = paramsOne.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.OMA
                ? new TestMCTSPlayer(paramsOne, OMATreeNode::new)
                : new TestMCTSPlayer(paramsOne, STNWithTestInstrumentation::new);
        playerOne.rolloutTest = false;
        playerTwo = new TestMCTSPlayer(paramsTwo, STNWithTestInstrumentation::new);
        playerTwo.rolloutTest = false;
        fm = new DominionForwardModel();
        game = GameType.Dominion.createGameInstance(3, 404);
        game.reset(List.of(playerOne, playerTwo, new RandomPlayer()));
        state = game.getGameState();
    }

    public void initialiseCantStop() {
        playerOne = new TestMCTSPlayer(paramsOne, STNWithTestInstrumentation::new);
        playerOne.rolloutTest = false;
        playerTwo = new TestMCTSPlayer(paramsTwo, STNWithTestInstrumentation::new);
        playerTwo.rolloutTest = false;
        fm = new CantStopForwardModel();
        game = GameType.CantStop.createGameInstance(3, 404);
        game.reset(List.of(playerOne, playerTwo, new RandomPlayer()));
        state = game.getGameState();
    }

    @Test
    public void treeReuseTestI() {
        // TicTacToe may be a good test environment.
        // Run MCTS for 100 iterations.
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
    public void treeReuseTestII() {
        initialiseCantStop();
        runGame();
    }

    @Test
    public void treeReuseWithEndOfTree() {
        // As previous test, but we set iterations to 5. This means that no node will have been
        // created for the next move until we get to the end of the game.
        paramsOne.budget = 20;
        paramsTwo.budget = 20;
        initialiseTicTacToe();
        runGame();
    }

    @Test
    public void treeReusedAfterSingleAction() {
        // if we have a mandatory single action that did not go through MCTSPlayer.getAction()
        // check we still move to the correct place in the tree
        // To test this we can use Dominion, in which the first couple of Turns will have the single END_PHASE action
        // to move on to the buy phase - as no player has any Action cards until these have been purchased and reshuffled into hand
        initialiseDominion();
        DominionGameState dgs = (DominionGameState) state;
        // this is to test more complicated actions
        dgs.addCard(CardType.MILITIA, 0, DominionConstants.DeckType.HAND);
        runGame();
    }

    @Test
    public void treeReusedWithSelfOnly() {
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly;
        initialiseDominion();
        DominionGameState dgs = (DominionGameState) state;
        // this is to test more complicated actions
        dgs.addCard(CardType.MINE, 0, DominionConstants.DeckType.HAND);
        runGame();
    }

    @Test
    public void treeReusedWithMultiTreeI() {
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        initialiseDominion();
        runGame();
    }

    @Test
    public void treeReusedWithMultiTreeII() {
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        initialiseCantStop();
        runGame();
    }

    @Test
    public void treeReusedWithOMA() {
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA;
        initialiseDominion();
        runGame();
    }

    @Test
    public void treeReusedWithRegretMatchingAndLowBudget() {
        paramsOne.treePolicy = MCTSEnums.TreePolicy.RegretMatching;
        paramsOne.budget = 8;
        paramsOne.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree;
        paramsTwo.treePolicy = MCTSEnums.TreePolicy.RegretMatching;
        paramsTwo.budget = 8;
        paramsTwo.reuseTree = true;
        for (int i = 0; i < 5; i++) {
            initialiseCantStop();
            runGame();
        }
    }


    private SingleTreeNode descendTree(SingleTreeNode startingNode, int[] actingPlayers, List<AbstractAction> actions) {
        if (actingPlayers.length != actions.size() + 1)
            throw new AssertionError("actingPlayers must be one longer than actions");
        SingleTreeNode currentNode = startingNode;
        if (currentNode == null)
            return null;
        for (int i = 0; i < actions.size(); i++) {
            AbstractAction action = actions.get(i);
            SingleTreeNode[] nodeArray = currentNode.getChildren().get(action);
            if (nodeArray != null)
                currentNode = nodeArray[actingPlayers[i + 1]];
            else
                currentNode = null;
            if (currentNode == null)
                break;
        }
        return currentNode;
    }

    public void runGame() {
        boolean selfOnlyTree = paramsOne.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.SelfOnly ||
                paramsOne.opponentTreePolicy == MCTSEnums.OpponentTreePolicy.MultiTree;
        List<AbstractAction> actionsTakenSinceLastPlayerZeroDecision = new ArrayList<>();
        List<Integer> nextActingPlayers = new ArrayList<>();
        nextActingPlayers.add(state.getCurrentPlayer());
        SingleTreeNode[] oldRoots = new SingleTreeNode[2];
        int oldVisits = 0;
        do {
            System.out.println("Current Player: " + state.getCurrentPlayer() + ", Turn: " + state.getTurnCounter() + ", Phase: " + state.getGamePhase());
            int currentPlayer = state.getCurrentPlayer();
            AbstractGameState preActionCopy = state.copy();
            boolean oneAction = fm.computeAvailableActions(state).size() == 1;
            AbstractAction nextAction = game.oneAction();
            System.out.println("Action: " + nextAction.toString());
            SingleTreeNode newRoot = (currentPlayer == 0 ? playerOne.getRoot(0) : playerTwo.getRoot(1));
            if (newRoot != null) {
                assertNull(newRoot.parent);
                assertEquals(newRoot.root, newRoot);
            }

            if (currentPlayer < 2 && newRoot != null) {
                assertEquals(currentPlayer, newRoot.decisionPlayer);
                SingleTreeNode topRoot = (currentPlayer == 0 ? playerOne.getRoot() : playerTwo.getRoot());
                assertEquals(currentPlayer, topRoot.decisionPlayer);
                assertNull(topRoot.parent);
                assertEquals(topRoot.root, topRoot);
            }
            if (currentPlayer == 0 && !oneAction && newRoot != null) {
                assertEquals(preActionCopy.getGamePhase(), newRoot.state.getGamePhase());
                // we also want to check if we have a whole load of ESTATE purchases
                if (preActionCopy instanceof DominionGameState dgs) {
                    if (newRoot instanceof STNWithTestInstrumentation STN) {
                        int ESTATE_Visits = STN.getActionStats(new BuyCard(CardType.ESTATE, 0)) == null ? 0
                                : STN.getActionStats(new BuyCard(CardType.ESTATE, 0)).validVisits;
                        int ESTATES_available = dgs.getCardsIncludedInGame().get(CardType.ESTATE);
                        if (ESTATES_available == 0)
                            assertTrue(ESTATE_Visits <= oldVisits);
                    }
                }
                // check tree reuse
                if (oldRoots[0] != null) {
                    assertNotEquals(oldRoots[0], playerOne.getRoot(0));
                    // we apply the last actions for p0 and p1 in sequence
                    SingleTreeNode expectedNewRoot = descendTree(oldRoots[0], nextActingPlayers.stream().mapToInt(i -> i).toArray(), actionsTakenSinceLastPlayerZeroDecision);
                    if (expectedNewRoot != null) {
                        assertEquals(expectedNewRoot, newRoot);
                    }
                }
                System.out.println("Visits: " + newRoot.getVisits());
                assertEquals(paramsOne.budget + oldVisits, newRoot.getVisits());
                // reset track of actions
                actionsTakenSinceLastPlayerZeroDecision.clear();
                nextActingPlayers.clear();
                nextActingPlayers.add(state.getCurrentPlayer());
            }

            if (currentPlayer == 0 || !selfOnlyTree)
                actionsTakenSinceLastPlayerZeroDecision.add(nextAction);
            if (state.getCurrentPlayer() == 0 || !selfOnlyTree)
                nextActingPlayers.add(state.getCurrentPlayer());

            if (currentPlayer != 2)
                oldRoots[currentPlayer] = newRoot;
            // if the next player is 0, then we determine how many oldVisits there are before they make the next decision
            if (state.getCurrentPlayer() == 0) {
                SingleTreeNode nextNode = descendTree(oldRoots[0], nextActingPlayers.stream().mapToInt(i -> i).toArray(), actionsTakenSinceLastPlayerZeroDecision);
                oldVisits = nextNode == null ? 0 : nextNode.getVisits();
            }
        } while (state.isNotTerminal());
    }
}

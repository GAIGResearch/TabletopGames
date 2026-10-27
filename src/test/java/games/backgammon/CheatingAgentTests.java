package games.backgammon;

import core.AbstractForwardModel;
import core.CoreConstants;
import core.DecoratedForwardModel;
import core.actions.AbstractAction;
import core.interfaces.IPlayerDecorator;
import games.backgammon.actions.LoadDice;
import games.backgammon.actions.LoadedDiceDecorator;
import games.backgammon.actions.MovePiece;
import games.backgammon.actions.RollDice;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import players.PlayerFactory;
import players.mcts.*;
import players.simple.RandomPlayer;
import utilities.Pair;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CheatingAgentTests {

    BGGameState gameState;
    BGParameters parameters;
    AbstractForwardModel forwardModel;
    TestMCTSPlayer decoratedMCTSPlayer;
    RandomPlayer decoratedRandomPlayer;

    @Before
    public void setUp() {
        parameters = new BGParameters();
        parameters.setParameterValue("doubleActions", false);
        gameState = new BGGameState(parameters, 2);
        forwardModel = new BGForwardModel();
        forwardModel.setup(gameState);
        assertEquals(new RollDice(), forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, new RollDice());

        decoratedMCTSPlayer = (TestMCTSPlayer) PlayerFactory.createPlayer("src/test/java/games/backgammon/CheatingAgent.json");
        decoratedMCTSPlayer.setPlayerID(0);
        decoratedMCTSPlayer.rolloutTest = false;
        decoratedMCTSPlayer.setForwardModel(forwardModel);

        decoratedRandomPlayer = new RandomPlayer();
        decoratedRandomPlayer.setPlayerID(0);
        decoratedRandomPlayer.setForwardModel(forwardModel);
        decoratedRandomPlayer.addDecorator(new LoadedDiceDecorator(6,
                new double[]{
                        0.167, 0.167, 0.166, 0.166, 0.167, 0.167,
                        0.1, 0.1, 0.1, 0.1, 0.1, 0.5,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0
                }, true, 0.00));
    }

    @Test
    public void loadCheatingAgentFromJSON() {
        List<IPlayerDecorator> decorators = decoratedMCTSPlayer.getDecorators();
        assertEquals(1, decorators.size());
        assertTrue(decorators.getFirst() instanceof LoadedDiceDecorator);

        LoadedDiceDecorator loadedDiceDecorator = (LoadedDiceDecorator) decorators.get(0);
        assertEquals(3, loadedDiceDecorator.getPDFCount());
        assertEquals(6, loadedDiceDecorator.getPDF(0).length);
        for (int i = 0; i < 6; i++) {
            assertEquals(1.0 / 6, loadedDiceDecorator.getPDF(0)[i], 1e-3);
        }
        assertEquals(6, loadedDiceDecorator.getPDF(2).length);
        for (int i = 0; i < 6; i++) {
            assertEquals(i == 0 ? 1.0 : 0.0, loadedDiceDecorator.getPDF(2)[i], 1e-3);
        }
        assertTrue(loadedDiceDecorator.isPermanent());
    }

    @Test
    public void cheaterGetsAdditionalOptionsInRollDicePhase() {
        // move two pieces
        // here we use the forward model outside of the AbstractPlayer.getAction() .. which is tested later
        IPlayerDecorator loadedDiceDecorator = decoratedMCTSPlayer.getDecorators().getFirst();
        forwardModel = (new DecoratedForwardModel(forwardModel)).addDecorator(1, loadedDiceDecorator);

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        // check we have cheat options for player 1
        assertEquals(BGGamePhase.RollDice, gameState.getGamePhase());
        assertEquals(1, gameState.getCurrentPlayer());
        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(new RollDice(), actions.getFirst());
        assertEquals(3, actions.size());
        assertTrue(actions.get(1) instanceof LoadDice);
        assertTrue(actions.get(2) instanceof LoadDice);

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        // and not for player 0 : they can only roll the dice, no cheating options
        assertEquals(BGGamePhase.RollDice, gameState.getGamePhase());
        assertEquals(0, gameState.getCurrentPlayer());
        actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(new RollDice(), actions.getFirst());
        assertEquals(1, actions.size());
    }

    @Test
    public void loadDiceNotAddedInMovePiecesPhase() {
        IPlayerDecorator loadedDiceDecorator = decoratedMCTSPlayer.getDecorators().getFirst();
        forwardModel = (new DecoratedForwardModel(forwardModel)).addDecorator(1, loadedDiceDecorator);

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        // player 1 just rolls the dice
        assertEquals(1, gameState.getCurrentPlayer());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        assertEquals(BGGamePhase.MovePieces, gameState.getGamePhase());
        assertEquals(1, gameState.getCurrentPlayer());


        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        assertTrue(actions.stream().allMatch(a -> a instanceof MovePiece));

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        actions = forwardModel.computeAvailableActions(gameState);
        assertTrue(actions.stream().allMatch(a -> a instanceof MovePiece));
    }

    @Test
    public void loadingTheDiceAlsoRollsThem() {
        // here we use the forward model outside of the AbstractPlayer.getAction() .. which is tested later
        IPlayerDecorator loadedDiceDecorator = decoratedMCTSPlayer.getDecorators().getFirst();
        forwardModel = (new DecoratedForwardModel(forwardModel)).addDecorator(1, loadedDiceDecorator);
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        // check we have cheat options for player 1
        assertEquals(BGGamePhase.RollDice, gameState.getGamePhase());
        assertEquals(1, gameState.getCurrentPlayer());
        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(3, actions.size());
        assertTrue(actions.get(1) instanceof LoadDice);
        assertTrue(actions.get(2) instanceof LoadDice);

        int[] oldDiceValues = gameState.availableDiceValues.clone();
        forwardModel.next(gameState, actions.get(2));
        // check that the dice have been rolled
        assertEquals(BGGamePhase.MovePieces, gameState.getGamePhase());
        assertFalse(oldDiceValues[0] == gameState.availableDiceValues[0] && oldDiceValues[1] == gameState.availableDiceValues[1]);
    }

    @Test
    public void loadDiceActionChangesTheProbabilities() {
        LoadedDiceDecorator loadedDiceDecorator = (LoadedDiceDecorator) decoratedMCTSPlayer.getDecorators().get(0);
        forwardModel = (new DecoratedForwardModel(forwardModel)).addDecorator(1, loadedDiceDecorator);

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        LoadDice loadDiceAction = (LoadDice) actions.get(1);
        forwardModel.next(gameState, loadDiceAction);

        // this should include rolling the dice
        assertEquals(BGGamePhase.MovePieces, gameState.getGamePhase());
        assertEquals(1, gameState.getCurrentPlayer());

        // check the expected pdf has been loaded
        double[] newPDF = gameState.dice[0].getPdf();
        for (int i = 0; i < newPDF.length; i++) {
            assertEquals(i == 5 ? 0.5 : 0.1, newPDF[i], 0.001);
        }

        // Now we roll the dice 100 times, and expect about 50 sixes
        int sixCountOne = 0;
        int sixCountTwo = 0;
        for (int i = 0; i < 100; i++) {
            gameState.rollDice();
            if (gameState.availableDiceValues[0] == 6) {
                sixCountOne++;
            }
            if (gameState.availableDiceValues[1] == 6) {
                sixCountTwo++;
            }
        }
        assertTrue(sixCountOne > 35);
        assertTrue(sixCountTwo < 25);
        assertTrue(sixCountTwo > 4);
    }

    @Test
    public void loadDiceOptionsExcludeTheCurrentSelectedPdfViaFM() {
        LoadedDiceDecorator loadedDiceDecorator = new LoadedDiceDecorator(6,
                new double[]{
                        0.167, 0.167, 0.166, 0.166, 0.167, 0.167,
                        0.1, 0.1, 0.1, 0.1, 0.1, 0.5,
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0
                }, true, 0.00);
        forwardModel = (new DecoratedForwardModel(forwardModel)).addDecorator(1, loadedDiceDecorator);

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        List<AbstractAction> actions = forwardModel.computeAvailableActions(gameState);
        forwardModel.next(gameState, actions.get(1));

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        forwardModel.next(gameState, new RollDice()); // p0
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        assertEquals(BGGamePhase.RollDice, gameState.getGamePhase());
        assertEquals(1, gameState.getCurrentPlayer());
        actions = forwardModel.computeAvailableActions(gameState);
        assertEquals(3, actions.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(0.167, ((LoadDice) actions.get(1)).getPdf()[i], 0.01);
            assertEquals(i == 0 ? 1.0 : 0.0, ((LoadDice) actions.get(2)).getPdf()[i], 1e-3);
        }

    }

    @Test
    public void loadDiceOptionsExcludeTheCurrentSelectedPdfViaPlayer() {
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        assertEquals(0, gameState.getCurrentPlayer());

        LoadedDiceDecorator loadedDiceDecorator = (LoadedDiceDecorator) decoratedRandomPlayer.getDecorators().getFirst();
        AbstractAction actionTaken;
        int count = 0;
        do {
            actionTaken = decoratedRandomPlayer.getAction(gameState, forwardModel.computeAvailableActions(gameState));
            count++;
        } while (count < 10 && !(actionTaken instanceof LoadDice));
        if (!(actionTaken instanceof LoadDice)) {
            fail("Failed to take a LoadDice action after 10 attempts, got " + actionTaken);
        }
        int selectedPdf = ((LoadDice) actionTaken).getPdf()[0] == 1.0 ? 2 : 1; // we have two options, one with pdf[0] = 1.0, and one with pdf[0] = 0.1
        forwardModel.next(gameState, actionTaken);

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        forwardModel.next(gameState, new RollDice()); // p1
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        List<AbstractAction> actions = loadedDiceDecorator.actionFilter(gameState, forwardModel.computeAvailableActions(gameState));
        assertEquals(BGGamePhase.RollDice, gameState.getGamePhase());
        assertEquals(0, gameState.getCurrentPlayer());
        assertEquals(3, actions.size());
        for (int i = 0; i < 6; i++) {
            assertEquals(0.167, ((LoadDice) actions.get(1)).getPdf()[i], 0.01);
            if (selectedPdf == 1)
                assertEquals(i == 0 ? 1.0 : 0.0, ((LoadDice) actions.get(2)).getPdf()[i], 1e-3);
            else
                assertEquals(i == 5 ? 0.5 : 0.1, ((LoadDice) actions.get(2)).getPdf()[i], 1e-3);
        }
    }

    @Test
    public void decoratorOnlyAppliesLoadedDiceForSpecifiedPlayer() {
        // the decorator is for player 0...so player 1 cannot select anything other than rolling the dice
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());

        assertEquals(1, gameState.getCurrentPlayer());
        AbstractAction actionTaken;
        int count = 0;
        do {
            assertEquals(1, forwardModel.computeAvailableActions(gameState).size());
            actionTaken = decoratedMCTSPlayer.getAction(gameState, forwardModel.computeAvailableActions(gameState));
            count++;
        } while (count < 10 && !(actionTaken instanceof LoadDice));
        assertFalse(actionTaken instanceof LoadDice);
    }

    @Test
    public void undecoratedPlayerNeverUsesLoadDice() {
        // first check that MCTS search for a different agent never Loads Dice
        decoratedMCTSPlayer.clearDecorators();
        decoratedMCTSPlayer.setForwardModel(forwardModel);  // the old was was already decorated
        decoratedMCTSPlayer.getAction(gameState, forwardModel.computeAvailableActions(gameState));
        SingleTreeNode root = decoratedMCTSPlayer.getRoot();
        int[] countOfLoadDice = new int[2];

        Queue<SingleTreeNode> queue = new LinkedList<>();
        if (root instanceof MultiTreeNode mtn) {
            queue.add(mtn.getRoot(0));
            queue.add(mtn.getRoot(1));
        } else {
            queue.add(root);
        }
        while (!queue.isEmpty()) {
            SingleTreeNode node = queue.poll();
            Map<AbstractAction, SingleTreeNode[]> data = node.getChildren();
            int player = node.getActor();
            for (AbstractAction action : data.keySet()) {
                // we check to see what the action is, add it to the count if relevant
                // and then add the children nodes to the queue
                if (action instanceof LoadDice) {
                    countOfLoadDice[player]++;
                }
                SingleTreeNode[] children = data.get(action);
                if (children != null)
                    // add non-null entries
                    for (SingleTreeNode child : children) {
                        if (child != null) {
                            queue.add(child);
                        }
                    }
            }
        }
        assertEquals(0, countOfLoadDice[0]);
        assertEquals(0, countOfLoadDice[1]);
    }

    @Test
    public void decoratedPlayerNeverUsesLoadDiceForOpponent() {
        // then check that when planning for us, we only use LoadDice for our action
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        assertEquals(1, gameState.getCurrentPlayer());

        decoratedMCTSPlayer.getAction(gameState, forwardModel.computeAvailableActions(gameState));
        int[] countOfLoadDice = new int[2];
        Queue<SingleTreeNode> queue = new LinkedList<>();
        SingleTreeNode root = decoratedMCTSPlayer.getRoot();
        if (root instanceof MultiTreeNode mtn) {
            queue.add(mtn.getRoot(0));
            queue.add(mtn.getRoot(1));
        } else {
            queue.add(root);
        }
        while (!queue.isEmpty()) {
            SingleTreeNode node = queue.poll();
            Map<AbstractAction, SingleTreeNode[]> data = node.getChildren();
            int player = node.getActor();
            for (AbstractAction action : data.keySet()) {
                // we check to see what the action is, add it to the count if relevant
                // and then add the children nodes to the queue
                if (action instanceof LoadDice) {
                    countOfLoadDice[player]++;
                }
                SingleTreeNode[] children = data.get(action);
                if (children != null) {
                    // add non-null entries
                    for (SingleTreeNode child : children) {
                        if (child != null) {
                            queue.add(child);
                        }
                    }
                }
            }
        }
        System.out.println("countOfLoadDice: " + countOfLoadDice[0] + " :   " + countOfLoadDice[1]);
        assertNotEquals(0, countOfLoadDice[0]);
        assertEquals(0, countOfLoadDice[1]);
    }

    @Test
    public void decoratorResetIsCalledOncePerIteration() {
        IPlayerDecorator mockedDecorator = Mockito.mock(IPlayerDecorator.class);
        when(mockedDecorator.actionFilter(any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(1)); // return the actions unchanged
        when(mockedDecorator.copy()).thenReturn(mockedDecorator); // return the same instance for simplicity
        decoratedMCTSPlayer.addDecorator(mockedDecorator);
        decoratedMCTSPlayer.getAction(gameState, forwardModel.computeAvailableActions(gameState));
        verify(mockedDecorator, times(1000)).reset(); // once per iteration
        verify(mockedDecorator, times(3)).copy();
        // Currently we have the forward model on each of the player root nodes referenced from MultiTreeNode
        // as well as on the MultTreeNode giving P +1 copies with MultiTree, which feels a little inefficient
        // But, tampering with this would be too dangerous
    }

    @Test
    public void checkNoConsecutiveLoadOrRollDiceActions() {
        // on each MCTS rollout we should not have any consecutive actions from the RollDice/LoadDice family

        for (int move = 0; move < 10; move++) {
            AbstractAction actionTaken = decoratedMCTSPlayer.getAction(gameState, forwardModel.computeAvailableActions(gameState));
            // now check the rollout
            SingleTreeNode root = decoratedMCTSPlayer.getRoot();
            List<Pair<Integer, AbstractAction>> rolloutActions = root.getActionsInRollout();
            AbstractAction previousAction = null;
            for (Pair<Integer, AbstractAction> pair : rolloutActions) {
                if (pair.b instanceof RollDice || pair.b instanceof LoadDice) {
                    assertFalse(previousAction instanceof RollDice);
                    assertFalse(previousAction instanceof LoadDice);
                }
                previousAction = pair.b;
            }

            forwardModel.next(gameState, actionTaken);
        }
    }

    @Test
    public void oneOffShiftRevertsPdfBackAfterRoll() {
        decoratedMCTSPlayer = (TestMCTSPlayer) PlayerFactory.createPlayer("src/test/java/games/backgammon/CheatingAgentOneOff.json");
        decoratedMCTSPlayer.setPlayerID(1);
        decoratedMCTSPlayer.rolloutTest = false;
        decoratedMCTSPlayer.setForwardModel(forwardModel);

        assertTrue(decoratedMCTSPlayer.getDecorators().getFirst() instanceof LoadedDiceDecorator);
        LoadedDiceDecorator loadedDiceDecorator = (LoadedDiceDecorator) decoratedMCTSPlayer.getDecorators().getFirst();
        assertFalse(loadedDiceDecorator.isPermanent());

        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        assertEquals(1, gameState.getCurrentPlayer());

        List<AbstractAction> actions = decoratedMCTSPlayer.getForwardModel().computeAvailableActions(gameState);
        assertEquals(3, actions.size());
        for (int i = 0; i < 10; i++) {
            forwardModel.next(gameState, actions.get(2)); // we take the one-off shift
            assertEquals(1, gameState.availableDiceValues[0]);
            assertEquals(0.167, gameState.getDicePdf(0)[0], 0.001); // check the pdf is the old one
        }

    }

    @Test
    public void cheatingDetectionEndsGameWithCheaterLosing() {
        int detections = 0;
        int trials = 100;

        TestMCTSPlayer caughtMCTSPlayer = (TestMCTSPlayer) PlayerFactory.createPlayer("src/test/java/games/backgammon/CheatingAgentGetsCaught.json");
        IPlayerDecorator loadedDiceDecorator = caughtMCTSPlayer.getDecorators().getFirst();

        for (int i = 0; i < trials; i++) {
            BGGameState state = (BGGameState) gameState.copy();
            state.setGamePhase(BGGamePhase.RollDice);
            // find a LoadDice action
            List<AbstractAction> actions = loadedDiceDecorator.actionFilter(state, forwardModel.computeAvailableActions(state));
            LoadDice loadDice = (LoadDice) actions.stream().filter(a -> a instanceof LoadDice).findFirst().orElse(null);

            forwardModel.next(state, loadDice);
            if (state.isGameOver()) {
                detections++;
                assertEquals(15.0, state.getGameScore(1), 0.001);
                assertEquals(0.0, state.getGameScore(0), 0.001);
                assertEquals(1, state.getOrdinalPosition(1));
                assertEquals(2, state.getOrdinalPosition(0));
                assertEquals(CoreConstants.GameResult.LOSE_GAME, state.getPlayerResults()[0]);
                assertEquals(CoreConstants.GameResult.WIN_GAME, state.getPlayerResults()[1]);
                assertEquals(Set.of(1), state.getWinners());
            }
        }

        // with 10% probability, we expect around 10 detections in 100 trials.
        assertTrue("Detections: " + detections, detections > 3 && detections < 20);
    }

    @Test
    public void incrementingCheatCountAndDetectionChance() {
        parameters = new BGParameters();
        gameState = new BGGameState(parameters, 2);
        forwardModel = new BGForwardModel();
        forwardModel.setup(gameState);

        double baseDetectionChance = 0.05;
        LoadedDiceDecorator decorator = new LoadedDiceDecorator(6,
                new double[]{
                        1.0, 0.0, 0.0, 0.0, 0.0, 0.0
                }, false, true, baseDetectionChance);

        assertEquals(0, gameState.getCheatCount(0));
        assertEquals(0, gameState.getCheatCount(1));

        // Initial RollDice phase for Player 0
        int currentPlayer = gameState.getCurrentPlayer();
        assertEquals(0, currentPlayer);

        List<AbstractAction> availableActions = forwardModel.computeAvailableActions(gameState);
        List<AbstractAction> decoratedActions = decorator.actionFilter(gameState, availableActions);

        // Check that LoadDice actions have the correct initial detection chance
        // chance = baseDetectionChance * (cheatCount + 1) = 0.05 * (0 + 1) = 0.05
        for (AbstractAction a : decoratedActions) {
            if (a instanceof LoadDice ld) {
                assertEquals(baseDetectionChance, ld.getDetectionChance(), 0.001);
            }
        }

        // Execute a LoadDice action
        LoadDice loadDice = (LoadDice) decoratedActions.stream().filter(a -> a instanceof LoadDice).findFirst().get();
        forwardModel.next(gameState, loadDice);

        // Check cheat count incremented
        assertEquals(1, gameState.getCheatCount(currentPlayer));
        assertEquals(0, gameState.getCheatCount(1 - currentPlayer));

        // Now check detection chance for the NEXT time they want to cheat
        // We need to be in RollDice phase again for the same player (for simplicity of testing)
        // so roll game forward
        while (!(gameState.getCurrentPlayer() == currentPlayer && gameState.getGamePhase() == BGGamePhase.RollDice)) {
            forwardModel.next(gameState, forwardModel.computeAvailableActions(gameState).getFirst());
        }

        // chance = baseDetectionChance * (cheatCount + 1) = 0.05 * (1 + 1) = 0.10
        double expectedChance = baseDetectionChance * 2;

        decoratedActions = decorator.actionFilter(gameState, forwardModel.computeAvailableActions(gameState));
        assertTrue(decoratedActions.stream().anyMatch(a -> a instanceof LoadDice));
        for (AbstractAction a : decoratedActions) {
            if (a instanceof LoadDice ld) {
                assertEquals(expectedChance, ld.getDetectionChance(), 0.001);
            }
        }
        forwardModel.next(gameState, decoratedActions.stream().filter(a -> a instanceof LoadDice).findFirst().get());
        assertEquals(2,  gameState.getCheatCount(currentPlayer));

        forwardModel.setup(gameState);
        assertEquals(0, gameState.getCheatCount(currentPlayer));

    }
}

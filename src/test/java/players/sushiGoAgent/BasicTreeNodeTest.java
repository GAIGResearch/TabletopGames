package players.sushiGoAgent;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import games.GameType;
import games.sushigo.SGGameState;
import games.sushigo.actions.ChooseCard;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class BasicTreeNodeTest {

    private BasicMCTSParams params(int iterations) {
        BasicMCTSParams params = new BasicMCTSParams();
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = iterations;
        params.maxTreeDepth = 5;
        params.rolloutLength = 6;
        return params;
    }

    private Object field(BasicTreeNode node, String name) throws ReflectiveOperationException {
        Field field = BasicTreeNode.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(node);
    }

    @Test
    public void sushiGoSearchResamplesEachIterationAndPreservesObservation() throws ReflectiveOperationException {
        Game game = GameType.SushiGo.createGameInstance(3, 101);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));

        for (int seat = 0; seat < 3; seat++) {
            SushiGoMCTSPlayer player = new SushiGoMCTSPlayer(params(40));
            player.setPlayerID(seat);
            AbstractForwardModel forwardModel = spy(game.getForwardModel());
            player.setForwardModel(forwardModel);
            SGGameState observation = spy((SGGameState) game.getGameState().copy(seat));
            SGGameState before = (SGGameState) observation.copy();
            List<AbstractAction> actions = forwardModel.computeAvailableActions(observation);
            int observer = seat;
            Set<List<String>> sampledHands = new HashSet<>();
            doAnswer(invocation -> {
                AbstractGameState state = invocation.getArgument(0);
                AbstractAction action = invocation.getArgument(1);
                if (state.getGameTick() == before.getGameTick()) {
                    SGGameState sampled = (SGGameState) state;
                    assertEquals(before.getPlayerHands().get(observer), sampled.getPlayerHands().get(observer));
                    sampledHands.add(sampled.getPlayerHands().get((observer + 1) % 3).getComponents().stream()
                            .map(card -> card.type.name() + card.count).toList());
                }
                assertTrue("Search tried an illegal action",
                        game.getForwardModel().computeAvailableActions(state, player.getParameters().actionSpace)
                                .contains(action));
                return invocation.callRealMethod();
            }).when(forwardModel).next(any(AbstractGameState.class), any(AbstractAction.class));

            BasicTreeNode root = new BasicTreeNode(player, null, observation, new Random(4711));
            root.mctsSearch();
            AbstractAction chosen = root.bestAction();

            verify(observation, times(40)).copy(seat);
            assertTrue("Hidden hands were not re-sampled", sampledHands.size() > 1);
            assertEquals(before.getPlayerHands(), observation.getPlayerHands());
            assertEquals(before.getPlayedCards(), observation.getPlayedCards());
            assertEquals(before.getCardChoices(), observation.getCardChoices());
            assertArrayEquals(before.getPlayerScore(), observation.getPlayerScore());
            assertArrayEquals(before.getPlayedCardTypes(), observation.getPlayedCardTypes());
            assertEquals(before.getGameTick(), observation.getGameTick());
            assertEquals(before.getCurrentPlayer(), observation.getCurrentPlayer());
            assertEquals(before.getRoundCounter(), observation.getRoundCounter());
            assertTrue(actions.contains(chosen));
            assertEquals(seat, ((ChooseCard) chosen).playerId);
            assertEquals(40, field(root, "nVisits"));
            assertSame(root, root.root);
            assertNull(root.parent);
            assertEquals(0, root.depth);
            int totalVisits = 0;
            int highestVisits = 0;
            for (BasicTreeNode child : root.children.values()) {
                if (child != null) {
                    assertSame(root, child.root);
                    assertSame(root, child.parent);
                    assertEquals(1, child.depth);
                    int visits = (int) field(child, "nVisits");
                    totalVisits += visits;
                    highestVisits = Math.max(highestVisits, visits);
                }
            }
            assertEquals(40, totalVisits);
            assertEquals(highestVisits, field(root.children.get(chosen), "nVisits"));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void sharesChildrenAcrossDeterminizationsAndUsesActionAvailability() throws ReflectiveOperationException {
        BasicMCTSParams params = params(9);
        params.maxTreeDepth = 2;
        params.rolloutLength = 0;
        params.epsilon = 0;
        params.heuristic = (state, playerId) -> 0.0;
        SushiGoMCTSPlayer player = new SushiGoMCTSPlayer(params);
        player.setPlayerID(0);
        AbstractForwardModel forwardModel = mock(AbstractForwardModel.class);
        player.setForwardModel(forwardModel);
        AbstractAction rootAction = action();
        AbstractAction x = action();
        AbstractAction y = action();
        AbstractGameState observation = mock(AbstractGameState.class);
        when(observation.isNotTerminal()).thenReturn(true);
        when(forwardModel.computeAvailableActions(eq(observation), any())).thenReturn(List.of(rootAction));

        AtomicInteger samples = new AtomicInteger();
        AtomicBoolean bothLegal = new AtomicBoolean();
        when(observation.copy(0)).thenAnswer(invocation -> {
            boolean xLegal = samples.getAndIncrement() % 2 == 0;
            AtomicInteger depth = new AtomicInteger();
            AbstractGameState determinization = mock(AbstractGameState.class);
            when(determinization.isNotTerminal()).thenAnswer(ignored -> depth.get() < 2);
            when(determinization.getCurrentPlayer()).thenReturn(0);
            when(determinization.copy()).thenReturn(determinization);
            when(forwardModel.computeAvailableActions(eq(determinization), any())).thenAnswer(ignored ->
                    depth.get() == 0 ? List.of(rootAction)
                            : bothLegal.get() ? List.of(y, x) : List.of(xLegal ? x : y));
            doAnswer(next -> {
                AbstractAction chosen = next.getArgument(1);
                assertTrue(forwardModel.computeAvailableActions(determinization, params.actionSpace).contains(chosen));
                depth.incrementAndGet();
                return null;
            }).when(forwardModel).next(eq(determinization), any(AbstractAction.class));
            return determinization;
        });

        BasicTreeNode root = new BasicTreeNode(player, null, observation, new Random(4711));
        root.mctsSearch();
        BasicTreeNode shared = root.children.get(rootAction);
        assertEquals(9, samples.get());
        assertEquals(9, field(shared, "nVisits"));
        assertEquals(2, shared.children.size());
        assertEquals(4, field(shared.children.get(x), "nVisits"));
        assertEquals(4, field(shared.children.get(y), "nVisits"));
        assertEquals(Map.of(x, 4, y, 4), field(shared, "availabilityCounts"));
        assertSame(rootAction, root.bestAction());

        // Equal values/visits: ISUCT must explore the action that was available more often.
        bothLegal.set(true);
        AbstractGameState sample = observation.copy(0);
        forwardModel.next(sample, rootAction);
        Method setState = BasicTreeNode.class.getDeclaredMethod("setState", AbstractGameState.class);
        setState.setAccessible(true);
        setState.invoke(shared, sample);
        Map<AbstractAction, Integer> availability =
                (Map<AbstractAction, Integer>) field(shared, "availabilityCounts");
        availability.put(x, 100);
        availability.put(y, 1);
        Method ucb = BasicTreeNode.class.getDeclaredMethod("ucb");
        ucb.setAccessible(true);
        assertSame(x, ucb.invoke(shared));
    }

    @Test
    public void playerEntryPointAndCopyRemainCompatible() {
        Game game = GameType.SushiGo.createGameInstance(3, 202);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        BasicMCTSParams params = params(15);
        params.setRandomSeed(4711);
        SushiGoMCTSPlayer player = new SushiGoMCTSPlayer(params);
        player.setStateHeuristic((state, playerId) -> state.getGameScore(playerId));

        for (SushiGoMCTSPlayer candidate : List.of(player, player.copy())) {
            candidate.setPlayerID(2);
            candidate.setForwardModel(game.getForwardModel());
            AbstractGameState observation = game.getGameState().copy(2);
            List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(observation);
            assertTrue(actions.contains(candidate._getAction(observation, actions)));
        }
    }

    @Test
    public void supportsForwardModelCopyAndCombinedBudgets() throws ReflectiveOperationException {
        Game game = GameType.SushiGo.createGameInstance(3, 303);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        for (PlayerConstants budgetType : List.of(PlayerConstants.BUDGET_FM_CALLS,
                PlayerConstants.BUDGET_COPY_CALLS, PlayerConstants.BUDGET_FMANDCOPY_CALLS)) {
            BasicMCTSParams params = params(12);
            params.budgetType = budgetType;
            SushiGoMCTSPlayer player = new SushiGoMCTSPlayer(params);
            player.setPlayerID(0);
            player.setForwardModel(game.getForwardModel());
            AbstractGameState observation = game.getGameState().copy(0);
            BasicTreeNode root = new BasicTreeNode(player, null, observation, new Random(4711));
            root.mctsSearch();
            int calls = switch (budgetType) {
                case BUDGET_FM_CALLS -> (int) field(root, "fmCallsCount");
                case BUDGET_COPY_CALLS -> (int) field(root, "copyCallsCount");
                case BUDGET_FMANDCOPY_CALLS ->
                        (int) field(root, "fmCallsCount") + (int) field(root, "copyCallsCount");
                default -> throw new AssertionError("Unexpected budget");
            };
            assertTrue(calls >= params.budget);
            assertTrue(game.getForwardModel().computeAvailableActions(observation).contains(root.bestAction()));
        }
    }

    @Test(timeout = 10000)
    public void completesSushiGoGameWithAllSeatsUsingIsmcts() {
        Game game = GameType.SushiGo.createGameInstance(3, 404);
        game.reset(List.of(new SushiGoMCTSPlayer(params(12)),
                new SushiGoMCTSPlayer(params(12)), new SushiGoMCTSPlayer(params(12))));
        game.run();
        assertFalse(game.getGameState().isNotTerminal());
    }

    @Test(timeout = 10000)
    public void timeBudgetSearchReturnsLegalAction() throws ReflectiveOperationException {
        Game game = GameType.SushiGo.createGameInstance(3, 505);
        game.reset(List.of(new RandomPlayer(), new RandomPlayer(), new RandomPlayer()));
        BasicMCTSParams params = params(20);
        params.budgetType = PlayerConstants.BUDGET_TIME;
        SushiGoMCTSPlayer player = new SushiGoMCTSPlayer(params);
        player.setPlayerID(1);
        player.setForwardModel(game.getForwardModel());
        AbstractGameState observation = game.getGameState().copy(1);
        BasicTreeNode root = new BasicTreeNode(player, null, observation, new Random(4711));
        root.mctsSearch();
        assertTrue((int) field(root, "nVisits") > 0);
        assertTrue(game.getForwardModel().computeAvailableActions(observation).contains(root.bestAction()));
    }

    private AbstractAction action() {
        AbstractAction action = mock(AbstractAction.class);
        when(action.copy()).thenReturn(action);
        return action;
    }
}

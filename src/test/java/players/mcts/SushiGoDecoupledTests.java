package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import games.GameType;
import games.sushigo.SGGameState;
import games.sushigo.actions.ChooseCard;
import games.sushigo.cards.SGCard;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Decoupled search on a real simultaneous-move game. Three-player SushiGo: every turn starts with
 * all three players deciding at once, so the root of every search is a multi-actor node.
 */
public class SushiGoDecoupledTests {

    private static MCTSParams params(Consumer<MCTSParams> tweak) {
        MCTSParams p = new MCTSParams();
        p.setRandomSeed(4711);
        p.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        p.budget = 200;
        p.rolloutLength = 30;
        p.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        p.information = MCTSEnums.Information.Information_Set;
        tweak.accept(p);
        return p;
    }

    private static TestMCTSPlayer mcts(Consumer<MCTSParams> tweak) {
        TestMCTSPlayer player = new TestMCTSPlayer(params(tweak));
        player.rolloutTest = false;
        return player;
    }

    private static Game game(long seed, List<AbstractPlayer> players) {
        Game g = GameType.SushiGo.createGameInstance(3, seed);
        g.reset(players);
        return g;
    }

    private static Game threeMCTS(long seed, Consumer<MCTSParams> tweak) {
        return game(seed, List.of(mcts(tweak), mcts(tweak), mcts(tweak)));
    }

    /** One decision for the given seat, from that seat's own observation, as the game loop would ask for it. */
    private static AbstractAction decide(Game g, int seat) {
        AbstractGameState obs = g.getGameState().copy(seat);
        List<AbstractAction> actions = g.getForwardModel().computeAvailableActions(obs);
        AbstractAction chosen = g.getPlayers().get(seat).getAction(obs, actions);
        assertTrue("chosen action is not legal", actions.contains(chosen));
        return chosen;
    }

    private static SingleTreeNode root(Game g, int seat) {
        return ((TestMCTSPlayer) g.getPlayers().get(seat)).getRoot();
    }

    // ------------------------------------------------------------------ the single most important test

    @Test
    public void rootReturnsOwnAction() {
        Game g = threeMCTS(101, p -> {
        });
        for (int seat = 0; seat < 3; seat++) {
            AbstractAction chosen = decide(g, seat);
            assertTrue(chosen instanceof ChooseCard);
            assertEquals("seat " + seat + " returned another player's action", seat, ((ChooseCard) chosen).playerId);
            SingleTreeNode root = root(g, seat);
            assertTrue(root.isMultiActor());
            assertEquals(seat, root.getActor());
            assertEquals(g.getGameState().copy(seat).getCurrentSimultaneousPlayers(), root.getActingPlayers());
        }
    }

    @Test
    public void actingPlayersMatchGameStateAndChildrenAreJoint() {
        Game g = threeMCTS(202, p -> {
        });
        decide(g, 1);
        SingleTreeNode root = root(g, 1);
        assertEquals(List.of(0, 1, 2), root.getActingPlayers());
        assertEquals(Set.of(0, 1, 2), root.statsByPlayer.keySet());
        assertFalse(root.children.isEmpty());
        for (Map.Entry<AbstractAction, SingleTreeNode[]> e : root.children.entrySet()) {
            assertTrue(e.getKey() instanceof SimultaneousAction);
            Map<Integer, AbstractAction> components = ((SimultaneousAction) e.getKey()).getPlayerActions();
            assertEquals(Set.of(0, 1, 2), components.keySet());
            for (Map.Entry<Integer, AbstractAction> c : components.entrySet())
                assertEquals((int) c.getKey(), ((ChooseCard) c.getValue()).playerId);
            assertNotNull(e.getValue());
        }
        // and each player's table accounts for every visit
        for (int p : root.getActingPlayers())
            assertEquals(root.getVisits(), root.getActionValues(p).values().stream().mapToInt(s -> s.nVisits).sum());
    }

    @Test
    public void decoupledOffIsSequential() {
        Game g = threeMCTS(303, p -> p.decoupled = false);
        AbstractAction chosen = decide(g, 2);
        assertEquals(2, ((ChooseCard) chosen).playerId);
        SingleTreeNode root = root(g, 2);
        assertFalse(root.isMultiActor());
        assertEquals(List.of(2), root.getActingPlayers());
        for (AbstractAction key : root.children.keySet())
            assertTrue(key instanceof ChooseCard);
    }

    // ------------------------------------------------------------------ variants

    @Test
    public void mastActiveOnTheJointPath() {
        // the SushiGo.json configuration, on an iteration budget
        Game g = threeMCTS(404, p -> {
            p.MAST = MCTSEnums.MASTType.Both;
            p.useMAST = true;
            p.treePolicy = MCTSEnums.TreePolicy.UCB_Tuned;
        });
        decide(g, 0);
        SingleTreeNode root = root(g, 0);
        for (int p = 0; p < 3; p++) {
            Map<Object, utilities.Pair<Integer, Double>> stats = root.MASTStatistics.get(p);
            assertFalse("no MAST statistics for P" + p, stats.isEmpty());
            for (Object key : stats.keySet())
                assertTrue(key instanceof ChooseCard);
        }
    }

    @Test
    public void closedLoop() {
        Game g = threeMCTS(505, p -> {
            p.information = MCTSEnums.Information.Closed_Loop;
            p.discardStateAfterEachIteration = false;
        });
        AbstractAction chosen = decide(g, 0);
        assertEquals(0, ((ChooseCard) chosen).playerId);
        SingleTreeNode root = root(g, 0);
        assertTrue(root.isMultiActor());
        for (Map.Entry<AbstractAction, SingleTreeNode[]> e : root.children.entrySet()) {
            assertTrue(e.getKey() instanceof SimultaneousAction);
            assertEquals(1, Arrays.stream(e.getValue()).filter(Objects::nonNull).count());
        }
    }

    @Test
    public void regretMatching() {
        Game g = threeMCTS(606, p -> p.treePolicy = MCTSEnums.TreePolicy.RegretMatching);
        AbstractAction chosen = decide(g, 1);
        assertEquals(1, ((ChooseCard) chosen).playerId);
        SingleTreeNode root = root(g, 1);
        for (int p = 0; p < 3; p++)
            assertFalse("no average policy for P" + p, root.statsByPlayer.get(p).regretMatchingAverage.isEmpty());
    }

    @Test
    public void exp3() {
        Game g = threeMCTS(707, p -> p.treePolicy = MCTSEnums.TreePolicy.EXP3);
        assertEquals(2, ((ChooseCard) decide(g, 2)).playerId);
    }

    @Test
    public void toStringAndTreeStatisticsSurvive() {
        Game g = threeMCTS(808, p -> {
        });
        decide(g, 0);
        SingleTreeNode root = root(g, 0);
        String s = root.toString();
        assertTrue(s.contains("Player 0:"));
        assertTrue(s.contains("Player 2:"));
        TreeStatistics stats = new TreeStatistics(root);
        assertEquals(root.allNodesInTree().size(), stats.totalNodes);
    }

    @Test
    public void chopsticksInsideTree() {
        // Force players 0 and 1 to hold played Chopsticks, so that joint actions in which one or
        // both use them appear in the tree. Using chopsticks opens an extended sequence for that
        // player alone, so the node after such a joint action has a single acting player.
        Game g = threeMCTS(909, p -> p.budget = 400);
        SGGameState state = (SGGameState) g.getGameState();
        for (int p : List.of(0, 1)) {
            // both the type counter the action list reads, and the card the reveal hands back
            state.getPlayedCardTypes()[p].get(SGCard.SGCardType.Chopsticks).setValue(1);
            state.getPlayedCards().get(p).add(new SGCard(SGCard.SGCardType.Chopsticks));
        }
        AbstractAction chosen = decide(g, 0);
        assertEquals(0, ((ChooseCard) chosen).playerId);
        SingleTreeNode root = root(g, 0);
        int chopstickChildren = 0, bothChopsticks = 0;
        for (Map.Entry<AbstractAction, SingleTreeNode[]> e : root.children.entrySet()) {
            Map<Integer, AbstractAction> components = ((SimultaneousAction) e.getKey()).getPlayerActions();
            List<Integer> users = components.entrySet().stream()
                    .filter(c -> ((ChooseCard) c.getValue()).useChopsticks).map(Map.Entry::getKey).toList();
            if (users.isEmpty()) continue;
            chopstickChildren++;
            if (users.size() == 2) bothChopsticks++;
            for (SingleTreeNode child : e.getValue()) {
                if (child == null) continue;
                assertFalse("node after a chopsticks joint action should have one acting player", child.isMultiActor());
                assertTrue("the acting player must be one of the chopstick users", users.contains(child.getActor()));
            }
        }
        assertTrue("no joint action used chopsticks", chopstickChildren > 0);
        assertTrue("no joint action had both players using chopsticks", bothChopsticks > 0);
    }

    @Test
    public void reuseTreeIsSkippedAtASimultaneousRoot() {
        Game g = threeMCTS(1010, p -> p.reuseTree = true);
        // two full simultaneous turns through the real game loop; the second search of each
        // player finds a root from the first and must start afresh rather than reuse it
        g.oneAction();
        g.oneAction();
        for (int seat = 0; seat < 3; seat++) {
            SingleTreeNode root = root(g, seat);
            assertNotNull(root);
            assertTrue(root.isMultiActor());
            assertEquals(0, root.inheritedVisits);
            assertNull(root.getParent());
        }
    }

    @Test
    public void wholeGameWithMixedSeats() {
        Game g = game(1111, List.of(mcts(p -> p.budget = 50), new RandomPlayer(new Random(3)), mcts(p -> p.budget = 50)));
        g.run();
        assertFalse(g.getGameState().isNotTerminal());
    }
}

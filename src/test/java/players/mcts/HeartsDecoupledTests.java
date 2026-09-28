package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import games.GameType;
import games.hearts.HeartsGameState;
import games.hearts.actions.Pass;
import games.hearts.actions.Play;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Decoupled search on Hearts, which mixes a simultaneous phase (passing) with a sequential one
 * (trick play). The root is multi-actor while passing and single-actor during play, and the node
 * after the last joint pass is where the tree switches from one to the other.
 */
public class HeartsDecoupledTests {

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
        Game g = GameType.Hearts.createGameInstance(4, seed);
        g.reset(players);
        return g;
    }

    private static Game fourMCTS(long seed, Consumer<MCTSParams> tweak) {
        return game(seed, List.of(mcts(tweak), mcts(tweak), mcts(tweak), mcts(tweak)));
    }

    private static int playerOf(AbstractAction a) {
        if (a instanceof Pass pass) return pass.playerID;
        if (a instanceof Play play) return play.playerID;
        throw new AssertionError("unexpected action " + a);
    }

    /** One passing sub-turn applied to the real game state, everyone committing the first card of their hand. */
    private static void passSubTurn(Game g) {
        HeartsGameState s = (HeartsGameState) g.getGameState();
        for (int i = 0; i < 4; i++) {
            int p = s.getCurrentPlayer();
            g.getForwardModel().next(s, new Pass(p, s.getPlayerDecks().get(p).get(0)));
        }
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

    @Test
    public void rootIsMultiActorWhilePassing() {
        Game g = fourMCTS(101, p -> {
        });
        for (int seat = 0; seat < 4; seat++) {
            AbstractAction chosen = decide(g, seat);
            assertTrue(chosen instanceof Pass);
            assertEquals("seat " + seat + " returned another player's action", seat, playerOf(chosen));
            SingleTreeNode root = root(g, seat);
            assertTrue(root.isMultiActor());
            assertEquals(seat, root.getActor());
            assertEquals(List.of(0, 1, 2, 3), root.getActingPlayers());
            assertEquals(Set.of(0, 1, 2, 3), root.statsByPlayer.keySet());
            for (Map.Entry<AbstractAction, SingleTreeNode[]> e : root.children.entrySet()) {
                assertTrue(e.getKey() instanceof SimultaneousAction);
                Map<Integer, AbstractAction> components = ((SimultaneousAction) e.getKey()).getPlayerActions();
                assertEquals(Set.of(0, 1, 2, 3), components.keySet());
                for (Map.Entry<Integer, AbstractAction> c : components.entrySet()) {
                    assertTrue(c.getValue() instanceof Pass);
                    assertEquals((int) c.getKey(), playerOf(c.getValue()));
                }
            }
            for (int p : root.getActingPlayers()) {
                // our own table is our hand; an opponent's hand is redeterminised every iteration, so
                // their table spans every hidden card they were ever dealt, up to all 39 of them
                int tableSize = root.getActionValues(p).size();
                if (p == seat) assertEquals(13, tableSize);
                else assertTrue("P" + p + " table has " + tableSize + " actions", tableSize >= 13 && tableSize <= 39);
                assertEquals(root.getVisits(), root.getActionValues(p).values().stream().mapToInt(s -> s.nVisits).sum());
            }
        }
    }

    @Test
    public void nodeAfterTheLastJointPassIsSingleActor() {
        Game g = fourMCTS(202, p -> p.budget = 400);
        passSubTurn(g);
        passSubTurn(g);
        HeartsGameState state = (HeartsGameState) g.getGameState();
        assertEquals(HeartsGameState.Phase.PASSING, state.getGamePhase());
        assertEquals(2, state.fewestPendingPasses());

        decide(g, 0);
        SingleTreeNode root = root(g, 0);
        assertTrue(root.isMultiActor());
        int childNodes = 0;
        for (Map.Entry<AbstractAction, SingleTreeNode[]> e : root.children.entrySet()) {
            assertTrue(e.getKey() instanceof SimultaneousAction);
            SingleTreeNode[] slots = e.getValue();
            for (int slot = 0; slot < slots.length; slot++) {
                if (slots[slot] == null) continue;
                childNodes++;
                // trick play has started: one actor, the holder of the 2 of clubs in that determinisation,
                // and the child is filed under that player
                assertFalse("node after the last pass should have one acting player", slots[slot].isMultiActor());
                assertEquals(slot, slots[slot].getActor());
            }
        }
        assertTrue("the tree never reached trick play", childNodes > 0);
    }

    @Test
    public void rootIsSequentialDuringTrickPlay() {
        Game g = fourMCTS(303, p -> {
        });
        passSubTurn(g);
        passSubTurn(g);
        passSubTurn(g);
        HeartsGameState state = (HeartsGameState) g.getGameState();
        assertEquals(HeartsGameState.Phase.PLAYING, state.getGamePhase());
        // play out the first trick: the 2 of clubs lead is the only legal action, and a player
        // with a single action does not search at all
        for (int i = 0; i < 4; i++) {
            List<AbstractAction> legal = g.getForwardModel().computeAvailableActions(state);
            g.getForwardModel().next(state, legal.get(0));
        }
        int current = state.getCurrentPlayer();
        assertTrue(g.getForwardModel().computeAvailableActions(state).size() > 1);

        AbstractAction chosen = decide(g, current);
        assertTrue(chosen instanceof Play);
        assertEquals(current, playerOf(chosen));
        SingleTreeNode root = root(g, current);
        assertFalse(root.isMultiActor());
        assertEquals(current, root.getActor());
        assertEquals(List.of(current), root.getActingPlayers());
        for (AbstractAction key : root.children.keySet()) {
            assertTrue(key instanceof Play);
            assertEquals(current, playerOf(key));
        }
    }

    @Test
    public void decoupledOffIsSequential() {
        Game g = fourMCTS(404, p -> p.decoupled = false);
        AbstractAction chosen = decide(g, 2);
        assertTrue(chosen instanceof Pass);
        assertEquals(2, playerOf(chosen));
        SingleTreeNode root = root(g, 2);
        assertFalse(root.isMultiActor());
        assertEquals(List.of(2), root.getActingPlayers());
        for (AbstractAction key : root.children.keySet()) {
            assertTrue(key instanceof Pass);
            assertEquals(2, playerOf(key));
        }
    }

    @Test
    public void wholeGameThroughTheGameLoop() {
        Game g = fourMCTS(505, p -> p.budget = 50);
        g.oneAction();
        for (int seat = 0; seat < 4; seat++) {
            SingleTreeNode root = root(g, seat);
            assertNotNull(root);
            assertTrue(root.isMultiActor());
        }
        g.run();
        assertFalse(g.getGameState().isNotTerminal());
    }

    @Test
    public void wholeGameWithMixedSeats() {
        Game g = game(606, List.of(mcts(p -> p.budget = 50), new RandomPlayer(new Random(3)),
                mcts(p -> {
                    p.budget = 50;
                    p.decoupled = false;
                }), new RandomPlayer(new Random(4))));
        g.run();
        assertFalse(g.getGameState().isNotTerminal());
    }
}

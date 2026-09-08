package players.mcts;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import games.GameType;
import games.diamant.DiamantGameState;
import games.diamant.actions.ContinueInCave;
import games.diamant.actions.ExitFromCave;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * Decoupled search on Diamant. Four players, everyone in the cave decides at once, and once some
 * players have left the cave the acting set is only those who remain.
 */
public class DiamantDecoupledTests {

    private static MCTSParams params(Consumer<MCTSParams> tweak) {
        MCTSParams p = new MCTSParams();
        p.setRandomSeed(4711);
        p.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        p.budget = 200;
        p.rolloutLength = 30;
        p.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        p.information = MCTSEnums.Information.Information_Set;
        p.decoupled = true;  // these tests exercise the decoupled path; the default is sequential
        tweak.accept(p);
        return p;
    }

    private static TestMCTSPlayer mcts(Consumer<MCTSParams> tweak) {
        TestMCTSPlayer player = new TestMCTSPlayer(params(tweak));
        player.rolloutTest = false;
        return player;
    }

    private static Game game(long seed, List<AbstractPlayer> players) {
        Game g = GameType.Diamant.createGameInstance(4, seed);
        g.reset(players);
        return g;
    }

    private static Game fourMCTS(long seed, Consumer<MCTSParams> tweak) {
        return game(seed, List.of(mcts(tweak), mcts(tweak), mcts(tweak), mcts(tweak)));
    }

    private static int playerOf(AbstractAction a) {
        if (a instanceof ContinueInCave c) return c.playerId;
        if (a instanceof ExitFromCave e) return e.playerId;
        throw new AssertionError("unexpected action " + a);
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
    public void rootReturnsOwnAction() {
        Game g = fourMCTS(101, p -> {
        });
        for (int seat = 0; seat < 4; seat++) {
            AbstractAction chosen = decide(g, seat);
            assertEquals("seat " + seat + " returned another player's action", seat, playerOf(chosen));
            SingleTreeNode root = root(g, seat);
            assertTrue(root.isMultiActor());
            assertEquals(seat, root.getActor());
            assertEquals(List.of(0, 1, 2, 3), root.getActingPlayers());
        }
    }

    @Test
    public void childrenAreJointActionsOverEveryoneInTheCave() {
        Game g = fourMCTS(202, p -> {
        });
        decide(g, 1);
        SingleTreeNode root = root(g, 1);
        assertEquals(Set.of(0, 1, 2, 3), root.statsByPlayer.keySet());
        assertFalse(root.children.isEmpty());
        for (Map.Entry<AbstractAction, SingleTreeNode[]> e : root.children.entrySet()) {
            assertTrue(e.getKey() instanceof SimultaneousAction);
            Map<Integer, AbstractAction> components = ((SimultaneousAction) e.getKey()).getPlayerActions();
            assertEquals(Set.of(0, 1, 2, 3), components.keySet());
            for (Map.Entry<Integer, AbstractAction> c : components.entrySet())
                assertEquals((int) c.getKey(), playerOf(c.getValue()));
        }
        // two choices each, so at most 16 joint actions, and each player's table accounts for every visit
        assertTrue(root.children.size() <= 16);
        for (int p : root.getActingPlayers()) {
            assertEquals(2, root.getActionValues(p).size());
            assertEquals(root.getVisits(), root.getActionValues(p).values().stream().mapToInt(s -> s.nVisits).sum());
        }
    }

    @Test
    public void actingSetIsOnlyThoseStillInTheCave() {
        Game g = fourMCTS(303, p -> {
        });
        DiamantGameState state = (DiamantGameState) g.getGameState();
        // players 0 and 2 leave; the first turn resolves without a hazard being possible
        g.getForwardModel().next(state, new ExitFromCave(0));
        g.getForwardModel().next(state, new ContinueInCave(1));
        g.getForwardModel().next(state, new ExitFromCave(2));
        g.getForwardModel().next(state, new ContinueInCave(3));
        assertEquals(List.of(1, 3), state.getPlayersInCave());

        for (int seat : List.of(1, 3)) {
            AbstractAction chosen = decide(g, seat);
            assertEquals(seat, playerOf(chosen));
            SingleTreeNode root = root(g, seat);
            assertTrue(root.isMultiActor());
            assertEquals(List.of(1, 3), root.getActingPlayers());
            assertEquals(Set.of(1, 3), root.statsByPlayer.keySet());
            for (AbstractAction key : root.children.keySet())
                assertEquals(Set.of(1, 3), ((SimultaneousAction) key).getPlayerActions().keySet());
        }
    }

    @Test
    public void nodeValueWhereTheRootPlayerIsNotActing() {
        // Once the searching player has left the cave the others still decide together, so the tree
        // holds multi-actor nodes at which the root player does not act and has no statistics table;
        // and because all joint successors of an action share one child slot, a node can be visited
        // with different acting sets on different iterations. nodeValue(p) must report the mean
        // reward over every visit regardless: it must agree with the table of any player who acted
        // on every visit, and must not create a table for a player who never acted here.
        // Regression test: reading the root player's table gave 0 at such nodes, and reading the
        // last visit's first actor gave a partial sum when that player had acted on only some visits.
        Game g = fourMCTS(707, p -> p.budget = 400);
        decide(g, 0);
        SingleTreeNode root = root(g, 0);
        int rootAbsentNodes = 0, checkedAgainstTables = 0;
        for (SingleTreeNode node : root.allNodesInTree()) {
            if (node.getVisits() == 0) continue;
            boolean rootAbsent = node.isMultiActor() && !node.statsByPlayer.containsKey(0);
            if (rootAbsent) rootAbsentNodes++;
            for (Map.Entry<Integer, PlayerDecisionStats> e : node.statsByPlayer.entrySet()) {
                Collection<ActionStats> table = e.getValue().actionValues.values();
                if (table.stream().mapToInt(s -> s.nVisits).sum() != node.getVisits())
                    continue; // this player acted on only some visits, so their table is a partial sum
                checkedAgainstTables++;
                for (int player = 0; player < 4; player++) {
                    final int pid = player;
                    double expected = table.stream().mapToDouble(s -> s.totValue[pid]).sum() / node.getVisits();
                    assertEquals("node value for P" + pid + " at depth " + node.depth + " via P" + e.getKey() + "'s table",
                            expected, node.nodeValue(pid), 1e-9);
                }
            }
            if (rootAbsent)
                assertFalse("nodeValue created a table for a player who does not act here", node.statsByPlayer.containsKey(0));
        }
        assertTrue("expected nodes at which the root player never acted, found none", rootAbsentNodes > 0);
        assertTrue(checkedAgainstTables > 0);
    }

    @Test
    public void decoupledOffIsSequential() {
        Game g = fourMCTS(404, p -> p.decoupled = false);
        AbstractAction chosen = decide(g, 2);
        assertEquals(2, playerOf(chosen));
        SingleTreeNode root = root(g, 2);
        assertFalse(root.isMultiActor());
        assertEquals(List.of(2), root.getActingPlayers());
        for (AbstractAction key : root.children.keySet())
            assertEquals(2, playerOf(key));
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
                mcts(p -> p.decoupled = false), new RandomPlayer(new Random(4))));
        g.run();
        assertFalse(g.getGameState().isNotTerminal());
    }
}

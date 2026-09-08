package players.mcts;

import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IStateHeuristic;
import org.junit.Test;
import players.PlayerConstants;
import utilities.Pair;

import java.util.*;
import java.util.function.Consumer;

import static org.junit.Assert.*;

/**
 * The decoupled (Stage B) path of {@link SingleTreeNode}, on the deterministic
 * {@link SimultaneousLMRGame} fixture, where every node is a multi-actor node with nine joint
 * actions. SushiGo coverage of the same path is in {@link SushiGoDecoupledTests}.
 * <p>
 * The pinned summaries at the bottom are the decoupled counterpart of {@link StageAGoldenTests}:
 * they were captured when Stage B landed and any change to them means decoupled behaviour changed.
 * Re-baseline by running {@link #main} and pasting its output over the static block.
 */
public class StageBDecoupledTests {

    private static final IStateHeuristic TICK_HEURISTIC =
            (gs, playerId) -> (((gs.getGameTick() * 37 + playerId * 11) % 23) / 23.0) - 0.5;
    private static final IActionHeuristic NAME_HEURISTIC =
            (action, state, contextActions) -> (Math.abs(action.toString().hashCode()) % 11) / 11.0;

    /** The Stage A golden value for lmr.ucb - a decoupled=false search over the simultaneous fixture must reproduce it. */
    private static final String STAGE_A_LMR_UCB =
            "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=99208fada037db8e|rnd=-3582349705094124591";

    private static MCTSParams baseParams() {
        MCTSParams p = new MCTSParams();
        p.setRandomSeed(9332);
        p.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        p.budget = 200;
        p.maxTreeDepth = 10;
        p.rolloutLength = 10;
        p.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        p.information = MCTSEnums.Information.Information_Set;
        p.treePolicy = MCTSEnums.TreePolicy.UCB;
        p.selectionPolicy = MCTSEnums.SelectionPolicy.SIMPLE;
        p.K = 1.0;
        p.decoupled = true;
        p.heuristic = TICK_HEURISTIC;
        return p;
    }

    private static SingleTreeNode rootFor(Consumer<MCTSParams> tweak, Random rnd) {
        MCTSParams params = baseParams();
        tweak.accept(params);
        LMRForwardModel fm = new LMRForwardModel();
        SimultaneousLMRGame game = new SimultaneousLMRGame(new LMTParameters(302));
        fm.setup(game);
        TestMCTSPlayer player = new TestMCTSPlayer(params);
        player.setForwardModel(fm);
        return SingleTreeNode.createRootNode(player, game, rnd, SingleTreeNode::new);
    }

    private static SingleTreeNode search(Consumer<MCTSParams> tweak, Random rnd) {
        SingleTreeNode root = rootFor(tweak, rnd);
        root.mctsSearch(0);
        return root;
    }

    private static SingleTreeNode search(Consumer<MCTSParams> tweak) {
        return search(tweak, new Random(303897));
    }

    private static String summary(Consumer<MCTSParams> tweak) {
        Random rnd = new Random(303897);
        SingleTreeNode root = search(tweak, rnd);
        return StageAGoldenTests.summary(root, rnd, true);
    }

    private static SimultaneousAction joint(String a0, String a1) {
        Map<Integer, AbstractAction> m = new LinkedHashMap<>();
        m.put(0, new LMRAction(a0));
        m.put(1, new LMRAction(a1));
        return new SimultaneousAction(m);
    }

    private static SingleTreeNode firstChild(SingleTreeNode node) {
        for (SingleTreeNode[] arr : node.children.values())
            for (SingleTreeNode c : arr)
                if (c != null) return c;
        throw new AssertionError("no child");
    }

    private static void assertJointChildKeys(SingleTreeNode root) {
        for (SingleTreeNode node : root.allNodesInTree()) {
            for (Map.Entry<AbstractAction, SingleTreeNode[]> e : node.children.entrySet()) {
                assertTrue("child key is not a joint action: " + e.getKey(), e.getKey() instanceof SimultaneousAction);
                assertEquals(Set.of(0, 1), ((SimultaneousAction) e.getKey()).getPlayerActions().keySet());
                assertNotNull("placeholder child survived at a multi-actor node", e.getValue());
                assertEquals("a joint transition has exactly one child", 1,
                        Arrays.stream(e.getValue()).filter(Objects::nonNull).count());
            }
        }
    }

    private static void assertVisitAccounting(SingleTreeNode root) {
        for (SingleTreeNode node : root.allNodesInTree()) {
            for (int p : node.getActingPlayers()) {
                int sum = node.getActionValues(p).values().stream().mapToInt(s -> s.nVisits).sum();
                assertEquals("P" + p + " visits at depth " + node.depth, node.getVisits(), sum);
                for (ActionStats s : node.getActionValues(p).values())
                    assertTrue(s.validVisits >= s.nVisits);
            }
        }
    }

    // ------------------------------------------------------------------ structure

    @Test
    public void rootIsMultiActorWithItsOwnDecisionOwner() {
        SingleTreeNode root = search(p -> {
        });
        assertTrue(root.isMultiActor());
        assertEquals(List.of(0, 1), root.getActingPlayers());
        // the root knows whose search this is, and a multi-actor child carries the same decision owner
        assertEquals(0, root.getActor());
        assertEquals(0, firstChild(root).getActor());
        assertTrue(firstChild(root).isMultiActor());
    }

    @Test
    public void jointChildKeys() {
        SingleTreeNode root = search(p -> {
        });
        assertJointChildKeys(root);
        assertTrue(root.children.size() > 1);
        assertTrue(root.children.size() <= 9);
    }

    @Test
    public void visitAccounting() {
        assertVisitAccounting(search(p -> {
        }));
    }

    @Test
    public void statsTablesAreIndependent() {
        SingleTreeNode root = search(p -> {
        });
        assertEquals(Set.of(0, 1), root.statsByPlayer.keySet());
        assertNotSame(root.getActionValues(0), root.getActionValues(1));
        assertEquals(3, root.getActionValues(0).size());
        assertEquals(3, root.getActionValues(1).size());
        // each player's table sums to the node's visits on its own
        assertEquals(200, root.getActionValues(0).values().stream().mapToInt(s -> s.nVisits).sum());
        assertEquals(200, root.getActionValues(1).values().stream().mapToInt(s -> s.nVisits).sum());
    }

    @Test
    public void backupCreditsOwnComponentOnly() {
        SingleTreeNode root = rootFor(p -> {
        }, new Random(1));
        root.actionsInTree = List.of(new Pair<>(-1, joint("Left", "Right")));
        root.currentNodeTrajectory = List.of(root);
        root.backUp(new double[]{5.0, 1.0});

        assertEquals(1, root.nVisits);
        ActionStats left0 = root.getActionStats(0, new LMRAction("Left"));
        assertEquals(1, left0.nVisits);
        assertEquals(5.0, left0.totValue[0], 1e-9);
        assertEquals(1.0, left0.totValue[1], 1e-9);
        ActionStats right1 = root.getActionStats(1, new LMRAction("Right"));
        assertEquals(1, right1.nVisits);
        assertEquals(5.0, right1.totValue[0], 1e-9);
        assertEquals(1.0, right1.totValue[1], 1e-9);
        // the other player's identically-named action is untouched
        assertEquals(0, root.getActionStats(0, new LMRAction("Right")).nVisits);
        assertEquals(0, root.getActionStats(1, new LMRAction("Left")).nVisits);
        assertEquals(0, root.getActionStats(0, new LMRAction("Middle")).nVisits);
    }

    @Test
    public void jointActionAtSingleActorNodeIsRejected() {
        SingleTreeNode root = rootFor(p -> p.decoupled = false, new Random(1));
        root.actionsInTree = List.of(new Pair<>(-1, joint("Left", "Right")));
        root.currentNodeTrajectory = List.of(root);
        assertThrows(AssertionError.class, () -> root.backUp(new double[]{5.0, 1.0}));
    }

    @Test
    public void noArgAccessorsReadRootPlayerTableAtMultiActorChild() {
        SingleTreeNode child = firstChild(search(p -> {
        }));
        assertEquals(0, child.getActor());
        assertSame(child.getActionValues(0), child.getActionValues());
        assertSame(child.getActionsFromOpenLoopState(0), child.getActionsFromOpenLoopState());
        // the named form works for either acting player
        assertEquals(3, child.getActionValues(0).size());
        assertEquals(3, child.getActionValues(1).size());
    }

    // ------------------------------------------------------------------ the sequential contract

    @Test
    public void decoupledOffIsExactlySequentialSearch() {
        // the same fixture, searched sequentially, is byte-for-byte the Stage A lmr.ucb tree
        assertEquals(STAGE_A_LMR_UCB, summary(p -> p.decoupled = false));
        SingleTreeNode root = search(p -> p.decoupled = false);
        assertFalse(root.isMultiActor());
        for (AbstractAction key : root.children.keySet())
            assertTrue(key instanceof LMRAction);
    }

    // ------------------------------------------------------------------ variants admitted to Stage B

    @Test
    public void closedLoopJointTrajectory() {
        SingleTreeNode root = search(p -> {
            p.information = MCTSEnums.Information.Closed_Loop;
            p.discardStateAfterEachIteration = false;
        });
        assertJointChildKeys(root);
        assertVisitAccounting(root);
        // the last iteration's record: one joint entry per node visited, index-aligned with the trajectory
        assertEquals(root.currentNodeTrajectory.size(), root.actionsInTree.size());
        assertFalse(root.actionsInTree.isEmpty());
        for (Pair<Integer, AbstractAction> pair : root.actionsInTree) {
            assertEquals(-1, (int) pair.a);
            assertTrue(pair.b instanceof SimultaneousAction);
        }
        // closed loop never copies the state on the way down, only at expansion
        assertEquals(200, root.copyCount);
    }

    @Test
    public void regretMatchingPerPlayer() {
        SingleTreeNode root = search(p -> p.treePolicy = MCTSEnums.TreePolicy.RegretMatching);
        // 3 actions each, so the average policy is refreshed every max(3, 10) = 10 node visits: 20 times in 200
        for (int p : List.of(0, 1)) {
            Map<AbstractAction, Double> average = root.statsByPlayer.get(p).regretMatchingAverage;
            assertEquals(3, average.size());
            double total = average.values().stream().mapToDouble(d -> d).sum();
            assertEquals("P" + p + " average policy mass", 20.0, total, 1e-9);
        }
        root.initialiseRootMetrics();
        assertTrue(root.statsByPlayer.get(0).regretMatchingAverage.isEmpty());
        assertTrue(root.statsByPlayer.get(1).regretMatchingAverage.isEmpty());
    }

    @Test
    public void mastExpandsJointActionsPerPlayer() {
        SingleTreeNode root = search(p -> {
            p.MAST = MCTSEnums.MASTType.Both;
            p.useMAST = true;
        });
        // Rollouts in the LMR fixture are always player 0's (the forward model never ends a turn),
        // so any MAST statistics for player 1 can only have come from expanding the joint actions
        // taken in the tree.
        for (int p : List.of(0, 1)) {
            Map<Object, Pair<Integer, Double>> stats = root.MASTStatistics.get(p);
            assertFalse("no MAST statistics for P" + p, stats.isEmpty());
            for (Object key : stats.keySet())
                assertTrue("MAST keyed by a joint action: " + key, key instanceof LMRAction);
        }
        int treeVisitsP1 = root.MASTStatistics.get(1).values().stream().mapToInt(s -> s.a).sum();
        assertTrue(treeVisitsP1 >= 200);
    }

    @Test
    public void nonMonteCarloBackupIsMonteCarloAtMultiActorNodes() {
        // documented limitation (DUCT Readme, §6): every node here is multi-actor, so the Lambda
        // and MaxMC tails never run and the tree is identical to the plain Monte Carlo one
        String plain = summary(p -> {
        });
        assertEquals(plain, summary(p -> {
            p.backupPolicy = MCTSEnums.BackupPolicy.Lambda;
            p.backupLambda = 0.8;
        }));
        assertEquals(plain, summary(p -> {
            p.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
            p.maxBackupThreshold = 20;
        }));
    }

    @Test
    public void toStringAndTreeStatisticsSurvive() {
        SingleTreeNode root = search(p -> {
        });
        String s = root.toString();
        assertTrue(s.contains("Player 0:"));
        assertTrue(s.contains("Player 1:"));
        assertFalse(firstChild(root).toString().isEmpty());
        TreeStatistics stats = new TreeStatistics(root);
        assertEquals(root.allNodesInTree().size(), stats.totalNodes);
        // six actions per node: three per player
        assertEquals(6.0, stats.meanActionsAtNode, 1e-9);
    }

    // ------------------------------------------------------------------ pinned decoupled behaviour

    private static Map<String, Consumer<MCTSParams>> scenarios() {
        Map<String, Consumer<MCTSParams>> s = new LinkedHashMap<>();
        s.put("ucb", p -> {
        });
        s.put("ucbTuned", p -> p.treePolicy = MCTSEnums.TreePolicy.UCB_Tuned);
        s.put("exp3", p -> p.treePolicy = MCTSEnums.TreePolicy.EXP3);
        s.put("regretMatching", p -> p.treePolicy = MCTSEnums.TreePolicy.RegretMatching);
        s.put("greedy", p -> p.treePolicy = MCTSEnums.TreePolicy.Greedy);
        s.put("closedLoop", p -> {
            p.information = MCTSEnums.Information.Closed_Loop;
            p.discardStateAfterEachIteration = false;
        });
        s.put("mast", p -> {
            p.MAST = MCTSEnums.MASTType.Both;
            p.useMAST = true;
            p.MASTGamma = 0.5;
        });
        s.put("progressiveWidening", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.progressiveWideningConstant = 1.5;
            p.progressiveWideningExponent = 0.5;
        });
        s.put("pUCT", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.pUCTTemperature = 1.0;
        });
        s.put("initialiseVisits", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.initialiseVisits = 3;
        });
        s.put("paranoid", p -> p.paranoid = true);
        return s;
    }

    private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        // GENERATED by main() - see the class comment before editing by hand
        EXPECTED.put("ucb",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2549|copies=201|rollout=2000|digest=7566360b81873a36|rnd=2228064810716131872");
        EXPECTED.put("ucbTuned",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2586|copies=201|rollout=2000|digest=eb8fe0bc55484f3e|rnd=8663219514887876724");
        EXPECTED.put("exp3",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2538|copies=201|rollout=2000|digest=972e93e16a2d6d89|rnd=745567633726733450");
        EXPECTED.put("regretMatching",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2644|copies=201|rollout=2000|digest=832537166a1cf220|rnd=3032803597294232314");
        EXPECTED.put("greedy",
                "type=SingleTreeNode|roots=1|visits=200|nodes=196|fm=3010|copies=201|rollout=2000|digest=3c0311683c599bd2|rnd=-7828797891413438531");
        EXPECTED.put("closedLoop",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2000|copies=200|rollout=2000|digest=59e78edbd19a6e04|rnd=-8021293852782020870");
        EXPECTED.put("mast",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2549|copies=201|rollout=2000|digest=7566360b81873a36|rnd=2228064810716131872");
        EXPECTED.put("progressiveWidening",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2544|copies=201|rollout=2000|digest=84408a700ecc6f82|rnd=-8907240076208972560");
        EXPECTED.put("pUCT",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2633|copies=201|rollout=2000|digest=87ab40ecc2d1379e|rnd=-786632837516347028");
        EXPECTED.put("initialiseVisits",
                "type=SingleTreeNode|roots=1|visits=209|nodes=201|fm=2534|copies=201|rollout=2000|digest=3a40422f380969e0|rnd=-934894581532007506");
        EXPECTED.put("paranoid",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2521|copies=201|rollout=2000|digest=ba7701a8e0f6b0db|rnd=-3001675053116265653");
    }

    @Test
    public void decoupledTreesUnchanged() {
        for (Map.Entry<String, Consumer<MCTSParams>> e : scenarios().entrySet()) {
            String actual = summary(e.getValue());
            String expected = EXPECTED.get(e.getKey());
            if (expected == null)
                throw new AssertionError("No golden value recorded for scenario '" + e.getKey() + "': " + actual);
            assertEquals("Decoupled behaviour changed for scenario '" + e.getKey() + "'", expected, actual);
        }
    }

    public static void main(String[] args) {
        for (Map.Entry<String, Consumer<MCTSParams>> e : scenarios().entrySet())
            System.out.printf("        EXPECTED.put(\"%s\",%n                \"%s\");%n", e.getKey(), summary(e.getValue()));
    }
}

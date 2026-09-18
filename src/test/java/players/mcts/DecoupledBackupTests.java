package players.mcts;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.SimultaneousAction;
import org.junit.Test;
import utilities.Pair;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * The Lambda, MaxLambda and MaxMC backup policies at a multi-actor node of a decoupled search,
 * on hand-built trees in the style of {@link BackupTests}. The rule under test (see
 * {@code MaxBackupAtSimultaneousNodes.md}):
 * <ul>
 *     <li>each acting player's entry of the value handed to the parent is mixed with that player's
 *     own counterfactual, read from that player's own table;</li>
 *     <li>the entry of a player who does not act at the node is left as it arrived;</li>
 *     <li>under paranoid only the paranoid player's entry is mixed, every other entry is its
 *     negation, and nothing is mixed at a node where the paranoid player does not act.</li>
 * </ul>
 * Statistics are laid down with {@code backUpSingleNode} under MonteCarlo, which bypasses the
 * paranoid transform in {@code backUp}; the policy is then switched and one backup made.
 */
public class DecoupledBackupTests {

    private static final double EPS = 1e-9;
    private static final LMRAction LEFT = new LMRAction("Left");
    private static final LMRAction MIDDLE = new LMRAction("Middle");
    private static final LMRAction RIGHT = new LMRAction("Right");

    private MCTSParams params;
    private SingleTreeNode root;
    private AbstractGameState game;

    // ------------------------------------------------------------------ fixture

    private void setup(AbstractGameState g) {
        params = new MCTSParams();
        params.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OneTree;
        params.information = MCTSEnums.Information.Information_Set;
        params.decoupled = true;
        params.backupPolicy = MCTSEnums.BackupPolicy.MonteCarlo;
        LMRForwardModel fm = new LMRForwardModel();
        fm.setup(g);
        TestMCTSPlayer player = new TestMCTSPlayer(params);
        player.setForwardModel(fm);
        game = g;
        root = SingleTreeNode.createRootNode(player, g, new Random(303897), SingleTreeNode::new);
    }

    private void setupTwoPlayer() {
        setup(new SimultaneousLMRGame(new LMTParameters(302)));
    }

    private void setupThreePlayer() {
        setup(new ThreePlayerSimultaneousLMRGame(new LMTParameters(302)));
    }

    /** A joint action of players 0 and 1. */
    private static SimultaneousAction joint(LMRAction p0, LMRAction p1) {
        Map<Integer, AbstractAction> m = new LinkedHashMap<>();
        m.put(0, p0);
        m.put(1, p1);
        return new SimultaneousAction(m);
    }

    private static void repeat(SingleTreeNode node, SimultaneousAction j, int n, double... result) {
        for (int i = 0; i < n; i++)
            node.backUpSingleNode(j, result.clone());
    }

    /**
     * The 50-visit fixture. Marginals: P0 Left 0.5, Middle 0.6 (best), Right 0.2; P1 Left 0.2,
     * Right 0.7, Middle 0.8 (best). Node totValue [24, 26].
     * {@code extra} is appended to every result vector, for the three-player fixture.
     */
    private static void layDownFixture(SingleTreeNode node, double... extra) {
        repeat(node, joint(LEFT, LEFT), 10, with(extra, 1.0, 0.0));
        repeat(node, joint(LEFT, RIGHT), 10, with(extra, 0.0, 1.0));
        repeat(node, joint(MIDDLE, LEFT), 10, with(extra, 0.6, 0.4));
        repeat(node, joint(MIDDLE, RIGHT), 10, with(extra, 0.6, 0.4));
        repeat(node, joint(RIGHT, MIDDLE), 10, with(extra, 0.2, 0.8));
    }

    /** The paranoid variant of the fixture: every P1 entry is the negation of P0's. */
    private static void layDownZeroSumFixture(SingleTreeNode node) {
        repeat(node, joint(LEFT, LEFT), 10, 1.0, -1.0);
        repeat(node, joint(LEFT, RIGHT), 10, 0.0, 0.0);
        repeat(node, joint(MIDDLE, LEFT), 10, 0.6, -0.6);
        repeat(node, joint(MIDDLE, RIGHT), 10, 0.6, -0.6);
        repeat(node, joint(RIGHT, MIDDLE), 10, 0.2, -0.2);
    }

    private static double[] with(double[] extra, double... base) {
        double[] r = new double[base.length + extra.length];
        System.arraycopy(base, 0, r, 0, base.length);
        System.arraycopy(extra, 0, r, base.length, extra.length);
        return r;
    }

    private static void assertStats(SingleTreeNode node, int player, LMRAction action, int visits, double... totValue) {
        ActionStats s = node.getActionStats(player, action);
        assertEquals("P" + player + " " + action + " visits", visits, s.nVisits);
        for (int i = 0; i < totValue.length; i++)
            assertEquals("P" + player + " " + action + " totValue[" + i + "]", totValue[i], s.totValue[i], EPS);
    }

    private static void assertVector(String what, double[] expected, double[] actual) {
        assertEquals(what + " length", expected.length, actual.length);
        for (int i = 0; i < expected.length; i++)
            assertEquals(what + "[" + i + "]", expected[i], actual[i], EPS);
    }

    /** The raw update that every case leaves in the node's own tables after backing up (Left, Right) with [0.4, 0.4]. */
    private void assertRawUpdateOfLeftRight(SingleTreeNode node) {
        assertEquals(51, node.getVisits());
        assertStats(node, 0, LEFT, 21, 10.4, 10.4);
        assertStats(node, 0, MIDDLE, 20, 12.0, 8.0);
        assertStats(node, 1, RIGHT, 21, 6.4, 14.4);
        assertStats(node, 1, MIDDLE, 10, 2.0, 8.0);
        assertEquals(24.4 / 51, node.nodeValue(0), EPS);
        assertEquals(26.4 / 51, node.nodeValue(1), EPS);
    }

    // ------------------------------------------------------------------ the fixture itself

    @Test
    public void fixtureMarginalsAreAsDocumented() {
        setupTwoPlayer();
        layDownFixture(root);
        assertEquals(50, root.getVisits());
        assertStats(root, 0, LEFT, 20, 10, 10);
        assertStats(root, 0, MIDDLE, 20, 12, 8);
        assertStats(root, 0, RIGHT, 10, 2, 8);
        assertStats(root, 1, LEFT, 20, 16, 4);
        assertStats(root, 1, RIGHT, 20, 6, 14);
        assertStats(root, 1, MIDDLE, 10, 2, 8);
        assertEquals(24.0 / 50, root.nodeValue(0), EPS);
        assertEquals(26.0 / 50, root.nodeValue(1), EPS);
        assertEquals(MIDDLE, root.bestAction(root.getActionsFromOpenLoopState(0)));
    }

    // ------------------------------------------------------------------ case A: neither player took their best

    @Test
    public void maxMCMixesEachActingPlayerWithTheirOwnCounterfactual() {
        setupTwoPlayer();
        layDownFixture(root);
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 30;
        double[] out = root.backUpSingleNode(joint(LEFT, RIGHT), new double[]{0.4, 0.4});
        // w = (51 - 30) / 51; P0's best is Middle (0.6), P1's best is Middle (0.8)
        assertVector("propagated", new double[]{24.6 / 51, 28.8 / 51}, out);
        assertRawUpdateOfLeftRight(root);
    }

    @Test
    public void maxMCBelowThresholdIsMonteCarlo() {
        setupTwoPlayer();
        layDownFixture(root);
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 100;
        double[] out = root.backUpSingleNode(joint(LEFT, RIGHT), new double[]{0.4, 0.4});
        assertVector("propagated", new double[]{0.4, 0.4}, out);
        assertRawUpdateOfLeftRight(root);
    }

    @Test
    public void maxLambdaMixesEachActingPlayerWithTheirOwnBest() {
        setupTwoPlayer();
        layDownFixture(root);
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxLambda;
        params.backupLambda = 0.75;
        double[] out = root.backUpSingleNode(joint(LEFT, RIGHT), new double[]{0.4, 0.4});
        assertVector("propagated", new double[]{0.75 * 0.4 + 0.25 * 0.6, 0.75 * 0.4 + 0.25 * 0.8}, out);
        assertRawUpdateOfLeftRight(root);
    }

    @Test
    public void lambdaMixesEachActingPlayerWithTheirOwnTakenAction() {
        setupTwoPlayer();
        layDownFixture(root);
        params.backupPolicy = MCTSEnums.BackupPolicy.Lambda;
        params.backupLambda = 0.75;
        double[] out = root.backUpSingleNode(joint(LEFT, RIGHT), new double[]{0.4, 0.4});
        // on-policy: the mean of the action actually taken, after this visit is included
        assertVector("propagated", new double[]{0.75 * 0.4 + 0.25 * 10.4 / 21, 0.75 * 0.4 + 0.25 * 14.4 / 21}, out);
        assertRawUpdateOfLeftRight(root);
    }

    // ------------------------------------------------------------------ case B: one player took their best

    @Test
    public void maxMCLeavesAPlayerWhoTookTheirBestActionAlone() {
        setupTwoPlayer();
        layDownFixture(root);
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 30;
        double[] out = root.backUpSingleNode(joint(MIDDLE, RIGHT), new double[]{0.4, 0.4});
        // P0 took Middle, still its best at 12.4/21: untouched. P1 took Right, best Middle: mixed.
        assertVector("propagated", new double[]{0.4, 28.8 / 51}, out);
        assertStats(root, 0, MIDDLE, 21, 12.4, 8.4);
        assertStats(root, 1, RIGHT, 21, 6.4, 14.4);
    }

    @Test
    public void maxLambdaMixesAPlayerWhoTookTheirBestActionToo() {
        setupTwoPlayer();
        layDownFixture(root);
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxLambda;
        params.backupLambda = 0.75;
        double[] out = root.backUpSingleNode(joint(MIDDLE, RIGHT), new double[]{0.4, 0.4});
        assertVector("propagated", new double[]{0.75 * 0.4 + 0.25 * 12.4 / 21, 0.75 * 0.4 + 0.25 * 0.8}, out);
    }

    // ------------------------------------------------------------------ case C: the parent receives the mixed value

    @Test
    public void mixedValueIsWhatTheParentReceives() {
        setupTwoPlayer();
        // root prior: P0 best Right (0.9), P1 best Middle (0.5)
        repeat(root, joint(LEFT, LEFT), 20, 0.3, 0.3);
        repeat(root, joint(RIGHT, RIGHT), 20, 0.9, 0.1);
        repeat(root, joint(MIDDLE, MIDDLE), 10, 0.5, 0.5);
        assertEquals(50, root.getVisits());
        SimultaneousAction leftRight = joint(LEFT, RIGHT);
        SingleTreeNode child = root.expandNode(leftRight, game);
        assertSame(child, root.children.get(leftRight)[game.getNPlayers()]);
        assertTrue(child.isMultiActor());
        layDownFixture(child);

        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 30;
        root.currentNodeTrajectory = List.of(root, child);
        root.actionsInTree = List.of(new Pair<>(-1, leftRight), new Pair<>(-1, leftRight));
        root.actionsInRollout = new ArrayList<>();
        root.backUp(new double[]{0.4, 0.4});

        // the child saw the raw leaf result (case A) and handed up [24.6/51, 28.8/51]
        assertRawUpdateOfLeftRight(child);
        // the root's tables received that mixed vector, not the leaf result
        assertEquals(51, root.getVisits());
        assertStats(root, 0, LEFT, 21, 6 + 24.6 / 51, 6 + 28.8 / 51);
        assertStats(root, 1, RIGHT, 21, 18 + 24.6 / 51, 2 + 28.8 / 51);
        assertEquals((29 + 24.6 / 51) / 51, root.nodeValue(0), EPS);
        assertEquals((13 + 28.8 / 51) / 51, root.nodeValue(1), EPS);
    }

    // ------------------------------------------------------------------ case D: a player who does not act here

    @Test
    public void nonActingPlayerEntryPassesThroughUnchanged() {
        setupThreePlayer();
        assertEquals(List.of(0, 1), root.getActingPlayers());
        layDownFixture(root, 0.9);
        assertFalse(root.statsByPlayer.containsKey(2));
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 30;
        double[] out = root.backUpSingleNode(joint(LEFT, RIGHT), new double[]{0.4, 0.4, 0.1});
        // players 0 and 1 as in case A; player 2's entry is the incoming 0.1, not 0.9 and not a mix
        assertVector("propagated", new double[]{24.6 / 51, 28.8 / 51, 0.1}, out);
        assertFalse("a table was created for a player who does not act here", root.statsByPlayer.containsKey(2));
        assertEquals((45 + 0.1) / 51, root.nodeValue(2), EPS);
    }

    // ------------------------------------------------------------------ case E: paranoid

    @Test
    public void paranoidMixesOnlyTheParanoidPlayerAndMirrorsIt() {
        setupTwoPlayer();
        layDownZeroSumFixture(root);
        // P1's own means: Left -0.8, Right -0.3, Middle -0.2 (its best); P0's as in the main fixture
        assertStats(root, 1, MIDDLE, 10, 2, -2);
        assertStats(root, 1, RIGHT, 20, 6, -6);
        params.paranoid = true;
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 30;
        // the vector as backUp would hand it over after the paranoid transform of [0.4, ...]
        double[] out = root.backUpSingleNode(joint(LEFT, RIGHT), new double[]{0.4, -0.4});
        // P0 (the paranoid player) took Left, best Middle at 0.6: mixed. P1 is never consulted.
        assertVector("propagated", new double[]{24.6 / 51, -24.6 / 51}, out);
        // the rejected per-player rule would have mixed P1 with its own best (Middle, -0.2)
        assertNotEquals(-16.2 / 51, out[1], 1e-6);
        assertStats(root, 0, LEFT, 21, 10.4, -10.4);
        assertStats(root, 1, RIGHT, 21, 6.4, -6.4);
    }

    @Test
    public void paranoidThroughBackUpReachesTheParentMirrored() {
        setupTwoPlayer();
        repeat(root, joint(LEFT, LEFT), 20, 0.3, -0.3);
        repeat(root, joint(RIGHT, RIGHT), 20, 0.9, -0.9);
        repeat(root, joint(MIDDLE, MIDDLE), 10, 0.5, -0.5);
        SimultaneousAction leftRight = joint(LEFT, RIGHT);
        SingleTreeNode child = root.expandNode(leftRight, game);
        layDownZeroSumFixture(child);

        params.paranoid = true;
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 30;
        root.currentNodeTrajectory = List.of(root, child);
        root.actionsInTree = List.of(new Pair<>(-1, leftRight), new Pair<>(-1, leftRight));
        root.actionsInRollout = new ArrayList<>();
        // backUp's paranoid transform turns this into [0.4, -0.4] before the first backup
        root.backUp(new double[]{0.4, 0.0});

        assertStats(child, 0, LEFT, 21, 10.4, -10.4);
        assertStats(child, 1, RIGHT, 21, 6.4, -6.4);
        // the root receives [24.6/51, -24.6/51]
        assertStats(root, 0, LEFT, 21, 6 + 24.6 / 51, -6 - 24.6 / 51);
        assertStats(root, 1, RIGHT, 21, 18 + 24.6 / 51, -18 - 24.6 / 51);
    }

    @Test
    public void paranoidPlayerWhoDoesNotActHereLeavesTheResultAlone() {
        setupThreePlayer();
        layDownFixture(root, 0.9);
        params.paranoid = true;
        root.paranoidPlayer = 2;
        params.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
        params.maxBackupThreshold = 30;
        // the vector as backUp would hand it over after the paranoid transform for player 2
        double[] out = root.backUpSingleNode(joint(LEFT, RIGHT), new double[]{-0.1, -0.1, 0.1});
        assertVector("propagated", new double[]{-0.1, -0.1, 0.1}, out);
        assertStats(root, 0, LEFT, 21, 9.9, 9.9, 18.1);
    }
}

package players.mcts;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IActionHeuristic;
import core.interfaces.IStateHeuristic;
import games.GameType;
import games.dominion.DominionForwardModel;
import games.dominion.DominionGameState;
import games.dominion.DominionParameters;
import org.junit.Test;
import players.PlayerConstants;
import players.simple.RandomPlayer;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;

/**
 * Characterisation ("golden") tests pinning the exact behaviour of {@link SingleTreeNode}.
 * <p>
 * These exist to support the Stage A refactor of the MCTS package (moving the five per-acting-player
 * fields into a PlayerDecisionStats container), which is required to be a *provable no-op*. The
 * values below were captured against the tree as it stood before that refactor began; any change to
 * them means behaviour changed.
 * <p>
 * Two independent mechanisms are used per scenario:
 * <ol>
 *     <li><b>An RNG canary.</b> {@code rnd} is a single {@link Random} shared by every node in the
 *     tree, so a draw added, removed or reordered anywhere in the search shifts the next one. The
 *     visit / forward-model / state-copy / rollout counters alongside it also catch divergence in
 *     the separately-seeded rollout and opponent-model policies, which the canary alone would not
 *     see.</li>
 *     <li><b>A tree digest.</b> A deterministic walk of every node, emitting visits and the full
 *     action statistics table <i>in map iteration order</i> - the order itself is pinned, because
 *     {@code nodeValue} sums over {@code values()} and so the floating-point summation order is
 *     observable in the search's decisions.</li>
 * </ol>
 * To re-baseline after a deliberate behaviour change, run {@link #main} and paste its output over
 * the block in the static initialiser.
 */
public class StageAGoldenTests {

    // ------------------------------------------------------------------ deterministic heuristics

    /**
     * LMRGame is stateless and scores every state 0, which would leave every backup policy and every
     * normalisation setting indistinguishable. This gives the search something to chew on that is
     * still perfectly reproducible: the length of the action history is a proxy for search depth.
     */
    private static final IStateHeuristic TICK_HEURISTIC =
            (gs, playerId) -> (((gs.getGameTick() * 37 + playerId * 11) % 23) / 23.0) - 0.5;

    /**
     * Several parameters (pUCT, progressive bias/widening, initialiseVisits) are silently switched
     * off in setActionsFromOpenLoopState when the action heuristic is nullReturn, so exercising them
     * at all requires a real one.
     */
    private static final IActionHeuristic NAME_HEURISTIC =
            (action, state, contextActions) -> (Math.abs(action.toString().hashCode()) % 11) / 11.0;

    // ------------------------------------------------------------------ scenarios

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
        return p;
    }

    /** Scenarios run on the LMR fixture, where the full tree digest is meaningful. */
    private static Map<String, Consumer<MCTSParams>> lmrScenarios() {
        Map<String, Consumer<MCTSParams>> s = new LinkedHashMap<>();
        s.put("ucb", p -> {
        });
        s.put("ucbTuned", p -> p.treePolicy = MCTSEnums.TreePolicy.UCB_Tuned);
        s.put("exp3", p -> p.treePolicy = MCTSEnums.TreePolicy.EXP3);
        s.put("regretMatching", p -> p.treePolicy = MCTSEnums.TreePolicy.RegretMatching);
        s.put("greedy", p -> p.treePolicy = MCTSEnums.TreePolicy.Greedy);
        s.put("normalised", p -> p.normaliseRewards = true);
        s.put("normalisedTuned", p -> {
            p.normaliseRewards = true;
            p.treePolicy = MCTSEnums.TreePolicy.UCB_Tuned;
        });
        s.put("backupLambda", p -> {
            p.backupPolicy = MCTSEnums.BackupPolicy.Lambda;
            p.backupLambda = 0.8;
        });
        s.put("backupMaxLambda", p -> {
            p.backupPolicy = MCTSEnums.BackupPolicy.MaxLambda;
            p.backupLambda = 0.8;
        });
        s.put("backupMaxMC", p -> {
            p.backupPolicy = MCTSEnums.BackupPolicy.MaxMC;
            p.maxBackupThreshold = 20;
        });
        s.put("progressiveWidening", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.progressiveWideningConstant = 1.5;
            p.progressiveWideningExponent = 0.5;
        });
        s.put("progressiveWideningTight", p -> {
            // constant/exponent chosen so the widened subset actually grows during the search
            // (1 action, then 2, then 3), which is what makes the backup sensitive to whether the
            // considered list is recomputed after the visit counts have been incremented.
            p.actionHeuristic = NAME_HEURISTIC;
            p.progressiveWideningConstant = 1.2;
            p.progressiveWideningExponent = 0.2;
        });
        s.put("progressiveBias", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.progressiveBias = 0.5;
        });
        s.put("initialiseVisits", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.initialiseVisits = 3;
        });
        s.put("pUCT", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.pUCTTemperature = 1.0;
        });
        s.put("moveOrdering", p -> {
            p.actionHeuristic = NAME_HEURISTIC;
            p.useActionHeuristicForMoveOrdering = true;
            p.actionHeuristicRecalculationThreshold = 1;
        });
        return s;
    }

    /** Scenarios run on a real game, covering the tree shapes LMR cannot produce. */
    private static Map<String, Consumer<MCTSParams>> dominionScenarios() {
        Map<String, Consumer<MCTSParams>> s = new LinkedHashMap<>();
        s.put("oneTree", p -> {
        });
        s.put("selfOnly", p -> p.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.SelfOnly);
        s.put("multiTree", p -> p.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.MultiTree);
        s.put("oma", p -> {
            p.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA;
            p.omaVisits = 10;
        });
        s.put("omaAll", p -> {
            p.opponentTreePolicy = MCTSEnums.OpponentTreePolicy.OMA_All;
            p.omaVisits = 10;
        });
        s.put("paranoid", p -> p.paranoid = true);
        s.put("closedLoop", p -> {
            p.information = MCTSEnums.Information.Closed_Loop;
            p.discardStateAfterEachIteration = false;
            p.maxTreeDepth = 3;
        });
        s.put("informationSet", p -> p.information = MCTSEnums.Information.Information_Set);
        s.put("mast", p -> {
            p.MAST = MCTSEnums.MASTType.Both;
            p.MASTGamma = 0.5;
            p.useMAST = true;
        });
        s.put("forest", p -> p.numDeterminizations = 3);
        return s;
    }

    // ------------------------------------------------------------------ drivers

    /**
     * A search on the stateless two-player LMR fixture. Chosen as the digest substrate because
     * LMRAction.hashCode() is name.hashCode(), and String hashing is specified by the JLS, so the
     * HashMap iteration order this pins is stable across JVM runs. Real-game actions frequently fold
     * an enum into their hashCode, and Enum.hashCode() is identity-based and so varies per run.
     */
    private static String lmrSearch(Consumer<MCTSParams> tweak) {
        MCTSParams params = baseParams();
        params.heuristic = TICK_HEURISTIC;
        tweak.accept(params);
        LMRForwardModel fm = new LMRForwardModel();
        LMRGame game = new LMRGame(new LMTParameters(302));
        fm.setup(game);
        TestMCTSPlayer player = new TestMCTSPlayer(params);
        player.setForwardModel(fm);
        Random rnd = new Random(303897);
        SingleTreeNode root = SingleTreeNode.createRootNode(player, game, rnd, SingleTreeNode::new);
        root.mctsSearch(0);
        return summary(root, rnd, true);
    }

    /**
     * Real MCTS decisions in a three-player game of Dominion, driven the way MCTSNodesAndVisitsTests
     * does. The tree digest is not pinned here (see above); the counters and the RNG canary are.
     */
    private static String dominionSearch(Consumer<MCTSParams> tweak, int decisions) {
        MCTSParams params = baseParams();
        // Information_Set is deliberately NOT the default here: Dominion.redeterminise iterates
        // enum-keyed collections, whose iteration order follows Enum.hashCode() - an identity hash that
        // varies per JVM run. The resulting searches are not reproducible and so cannot be pinned.
        // Information_Set is covered by the LMR scenarios above, which are reproducible.
        params.information = MCTSEnums.Information.Open_Loop;
        tweak.accept(params);
        TestMCTSPlayer mcts = new TestMCTSPlayer(params, null);
        mcts.rolloutTest = false;   // use the production node types, not the rollout-instrumented ones
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mcts);
        players.add(new RandomPlayer(new Random(3023)));
        players.add(new RandomPlayer(new Random(244)));
        DominionParameters dp = new DominionParameters();
        dp.setRandomSeed(330245);
        Game game = new Game(GameType.Dominion, players, new DominionForwardModel(),
                new DominionGameState(dp, players.size()));
        AbstractGameState state = game.getGameState();
        AbstractForwardModel fm = game.getForwardModel();

        int decisionsMade = 0;
        while (state.isNotTerminal()) {
            List<AbstractAction> available = fm.computeAvailableActions(state);
            int actor = state.getCurrentPlayer();
            AbstractAction chosen = players.get(actor)._getAction(state, available);
            if (actor == 0 && available.size() > 1) {
                decisionsMade++;
                if (decisionsMade == decisions) break;
            }
            fm.next(state, chosen);
        }
        return summary(mcts.getRoot(), mcts.getRoot().rnd, false);
    }

    // ------------------------------------------------------------------ summaries

    /**
     * ForestNode and MultiTreeNode do not search themselves; they own a set of sub-roots that do.
     * Returning those is what makes the counters meaningful for them (and is what turns the
     * ForestNode case from "did not throw" into a real assertion, since nothing else in the test
     * tree exercises numDeterminizations > 1 at all).
     */
    private static List<SingleTreeNode> searchRoots(SingleTreeNode root) {
        SingleTreeNode[] subRoots = null;
        if (root instanceof ForestNode) subRoots = ((ForestNode) root).roots;
        else if (root instanceof MultiTreeNode) subRoots = ((MultiTreeNode) root).roots;
        if (subRoots == null) return List.of(root);
        return Arrays.stream(subRoots).filter(Objects::nonNull).toList();
    }

    static String summary(SingleTreeNode root, Random rnd, boolean withDigest) {
        List<SingleTreeNode> roots = searchRoots(root);
        int visits = 0, fmCalls = 0, copies = 0, rolloutActions = 0, nodes = 0;
        StringBuilder dump = new StringBuilder();
        for (SingleTreeNode r : roots) {
            visits += r.getVisits();
            fmCalls += r.fmCallsCount;
            copies += r.copyCount;
            rolloutActions += r.rolloutActionsTaken;
            nodes += r.allNodesInTree().size();
            if (withDigest) dump.append(treeDump(r));
        }
        StringBuilder sb = new StringBuilder();
        sb.append("type=").append(root.getClass().getSimpleName())
                .append("|roots=").append(roots.size())
                .append("|visits=").append(visits)
                .append("|nodes=").append(nodes)
                .append("|fm=").append(fmCalls)
                .append("|copies=").append(copies)
                .append("|rollout=").append(rolloutActions);
        if (withDigest) sb.append("|digest=").append(sha256(dump.toString()));
        sb.append("|rnd=").append(rnd.nextLong());
        return sb.toString();
    }

    /**
     * Deterministic dump of a whole tree. allNodesInTree() is a BFS over `children`, which is a
     * LinkedHashMap, so node order is insertion order. Within a node the action statistics are
     * emitted in map iteration order deliberately - see the class comment.
     */
    static String treeDump(SingleTreeNode root) {
        StringBuilder sb = new StringBuilder();
        for (SingleTreeNode node : root.allNodesInTree()) {
            sb.append(node.depth).append(';')
                    .append(node.getActor()).append(';')
                    .append(node.getVisits()).append(';')
                    .append(node.terminalNode).append('\n');
            // One table per acting player. A sequential node has exactly one and is dumped exactly
            // as before (no player header), so the Stage A digests are unaffected; a multi-actor
            // node of a decoupled search gets one headed block per player.
            List<Integer> actors = node.isMultiActor() ? node.getActingPlayers() : List.of(node.getActor());
            for (int p : actors) {
                if (node.isMultiActor()) sb.append(" P").append(p).append('\n');
                for (Map.Entry<AbstractAction, ActionStats> e : node.getActionValues(p).entrySet()) {
                    ActionStats st = e.getValue();
                    sb.append("  ").append(e.getKey()).append(';')
                            .append(st.nVisits).append(';')
                            .append(st.validVisits);
                    for (int i = 0; i < st.totValue.length; i++)
                        sb.append(';').append(Double.doubleToLongBits(st.totValue[i]))
                                .append(':').append(Double.doubleToLongBits(st.squaredTotValue[i]));
                    sb.append('\n');
                }
            }
        }
        return sb.toString();
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 8; i++) sb.append(String.format("%02x", d[i]));
            return sb.toString();
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }


    // ------------------------------------------------------------------ expected values

    /**
     * Scenarios whose exact numbers cannot be pinned, because they are not reproducible between runs
     * at all. {@code AbstractGameState.redeterminisationRnd} is deliberately unseeded, and
     * every {@code AbstractGameState.copy()} reseeds that copy's RNG from it, so any game whose forward model
     * consumes {@code gs.rnd} - Dominion shuffles its deck - takes a different trajectory on every
     * run. That is a framework design decision (hidden information must not be predictable from the
     * game seed), not something this test should try to work around.
     * <p>
     * They are still worth running, checked structurally (node type, number of sub-roots, total
     * visits): enough to catch a refactor that drops statistics, mis-keys a table, or throws - and
     * for ForestNode, which nothing else in the test tree covers at all, enough to catch the fact
     * that it never calls instantiate(). The exact-value coverage they cannot give is supplied by
     * the LMR scenarios, which run Information_Set with full tree digests and are reproducible
     * precisely because LMRGame never consumes gs.rnd.
     */
    private static boolean structuralOnly(String name) {
        return name.startsWith("dominion.");
    }

    private static final Map<String, String> EXPECTED = new LinkedHashMap<>();

    static {
        // GENERATED by main() - see the class comment before editing by hand
        EXPECTED.put("lmr.ucb",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=99208fada037db8e|rnd=-3582349705094124591");
        EXPECTED.put("lmr.ucbTuned",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=5236790b6465bde2|rnd=-3582349705094124591");
        EXPECTED.put("lmr.exp3",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2827|copies=201|rollout=2000|digest=c7ff90d04d8e2bf2|rnd=8392300257802280572");
        EXPECTED.put("lmr.regretMatching",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2859|copies=201|rollout=2000|digest=dfdd5b8daca10400|rnd=4025718201467067252");
        EXPECTED.put("lmr.greedy",
                "type=SingleTreeNode|roots=1|visits=200|nodes=178|fm=3378|copies=201|rollout=2000|digest=caa2b4bdb1705aa4|rnd=-2865431006968055350");
        EXPECTED.put("lmr.normalised",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=99208fada037db8e|rnd=-3582349705094124591");
        EXPECTED.put("lmr.normalisedTuned",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=5236790b6465bde2|rnd=-3582349705094124591");
        EXPECTED.put("lmr.backupLambda",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=bbfe69150c33a9ca|rnd=-3582349705094124591");
        EXPECTED.put("lmr.backupMaxLambda",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=f81b08f927611369|rnd=-3582349705094124591");
        EXPECTED.put("lmr.backupMaxMC",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=ae656bccdc814390|rnd=-3582349705094124591");
        EXPECTED.put("lmr.progressiveWidening",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=f96b6b2aa83a0f4b|rnd=5149955473058966674");
        EXPECTED.put("lmr.progressiveWideningTight",
                "type=SingleTreeNode|roots=1|visits=200|nodes=139|fm=3450|copies=201|rollout=2000|digest=eaa173664a2914f7|rnd=-6993690106722028079");
        EXPECTED.put("lmr.progressiveBias",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=b3b36948bfe90bac|rnd=-3582349705094124591");
        EXPECTED.put("lmr.initialiseVisits",
                "type=SingleTreeNode|roots=1|visits=209|nodes=201|fm=2841|copies=201|rollout=2000|digest=cd3b0fd992e28982|rnd=-8545389348336959084");
        EXPECTED.put("lmr.pUCT",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=caf88d002eb206c1|rnd=-3582349705094124591");
        EXPECTED.put("lmr.moveOrdering",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2826|copies=201|rollout=2000|digest=99208fada037db8e|rnd=-3582349705094124591");
        EXPECTED.put("dominion.oneTree",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2747|copies=201|rollout=2000|rnd=-4001477402015030767");
        EXPECTED.put("dominion.selfOnly",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=4272|copies=201|rollout=2000|rnd=5313071357424819615");
        EXPECTED.put("dominion.multiTree",
                "type=MultiTreeNode|roots=3|visits=600|nodes=601|fm=0|copies=3|rollout=0|rnd=6087241440033907686");
        EXPECTED.put("dominion.oma",
                "type=OMATreeNode|roots=1|visits=200|nodes=201|fm=2747|copies=201|rollout=2000|rnd=-4001477402015030767");
        EXPECTED.put("dominion.omaAll",
                "type=OMATreeNode|roots=1|visits=200|nodes=201|fm=2726|copies=201|rollout=2000|rnd=-1753681135991247560");
        EXPECTED.put("dominion.paranoid",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2718|copies=201|rollout=2000|rnd=5580764563828737479");
        EXPECTED.put("dominion.closedLoop",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2000|copies=200|rollout=2000|rnd=-9040922895717772145");
        EXPECTED.put("dominion.informationSet",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2671|copies=201|rollout=2000|rnd=-1064778795024471970");
        EXPECTED.put("dominion.mast",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2747|copies=201|rollout=2000|rnd=-4001477402015030767");
        EXPECTED.put("dominion.forest",
                "type=ForestNode|roots=3|visits=600|nodes=603|fm=8104|copies=603|rollout=6000|rnd=8517022915171131060");
        EXPECTED.put("dominion.reuseTree",
                "type=SingleTreeNode|roots=1|visits=200|nodes=201|fm=2586|copies=201|rollout=2000|rnd=-5140670200817469623");
    }

    /** The leading fields of a summary that are reproducible even for the structural-only cases. */
    private static String structure(String summary) {
        String[] parts = summary.split("\\|");
        return parts[0] + "|" + parts[1] + "|" + parts[2];
    }

    private void check(String name, String actual) {
        String expected = EXPECTED.get(name);
        if (expected == null)
            throw new AssertionError("No golden value recorded for scenario '" + name + "': " + actual);
        if (structuralOnly(name))
            assertEquals("Structure changed for scenario '" + name + "'", structure(expected), structure(actual));
        else
            assertEquals("Behaviour changed for scenario '" + name + "'", expected, actual);
    }

    // ------------------------------------------------------------------ tests

    @Test
    public void lmrTreesUnchanged() {
        for (Map.Entry<String, Consumer<MCTSParams>> e : lmrScenarios().entrySet())
            check("lmr." + e.getKey(), lmrSearch(e.getValue()));
    }

    @Test
    public void dominionSearchesUnchanged() {
        for (Map.Entry<String, Consumer<MCTSParams>> e : dominionScenarios().entrySet())
            check("dominion." + e.getKey(), dominionSearch(e.getValue(), 1));
    }

    @Test
    public void treeReuseUnchanged() {
        check("dominion.reuseTree", dominionSearch(p -> p.reuseTree = true, 4));
    }

    // ------------------------------------------------------------------ re-baselining

    public static void main(String[] args) {
        for (Map.Entry<String, Consumer<MCTSParams>> e : lmrScenarios().entrySet())
            emit("lmr." + e.getKey(), lmrSearch(e.getValue()));
        for (Map.Entry<String, Consumer<MCTSParams>> e : dominionScenarios().entrySet())
            emit("dominion." + e.getKey(), dominionSearch(e.getValue(), 1));
        emit("dominion.reuseTree", dominionSearch(p -> p.reuseTree = true, 4));
    }

    private static void emit(String name, String value) {
        System.out.printf("        EXPECTED.put(\"%s\",%n                \"%s\");%n", name, value);
    }
}

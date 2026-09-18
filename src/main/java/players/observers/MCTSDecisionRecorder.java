package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.listeners.IGameListener;
import evaluation.metrics.Event;
import games.GameType;
import gui.StateRenderer;
import players.mcts.MCTSPlayer;
import players.mcts.MultiTreeNode;
import players.mcts.SingleTreeNode;
import utilities.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A passive {@link IGameListener} that records, for every real decision in a game, a complete picture
 * of what the (MCTS) agent was thinking. It does <b>not</b> change the action taken (unlike
 * {@link GameAdviser}); it merely observes.
 * <p>
 * For each {@code ACTION_CHOSEN} event (skipping forced single-action decisions) it produces, in the
 * same {@code <savedStatesDir>/<GameType>/G<GameID>/} directory the {@link Game} uses for saved states:
 * <ul>
 *     <li>{@code P<playerID>_Tick<tick>.json} - the full game state, written by the {@code Game} itself
 *         (we simply flag the chosen action with {@link AbstractAction#setSaveGame(boolean)});</li>
 *     <li>{@code P<playerID>_Tick<tick>.png}  - a render of the live state (only if the game has a GUI),
 *         annotated below with the top 5 actions the agent considered;</li>
 *     <li>{@code P<playerID>_Tick<tick>_actions.tsv} - a tab-delimited table of every action available at
 *         the MCTS root with its visit count, valid-visit count, whether it is currently valid, mean
 *         value and action-heuristic value (untried actions included with a visit count of zero);</li>
 *     <li>{@code P<playerID>_Tick<tick>.dot} - a graphviz digraph of the part of the search tree whose
 *         nodes have more than {@code visitThreshold} visits, plus, if graphviz {@code dot} is installed,
 *         a rendered {@code P<playerID>_Tick<tick>_tree.png}.</li>
 * </ul>
 * The last two are only produced when the acting player is an {@link MCTSPlayer}.
 */
public class MCTSDecisionRecorder implements IGameListener {

    protected Game game;
    protected final int visitThreshold;
    protected final boolean fullPanel;
    // null = not yet checked; cached after the first probe so we do not spawn a process per decision.
    private Boolean dotAvailable;

    public MCTSDecisionRecorder() {
        this(30, true);
    }

    public MCTSDecisionRecorder(int visitThreshold) {
        this(visitThreshold, true);
    }

    /**
     * @param visitThreshold only tree nodes with strictly more than this many visits appear in the graphviz output.
     * @param fullPanel       if true the PNG renders the whole game panel (board + info/history panel); otherwise the
     *                        board view only. Defaults to true here because we render the live state, which still
     *                        carries the history/info-panel data (unlike a state loaded back from JSON).
     */
    public MCTSDecisionRecorder(int visitThreshold, boolean fullPanel) {
        this.visitThreshold = visitThreshold;
        this.fullPanel = fullPanel;
    }

    @Override
    public void onEvent(Event event) {
        if (event.type != Event.GameEvent.ACTION_CHOSEN)
            return;
        if (event.action == null || event.actions == null || event.actions.size() <= 1)
            return; // skip forced (single-action) decisions - there is no real choice to record

        AbstractGameState state = event.state;
        int playerID = event.playerID;

        // Ask the Game to write the JSON state for us (it does this in the game loop, after this event)
        event.action.setSaveGame(true);

        GameType gameType = game.getGameType();
        String directory = String.format("%s%s%s%sG%d",
                game.getSavedStatesDirectory(), File.separator, gameType.name(), File.separator, state.getGameID());
        Utils.createDirectory(directory);
        String base = String.format("%s%sP%d_Tick%d", directory, File.separator, playerID, state.getGameTick());

        // Resolve the MCTS root(s) up front. The acting player's root drives the PNG annotation and the
        // actions TSV. The graphviz output covers every tree: for MultiTree MCTS there is a separate tree
        // per player, which we render in turn order (active player first, then the rest) so that the
        // rendered PNG lays the trees out one after another down the page.
        SingleTreeNode actingRoot = null;
        List<SingleTreeNode> graphRoots = new ArrayList<>();
        AbstractPlayer player = game.getPlayers().get(playerID);
        if (player instanceof MCTSPlayer mcts) {
            SingleTreeNode root = mcts.getRoot();
            if (root instanceof MultiTreeNode mt) {
                int n = state.getNPlayers();
                for (int i = 0; i < n; i++) {
                    SingleTreeNode playerRoot = mt.getRoot((playerID + i) % n);
                    if (playerRoot != null)
                        graphRoots.add(playerRoot);
                }
                actingRoot = mt.getRoot(playerID);
            } else if (root != null) {
                actingRoot = root;
                graphRoots.add(root);
            }
        }

        // 1. PNG of the state (only if this game has a GUI), annotated below with the top actions considered.
        if (gameType.getGuiManagerClass() != null) {
            try {
                BufferedImage img = StateRenderer.renderToImage(gameType, state.copy(), fullPanel);
                if (actingRoot != null)
                    img = StateRenderer.withTextBelow(img, topActionLines(actingRoot, playerID, 5), null);
                ImageIO.write(img, "png", new File(base + ".png"));
            } catch (Exception e) {
                System.err.println("MCTSDecisionRecorder: failed to render PNG for " + base + " : " + e.getMessage());
            }
        }

        // 2. MCTS statistics (only if the acting player is an MCTS player with a usable root)
        if (actingRoot != null)
            writeRootActions(base + "_actions.tsv", actingRoot, playerID);
        if (!graphRoots.isEmpty())
            writeGraphviz(base + ".dot", base + "_tree.png", graphRoots);
    }

    /**
     * Writes a tab-delimited table of every action available at the root node. Columns are: visit count,
     * valid-visit count, whether the action is currently valid (part of the open-loop action set), the
     * action description, its mean value for the deciding player, and its action-heuristic estimate.
     * Actions never tried are included with a visit count of zero. Rows are ordered by visits descending.
     */
    protected void writeRootActions(String fileName, SingleTreeNode root, int playerID) {
        AbstractGameState rootState = root.getState();
        List<AbstractAction> openLoop = root.getActionsFromOpenLoopState();
        List<AbstractAction> actions = root.getChildren().keySet().stream()
                .sorted(Comparator.comparingInt((AbstractAction a) -> root.actionVisits(a)).reversed())
                .toList();
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write("Visits\tnValidVisits\tInOpenLoop\tAction\tValue\tHeuristicValue\n");
            for (AbstractAction action : actions) {
                int visits = root.actionVisits(action);
                int validVisits = root.actionValidVisits(action);
                String inOpenLoop = openLoop.contains(action) ? "Y" : "N";
                double value = visits > 0 ? root.actionTotValue(action, playerID) / visits : 0.0;
                double heuristic = root.actionHeuristicValue(action);
                String desc = rootState != null ? action.getString(rootState) : action.toString();
                writer.write(String.format("%d\t%d\t%s\t%s\t%.4f\t%.4f\n",
                        visits, validVisits, inOpenLoop, desc, value, heuristic));
            }
        } catch (IOException e) {
            System.err.println("MCTSDecisionRecorder: failed to write actions file " + fileName + " : " + e.getMessage());
        }
    }

    /** Builds the lines used to annotate the state PNG: the top {@code n} actions by visits, with value. */
    protected List<String> topActionLines(SingleTreeNode root, int playerID, int n) {
        AbstractGameState rootState = root.getState();
        List<AbstractAction> top = root.getChildren().keySet().stream()
                .sorted(Comparator.comparingInt((AbstractAction a) -> root.actionVisits(a)).reversed())
                .limit(n)
                .toList();
        List<String> lines = new ArrayList<>();
        lines.add(String.format("Top %d actions (visits, value):", top.size()));
        int rank = 1;
        for (AbstractAction action : top) {
            int visits = root.actionVisits(action);
            double value = visits > 0 ? root.actionTotValue(action, playerID) / visits : 0.0;
            String desc = rootState != null ? action.getString(rootState) : action.toString();
            lines.add(String.format("%d. %s  (visits=%d, value=%.3f)", rank++, desc, visits, value));
        }
        return lines;
    }

    /**
     * Writes a graphviz (DOT) digraph containing only the tree nodes with more than {@code visitThreshold}
     * visits. Nodes are labelled with the deciding player, visit count and mean node value; edges are
     * labelled with the action that leads from parent to child, and the children of each node are emitted
     * in descending order of visits. An edge is drawn for every action taken more than {@code visitThreshold}
     * times; if its child node is not itself in the graph (e.g. the action leads beyond the tree's max depth,
     * so the child accrued no node visits of its own) the edge instead leads to an empty (dashed) box
     * labelled with the action's visit count. If graphviz {@code dot} is on the PATH, a PNG render is also produced.
     * <p>
     * More than one {@code root} may be supplied (one per player) when MultiTree MCTS is in use: each tree
     * is emitted as a contiguous block, in the order given, so the rendered graph stacks the per-player
     * trees down the page. At each root any action that is not currently valid (i.e. not part of that
     * root's open-loop action set) is dropped, along with the whole subtree beneath it; this filtering is
     * applied only at the roots, not at deeper nodes.
     */
    protected void writeGraphviz(String dotFileName, String pngFileName, List<SingleTreeNode> roots) {
        // First work out the subtrees to exclude: those reached from a root via an action that is not in
        // that root's current open-loop (valid) action set.
        Set<SingleTreeNode> excluded = Collections.newSetFromMap(new IdentityHashMap<>());
        for (SingleTreeNode root : roots) {
            List<AbstractAction> openLoop = root.getActionsFromOpenLoopState();
            if (openLoop == null) continue;
            for (Map.Entry<AbstractAction, SingleTreeNode[]> entry : root.getChildren().entrySet()) {
                if (entry.getValue() == null || openLoop.contains(entry.getKey())) continue;
                for (SingleTreeNode child : entry.getValue())
                    if (child != null) excluded.addAll(child.allNodesInTree());
            }
        }

        // Gather the included nodes, grouped by tree (root) so each player's tree forms a contiguous block.
        List<SingleTreeNode> nodes = new ArrayList<>();
        for (SingleTreeNode root : roots)
            for (SingleTreeNode node : root.allNodesInTree())
                if (node.getVisits() > visitThreshold && !excluded.contains(node))
                    nodes.add(node);

        IdentityHashMap<SingleTreeNode, Integer> ids = new IdentityHashMap<>();
        for (SingleTreeNode node : nodes)
            ids.put(node, ids.size());

        // Map each in-tree node to the value of the action that leads to it, evaluated on its parent (and
        // from the parent's deciding-player perspective). The root has no incoming action, so it falls back
        // to its own mean node value.
        IdentityHashMap<SingleTreeNode, Double> incomingValue = new IdentityHashMap<>();
        for (SingleTreeNode node : nodes) {
            int parentPlayer = node.getActor();
            for (Map.Entry<AbstractAction, SingleTreeNode[]> entry : node.getChildren().entrySet()) {
                if (entry.getValue() == null) continue;
                int actionVisits = node.actionVisits(entry.getKey());
                double value = actionVisits > 0 ? node.actionTotValue(entry.getKey(), parentPlayer) / actionVisits : 0.0;
                for (SingleTreeNode child : entry.getValue()) {
                    if (child == null || !ids.containsKey(child)) continue;
                    incomingValue.put(child, value);
                }
            }
        }

        try (FileWriter writer = new FileWriter(dotFileName)) {
            writer.write("digraph MCTS {\n");
            writer.write("  node [shape=box];\n");
            for (SingleTreeNode node : nodes) {
                int player = node.getActor();
                double value = incomingValue.containsKey(node) ? incomingValue.get(node) : node.nodeValue(player);
                String label = String.format("P%d\\nvisits=%d\\nvalue=%.3f", player, node.getVisits(), value);
                writer.write(String.format("  n%d [label=\"%s\"];\n", ids.get(node), label));
            }
            // An edge target is either a real in-tree node (n<id>) or, for an action that was tried but
            // never expanded into a node, a synthetic empty box (the optional emptyDecl declares it).
            record DotEdge(String emptyDecl, String targetId, String label, int sortVisits) {}
            Set<SingleTreeNode> rootSet = Collections.newSetFromMap(new IdentityHashMap<>());
            rootSet.addAll(roots);
            int emptyCounter = 0;
            for (SingleTreeNode node : nodes) {
                // Open-loop MCTS discards the state at non-root nodes, so describe actions relative to the
                // node's state when present, otherwise fall back to the action's own description.
                AbstractGameState nodeState = node.getState();
                // At a root we only show currently-valid (open-loop) actions; deeper nodes are not filtered.
                List<AbstractAction> openLoop = rootSet.contains(node) ? node.getActionsFromOpenLoopState() : null;
                List<DotEdge> edges = new ArrayList<>();
                for (Map.Entry<AbstractAction, SingleTreeNode[]> entry : node.getChildren().entrySet()) {
                    AbstractAction action = entry.getKey();
                    if (openLoop != null && !openLoop.contains(action)) continue;
                    int actionVisits = node.actionVisits(action);
                    if (actionVisits <= visitThreshold) continue; // only actions taken more than the threshold
                    String label = escape(nodeState != null ? action.getString(nodeState) : action.toString());

                    List<SingleTreeNode> inGraph = new ArrayList<>();
                    if (entry.getValue() != null)
                        for (SingleTreeNode child : entry.getValue())
                            if (child != null && ids.containsKey(child)) inGraph.add(child);

                    if (!inGraph.isEmpty()) {
                        for (SingleTreeNode child : inGraph)
                            edges.add(new DotEdge(null, "n" + ids.get(child), label, child.getVisits()));
                    } else {
                        // The action was taken more than the threshold, but its resulting node is not in the
                        // graph - typically because it sits beyond the tree's max depth and so never accrued
                        // node visits of its own. Show it as an edge to an empty box labelled with the
                        // action's visit count and value (the value of the action, evaluated on this node
                        // from its deciding player's perspective - matching how real nodes are labelled).
                        double actionValue = node.actionTotValue(action, node.getActor()) / actionVisits;
                        String emptyId = "empty" + (emptyCounter++);
                        String decl = String.format("  %s [label=\"visits=%d\\nvalue=%.3f\", style=dashed];\n",
                                emptyId, actionVisits, actionValue);
                        edges.add(new DotEdge(decl, emptyId, label, actionVisits));
                    }
                }
                edges.sort(Comparator.comparingInt(DotEdge::sortVisits).reversed());
                for (DotEdge e : edges) {
                    if (e.emptyDecl() != null) writer.write(e.emptyDecl());
                    writer.write(String.format("  n%d -> %s [label=\"%s\"];\n", ids.get(node), e.targetId(), e.label()));
                }
            }
            writer.write("}\n");
        } catch (IOException e) {
            System.err.println("MCTSDecisionRecorder: failed to write graphviz file " + dotFileName + " : " + e.getMessage());
            return;
        }
        renderDotToPng(dotFileName, pngFileName);
    }

    /** Renders a DOT file to PNG using the graphviz {@code dot} executable, if it is installed. */
    protected void renderDotToPng(String dotFileName, String pngFileName) {
        if (!isDotAvailable())
            return;
        try {
            Process p = new ProcessBuilder("dot", "-Tpng", dotFileName, "-o", pngFileName)
                    .redirectErrorStream(true).start();
            if (!p.waitFor(60, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                System.err.println("MCTSDecisionRecorder: dot timed out rendering " + dotFileName);
            }
        } catch (IOException e) {
            System.err.println("MCTSDecisionRecorder: failed to run dot for " + dotFileName + " : " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Probes once (and caches) whether the graphviz {@code dot} command is available on the PATH. */
    private boolean isDotAvailable() {
        if (dotAvailable == null) {
            try {
                Process p = new ProcessBuilder("dot", "-V").redirectErrorStream(true).start();
                p.waitFor(10, TimeUnit.SECONDS);
                dotAvailable = true;
            } catch (IOException e) {
                dotAvailable = false; // dot not installed / not on PATH
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                dotAvailable = false;
            }
        }
        return dotAvailable;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ");
    }

    @Override
    public void report() {
        // Nothing to do - files are written per decision.
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }
}

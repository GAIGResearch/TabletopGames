package players.observers;

import core.AbstractGameState;
import core.AbstractPlayer;
import core.Game;
import core.actions.AbstractAction;
import evaluation.metrics.Event;
import games.GameType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSEnums;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class MCTSDecisionRecorderTest {

    Path tempDir;

    @Before
    public void setup() throws IOException {
        tempDir = Files.createTempDirectory("mctsRecorderTest");
    }

    @After
    public void tearDown() throws IOException {
        if (tempDir != null && Files.exists(tempDir)) {
            try (Stream<Path> walk = Files.walk(tempDir)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
    }

    private MCTSPlayer mctsPlayer(int iterations) {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", iterations);
        return new MCTSPlayer(params);
    }

    private MCTSPlayer multiTreeMctsPlayer(int iterations) {
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", iterations);
        params.setParameterValue("opponentTreePolicy", MCTSEnums.OpponentTreePolicy.MultiTree);
        return new MCTSPlayer(params);
    }

    /**
     * Drives a single decision manually and checks the root-action TSV lists every available action,
     * including ones never tried (which must appear with a visit count of zero). A tiny budget (fewer
     * iterations than Connect4's seven opening columns) guarantees some untried actions.
     */
    @Test
    public void recordsRootActionsIncludingUntried() throws IOException {
        MCTSPlayer mcts = mctsPlayer(3);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mcts);
        players.add(mcts.copy());

        Game game = GameType.Connect4.createGameInstance(2);
        game.reset(players);
        game.setSavedStatesDirectory(tempDir.toString());

        MCTSDecisionRecorder recorder = new MCTSDecisionRecorder(30, false);
        game.addListener(recorder); // sets the game on the recorder

        AbstractGameState state = game.getGameState();
        mcts.setForwardModel(game.getForwardModel());
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
        assertTrue("Connect4 should open with multiple actions", actions.size() > 1);
        AbstractAction chosen = mcts.getAction(state, actions);

        Event event = Event.createEvent(Event.GameEvent.ACTION_CHOSEN, state, chosen, actions, state.getCurrentPlayer());
        recorder.onEvent(event);

        Path actionsFile = findFile(tempDir, "_actions.tsv");
        assertNotNull("An _actions.tsv file should have been written", actionsFile);

        List<String> lines = Files.readAllLines(actionsFile);
        assertEquals("Visits\tnValidVisits\tInOpenLoop\tAction\tValue\tHeuristicValue", lines.get(0));
        List<String> rows = lines.subList(1, lines.size());
        assertEquals("Every available action should be listed", actions.size(), rows.size());
        boolean anyUntried = rows.stream().anyMatch(r -> r.startsWith("0\t"));
        assertTrue("With a 3-iteration budget some actions must be untried (0 visits)", anyUntried);
        for (String row : rows) {
            String[] cols = row.split("\t");
            assertEquals("Each row should have six columns", 6, cols.length);
            Integer.parseInt(cols[0]); // Visits
            Integer.parseInt(cols[1]); // nValidVisits
            assertTrue("InOpenLoop should be Y or N", cols[2].equals("Y") || cols[2].equals("N"));
        }
    }

    /**
     * Runs a full short game with MCTS players and the recorder attached, then checks that the four
     * artefact types are produced (JSON via the Game, plus PNG / actions TSV / graphviz DOT from the
     * recorder), and that the DOT is well-formed.
     */
    @Test
    public void endToEndProducesAllArtefacts() throws IOException {
        int threshold = 5;
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mctsPlayer(100));
        players.add(mctsPlayer(100));

        Game game = GameType.Connect4.createGameInstance(2);
        game.reset(players);
        game.setSavedStatesDirectory(tempDir.toString());
        game.addListener(new MCTSDecisionRecorder(threshold, false));

        game.run();

        assertNotNull("A JSON state file should exist", findFile(tempDir, ".json"));
        assertNotNull("A PNG should exist (Connect4 has a GUI)", findFile(tempDir, ".png"));
        assertNotNull("An actions TSV should exist", findFile(tempDir, "_actions.tsv"));
        Path dot = findFile(tempDir, ".dot");
        assertNotNull("A graphviz DOT should exist", dot);

        List<String> dotLines = Files.readAllLines(dot);
        assertTrue("DOT should declare a digraph", dotLines.get(0).startsWith("digraph"));
        assertEquals("}", dotLines.get(dotLines.size() - 1).trim());
        // every declared node line carries a visit count above the threshold
        for (String line : dotLines) {
            String trimmed = line.trim();
            if (trimmed.contains("visits=")) {
                int visits = Integer.parseInt(trimmed.substring(trimmed.indexOf("visits=") + 7).split("\\\\n")[0]);
                assertTrue("Nodes in the DOT must exceed the visit threshold", visits > threshold);
            }
        }
    }

    /**
     * With MultiTree MCTS a separate tree is kept per player. The DOT should therefore contain a tree for
     * each player that acted during search, with the active player's tree emitted first.
     */
    @Test
    public void multiTreeProducesOneTreePerPlayer() throws IOException {
        int threshold = 5;
        MCTSPlayer mcts = multiTreeMctsPlayer(400);
        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mcts);
        players.add(mcts.copy());

        Game game = GameType.Connect4.createGameInstance(2);
        game.reset(players);
        game.setSavedStatesDirectory(tempDir.toString());

        MCTSDecisionRecorder recorder = new MCTSDecisionRecorder(threshold, false);
        game.addListener(recorder);

        AbstractGameState state = game.getGameState();
        mcts.setForwardModel(game.getForwardModel());
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
        AbstractAction chosen = mcts.getAction(state, actions);

        int actingPlayer = state.getCurrentPlayer();
        Event event = Event.createEvent(Event.GameEvent.ACTION_CHOSEN, state, chosen, actions, actingPlayer);
        recorder.onEvent(event);

        Path dot = findFile(tempDir, ".dot");
        assertNotNull("A graphviz DOT should exist", dot);
        String dotText = String.join("\n", Files.readAllLines(dot));

        // Both players' trees should be present (P0 and P1 appear as node labels)...
        assertTrue("The acting player's tree should be present", dotText.contains("P" + actingPlayer + "\\n"));
        int other = 1 - actingPlayer;
        assertTrue("The opponent's tree should be present", dotText.contains("P" + other + "\\n"));

        // ...and the active player's tree should come first in the file.
        List<String> dotLines = Files.readAllLines(dot);
        int firstActing = firstNodeIndexForPlayer(dotLines, actingPlayer);
        int firstOther = firstNodeIndexForPlayer(dotLines, other);
        assertTrue("Active player's tree should be emitted before the opponent's", firstActing < firstOther);
    }

    /**
     * Actions taken more than the threshold whose child node lies beyond the tree's max depth (and so
     * accrued no node visits of its own) should still appear as an edge - one leading to an empty,
     * dashed placeholder box labelled with the action's visit count.
     */
    @Test
    public void maxDepthActionsLeadToEmptyBoxes() throws IOException {
        int threshold = 5;
        MCTSParams params = new MCTSParams();
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", 1000);
        params.setParameterValue("maxTreeDepth", 2);
        MCTSPlayer mcts = new MCTSPlayer(params);

        List<AbstractPlayer> players = new ArrayList<>();
        players.add(mcts);
        players.add(mcts.copy());

        Game game = GameType.Connect4.createGameInstance(2);
        game.reset(players);
        game.setSavedStatesDirectory(tempDir.toString());

        MCTSDecisionRecorder recorder = new MCTSDecisionRecorder(threshold, false);
        game.addListener(recorder);

        AbstractGameState state = game.getGameState();
        mcts.setForwardModel(game.getForwardModel());
        List<AbstractAction> actions = game.getForwardModel().computeAvailableActions(state);
        AbstractAction chosen = mcts.getAction(state, actions);

        Event event = Event.createEvent(Event.GameEvent.ACTION_CHOSEN, state, chosen, actions, state.getCurrentPlayer());
        recorder.onEvent(event);

        Path dot = findFile(tempDir, ".dot");
        assertNotNull("A graphviz DOT should exist", dot);
        List<String> dotLines = Files.readAllLines(dot);

        long emptyDecls = dotLines.stream().filter(l -> l.contains("style=dashed")).count();
        assertTrue("Max-depth actions should produce empty placeholder boxes", emptyDecls > 0);
        long emptyEdges = dotLines.stream().filter(l -> l.contains("-> empty")).count();
        assertEquals("Each empty box should be the target of exactly one edge", emptyDecls, emptyEdges);
        // empty boxes carry the action's visit count (must exceed the threshold) and its value
        for (String line : dotLines) {
            if (line.contains("style=dashed")) {
                String after = line.substring(line.indexOf("visits=") + 7);
                int visits = Integer.parseInt(after.split("[^0-9]")[0]);
                assertTrue("Empty-box edges represent actions taken more than the threshold", visits > threshold);
                assertTrue("Empty boxes should also show the action value", line.contains("value="));
            }
        }
    }

    private static int firstNodeIndexForPlayer(List<String> dotLines, int player) {
        String marker = "label=\"P" + player + "\\n";
        for (int i = 0; i < dotLines.size(); i++)
            if (dotLines.get(i).contains(marker)) return i;
        return Integer.MAX_VALUE;
    }

    private static Path findFile(Path root, String suffix) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(suffix))
                    .findFirst().orElse(null);
        }
    }
}

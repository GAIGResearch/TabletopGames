package evaluation.metrics;

import evaluation.RunArg;
import evaluation.listeners.MetricsGameListener;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import org.junit.Test;
import players.PlayerConstants;
import players.mcts.MCTSMetrics;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import tech.tablesaw.api.Row;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.Assert.*;

public class SimultaneousActionMetricsTest {

    /**
     * Runs one game with MCTS TreeStats recorded on ACTION_CHOSEN, and returns the (Tick -> PlayerIDs) found in the csv.
     */
    private Map<Integer, List<Integer>> playersPerTick(GameType gameType) throws IOException {
        Map<RunArg, Object> config = RunArg.parseConfig(new String[]{}, Collections.singletonList(RunArg.Usage.RunGames));
        config.put(RunArg.matchups, 1);
        config.put(RunArg.mode, "random");
        config.put(RunArg.listener, new ArrayList<String>());

        MCTSParams params = new MCTSParams();
        params.budgetType = PlayerConstants.BUDGET_ITERATIONS;
        params.budget = 50;
        params.setParameterValue("budgetType", PlayerConstants.BUDGET_ITERATIONS);
        params.setParameterValue("budget", 50);
        RoundRobinTournament tournament = new RoundRobinTournament(List.of(new MCTSPlayer(params)), gameType, 2, null, config);

        Path outDir = Files.createTempDirectory("simMetrics");
        MetricsGameListener listener = new MetricsGameListener(IDataLogger.ReportDestination.ToFile,
                new IDataLogger.ReportType[]{IDataLogger.ReportType.RawDataPerEvent},
                new AbstractMetric[]{new MCTSMetrics.TreeStats()});
        tournament.addListener(listener);
        listener.setOutputDirectory(outDir.toString());
        tournament.run();

        File csv = outDir.resolve("ACTION_CHOSEN.csv").toFile();
        assertTrue(csv.exists());
        Table table = Table.read().csv(csv);
        Map<Integer, List<Integer>> retValue = new TreeMap<>();
        for (Row row : table) {
            retValue.computeIfAbsent(row.getInt("Tick"), k -> new ArrayList<>())
                    .add(row.getInt("TreeStats(PlayerID)"));
        }
        return retValue;
    }

    @Test
    public void simultaneousMovesRecordOneRowPerPlayerPerTick() throws IOException {
        Map<Integer, List<Integer>> ticks = playersPerTick(GameType.Goofspiel);
        assertFalse(ticks.isEmpty());
        for (Map.Entry<Integer, List<Integer>> entry : ticks.entrySet()) {
            List<Integer> players = new ArrayList<>(entry.getValue());
            Collections.sort(players);
            assertEquals("Tick " + entry.getKey(), List.of(0, 1), players);
        }
    }

    @Test
    public void sequentialMovesRecordOneRowPerTick() throws IOException {
        Map<Integer, List<Integer>> ticks = playersPerTick(GameType.TicTacToe);
        assertFalse(ticks.isEmpty());
        for (Map.Entry<Integer, List<Integer>> entry : ticks.entrySet())
            assertEquals("Tick " + entry.getKey(), 1, entry.getValue().size());
    }
}

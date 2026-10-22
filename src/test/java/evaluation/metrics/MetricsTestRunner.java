package evaluation.metrics;

import core.AbstractParameters;
import core.AbstractPlayer;
import evaluation.RunArg;
import evaluation.listeners.MetricsGameListener;
import evaluation.tournaments.RoundRobinTournament;
import games.GameType;
import tech.tablesaw.api.Table;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Runs a tournament with a MetricsGameListener writing RawDataPerEvent files, and reads the files back.
 */
public class MetricsTestRunner {

    /**
     * @param agents   the first agent is the focus player, in each position (or team) in turn against the others
     * @param matchups the total number of games
     * @return the raw data table for each event that has a file
     */
    public static Map<Event.GameEvent, Table> runFocus(GameType gameType, int nPlayers, AbstractParameters params,
                                                       List<AbstractPlayer> agents, int matchups, boolean byTeam,
                                                       AbstractMetric... metrics) throws IOException {
        Map<RunArg, Object> config = RunArg.parseConfig(new String[]{}, Collections.singletonList(RunArg.Usage.RunGames));
        config.put(RunArg.matchups, matchups);
        config.put(RunArg.mode, "onevsall");
        config.put(RunArg.byTeam, byTeam);
        config.put(RunArg.seed, 35830953L);
        config.put(RunArg.listener, new ArrayList<String>());
        RoundRobinTournament tournament = new RoundRobinTournament(agents, gameType, nPlayers, params, config);

        Path outDir = Files.createTempDirectory("metricsTest");
        MetricsGameListener listener = new MetricsGameListener(IDataLogger.ReportDestination.ToFile,
                new IDataLogger.ReportType[]{IDataLogger.ReportType.RawDataPerEvent}, metrics);
        tournament.addListener(listener);
        listener.setOutputDirectory(outDir.toString());
        tournament.run();

        Map<Event.GameEvent, Table> tables = new HashMap<>();
        for (Event.GameEvent event : Event.GameEvent.values()) {
            File csv = outDir.resolve(event.name() + ".csv").toFile();
            if (csv.exists())
                tables.put(event, Table.read().csv(csv));
        }
        return tables;
    }
}

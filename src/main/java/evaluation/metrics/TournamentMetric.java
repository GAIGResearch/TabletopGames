package evaluation.metrics;

import core.AbstractPlayer;
import core.Game;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;

import java.io.File;
import java.util.*;

import static utilities.Utils.createDirectory;

/**
 * Records all data per player combination.
 * This is a wrapper around any AbstractMetric
 */
public class TournamentMetric extends AbstractMetric {
    // Data logger, wrapper around a library that logs data into a table
    private final Map<Set<AbstractPlayer>, IDataLogger> dataLoggers = new HashMap<>();

    AbstractMetric wrappedMetric;

    private boolean firstReport;

    public TournamentMetric(AbstractMetric metric) {
        super(metric.getEventTypes());
        this.wrappedMetric = metric;
        this.firstReport = true;
    }

    /**
     * @param listener - game listener object, with access to the game itself and loggers
     * @param e        - event, including game event type, state, action and player ID (if these properties are relevant, they may not be set depending on event type)
     * @param records  - map of data points to be filled in by the metric with recorded information
     * @return - true if the data saved in records should be recorded indeed, false otherwise. The metric
     * might want to listen to events for internal saving of information, but not actually record it in the data table.
     */
    protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
        return wrappedMetric._run(listener, e, records);
    }

    /**
     * @return set of game events this metric should record information for.
     */
    public Set<IGameEvent> getDefaultEventTypes() {
        return wrappedMetric.getDefaultEventTypes();
    }

    public void reset() {
        super.reset();
        for (IDataLogger logger : dataLoggers.values()) {
            logger.reset();
        }
    }

    /**
     * Initialize columns separately when we have access to the game.
     *
     * @param game - game to initialize columns for
     */
    public void init(Game game, int nPlayers, Set<String> playerNames) {
        // Do nothing here, we init specially
    }

    @Override
    public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
        return wrappedMetric.getColumns(nPlayersPerGame, playerNames);
    }

    public void tournamentInit(Game game, int nPlayers, Set<String> playerNames, Set<AbstractPlayer> matchup) {
        // Create a data logger for this matchup
        // TODO this counts same matchup if same type of players are in, regardless of order
        // If order matters (E.G. to see first player advantage), then this should be adjusted
        Set<AbstractPlayer> matchupSet = new HashSet<>(matchup);
        IDataLogger loggerRecorded = null;
        for (Set<AbstractPlayer> key : dataLoggers.keySet()) {
            if (new HashSet<>(key).containsAll(matchup) && matchupSet.containsAll(key)) {
                loggerRecorded = dataLoggers.get(key);
                break;
            }
        }
        if (loggerRecorded != null) {
            setDataLogger(loggerRecorded);
        } else {
            IDataLogger logger = dataLogger.create();
            dataLoggers.put(matchup, logger);
            logger.init(game, nPlayers, playerNames);
            setDataLogger(logger);
        }
    }

    /**
     * Produces reports of data for this metric.
     *
     * @param folderName         - name of the folder to save the reports in
     * @param reportTypes        - list of report types to produce
     * @param reportDestinations - list of report destinations to produce
     */
    public void report(String folderName, List<IDataLogger.ReportType> reportTypes, List<IDataLogger.ReportDestination> reportDestinations) {
        //DataProcessor with compatibility assertion:
        IDataProcessor dataProcessor = getDataProcessor();
        assert dataProcessor.getClass().isAssignableFrom(dataLogger.getDefaultProcessor().getClass()) :
                "Using a Data Processor " + dataProcessor.getClass().getSimpleName() + " that is not compatible with the Data Logger "
                        + dataLogger.getClass().getSimpleName() + ". Data Processor and Data Logger must be using the same library, and " +
                        " the Data Processor must extend the Data Logger's default processor.";

        for (Map.Entry<Set<AbstractPlayer>, IDataLogger> e : dataLoggers.entrySet()) {
            String folder = folderName + e.getKey().toString();
            // Make folder if it doesn't exist
            File f = new File(folder);
            if (!f.exists()) {
                createDirectory(folder);
            }
            IDataLogger logger = e.getValue();

            for (int i = 0; i < reportTypes.size(); i++) {
                IDataLogger.ReportType reportType = reportTypes.get(i);
                IDataLogger.ReportDestination reportDestination;
                if (reportDestinations.size() == 1) reportDestination = reportDestinations.get(0);
                else reportDestination = reportDestinations.get(i);

                if (reportType == IDataLogger.ReportType.RawData) {
                    if (reportDestination == IDataLogger.ReportDestination.ToFile || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                        dataProcessor.processRawDataToFile(logger, folder, !firstReport);
                    }
                    if (reportDestination == IDataLogger.ReportDestination.ToConsole || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                        dataProcessor.processRawDataToConsole(logger);
                    }
                } else if (reportType == IDataLogger.ReportType.Summary) {
                    if (reportDestination == IDataLogger.ReportDestination.ToFile || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                        dataProcessor.processSummaryToFile(logger, folder);
                    }
                    if (reportDestination == IDataLogger.ReportDestination.ToConsole || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                        dataProcessor.processSummaryToConsole(logger);
                    }
                } else if (reportType == IDataLogger.ReportType.Plot) {
                    if (reportDestination == IDataLogger.ReportDestination.ToFile || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                        dataProcessor.processPlotToFile(logger, folder);
                    }
                    if (reportDestination == IDataLogger.ReportDestination.ToConsole || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                        dataProcessor.processPlotToConsole(logger);
                    }
                }
            }
        }
        firstReport = false;
    }
}

package evaluation.metrics;

import core.Game;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractMetric {
    // Data logger, wrapper around a library that logs data into a table
    protected IDataLogger dataLogger;

    // Set of event types this metric listens to, to record data when they occur
    private final Set<IGameEvent> eventTypes;

    // Arguments for the metric, if any
    protected final String[] args;

    // Set of column names this metric records data for specifically
    private final Set<String> columnNames = new HashSet<>();

    // Number of games completed so far, can be used for any metric reset between games
    private int gamesCompleted;

    public AbstractMetric() {
        this.gamesCompleted = 0;
        this.eventTypes = getDefaultEventTypes();
        this.args = null;
    }

    public AbstractMetric(Event.GameEvent... args) {
        this.gamesCompleted = 0;
        this.eventTypes = Arrays.stream(args).collect(Collectors.toSet());
        this.args = null;
    }
    public AbstractMetric(Set<IGameEvent> events) {
        this.gamesCompleted = 0;
        this.eventTypes = events;
        this.args = null;
    }

    public AbstractMetric(String[] args) {
        this.gamesCompleted = 0;
        this.eventTypes = getDefaultEventTypes();
        this.args = args;
    }

    public AbstractMetric(String[] args, Event.GameEvent... events) {
        this.gamesCompleted = 0;
        this.eventTypes = Arrays.stream(events).collect(Collectors.toSet());
        this.args = args;
    }

    /**
     * @param listener - game listener object, with access to the game itself and loggers
     * @param e        - event, including game event type, state, action and player ID (if these properties are relevant, they may not be set depending on event type)
     * @param records  - map of data points to be filled in by the metric with recorded information
     * @return - true if the data saved in records should be recorded indeed, false otherwise. The metric
     * might want to listen to events for internal saving of information, but not actually record it in the data table.
     */
    protected abstract boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records);

    /**
     * @return set of game events this metric should record information for.
     */
    public abstract Set<IGameEvent> getDefaultEventTypes();

    public void reset() {
        this.gamesCompleted = 0;
        columnNames.clear();
        dataLogger.reset();
    }

    /**
     * Initialize columns separately when we have access to the game.
     *
     * @param game - game to initialize columns for
     */
    public void init(Game game, int nPlayers, Set<String> playerNames) {
        dataLogger.init(game, nPlayers, playerNames);
    }

    /**
     * Runs this metric. It first adds the data for the default columns after a given event. THen it runs _run() to
     * record the data specified in the metric subclass. All data is added to the data logger.
     *
     * @param listener - game listener object, with access to the game itself
     * @param e        - event, which includes game event type, state, action and player ID
     */
    public final void run(MetricsGameListener listener, Event e) {
        // Ask for custom records from the metric and record these too
        Map<String, Object> records = new HashMap<>();

        // Fill in the map with column names and null values for the custom columns we need data for
        for (String name : columnNames) {
            records.put(name, null);
        }

        // Run the metric and fill in the map with recorded data
        boolean record = _run(listener, e, records);

        if (record) {
            // Record default column data first, custom data for each default column
            addDefaultData(e);

            // Add the recorded data to the table
            for (Map.Entry<String, Object> entry : records.entrySet()) {
                dataLogger.addData(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Return a list of columns that will be recorded for this metric. The string is the name of the column and
     * the class<?> is the type of data that will be recorded in that column.
     * !! If you want categorical data, make it a string column and use the string value, even if number
     *
     * @param nPlayersPerGame
     * @param playerNames
     * @return map of column names and types
     */
    public abstract Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames);

    /**
     * Returns a map of default column names and types. This will be included in all metrics unless overwritten by the metric subclass.
     * The string is the name of the column and the class<?> is the type of data that will be recorded in that column.
     *
     * @return map of column names and types
     */
    public Map<String, Class<?>> getDefaultColumns() {
        HashMap<String, Class<?>> columns = new HashMap<>();
        columns.put("GameID", String.class);
        columns.put("GameName", String.class);
        columns.put("PlayerCount", String.class);
        columns.put("GameSeed", String.class);
        columns.put("Tick", Integer.class);
        columns.put("Turn", Integer.class);
        columns.put("Round", Integer.class);
        columns.put("Event", String.class);
        return columns;
    }


    /**
     * Records data for the default columns. It must match the column names and types in getDefaultColumns().
     *
     * @param e event for which the data is recorded
     */
    public void addDefaultData(Event e) {
        dataLogger.addData("GameID", String.valueOf(e.state.getGameID()));
        dataLogger.addData("GameName", e.state.getGameType().name());
        dataLogger.addData("PlayerCount", String.valueOf(e.state.getNPlayers()));
        dataLogger.addData("GameSeed", String.valueOf(e.state.getGameParameters().getRandomSeed()));
        dataLogger.addData("Tick", e.state.getGameTick());
        dataLogger.addData("Turn", e.state.getTurnCounter());
        dataLogger.addData("Round", e.state.getRoundCounter());
        dataLogger.addData("Event", e.type.name());
    }

    /**
     * Returs the set of events this metric listens to
     *
     * @return set of events
     */
    public Set<IGameEvent> getEventTypes() {
        return eventTypes;
    }

    /**
     * Standard name for this metric, using the class name. If parameterized metric, different format applies.
     */
    public final String getName() {
        return this.getClass().getSimpleName();
    }

    /**
     * @return true if this metric listens to the given game event type, false otherwise.
     */
    public final boolean listens(IGameEvent eventType)
    {
        //by default, we listen to all types of events.
        if (eventTypes == null) return true;
        return eventTypes.contains(eventType);
    }

    /**
     * @return true if this metric should filter data in table by event type when reporting, creating several
     * tables of separate relevant data instead of one. Default behaviour is true. May override to return false
     * if the metric is not event-specific or would like to keep all data together.
     */
    public boolean filterByEventTypeWhenReporting() {
        return true;
    }

    /**
     * Produces reports of data for this metric.
     *
     * @param folderName         - name of the folder to save the reports in
     * @param reportTypes        - list of report types to produce
     * @param reportDestinations - list of report destinations to produce
     */
    public void report(String folderName,
                       List<IDataLogger.ReportType> reportTypes,
                       List<IDataLogger.ReportDestination> reportDestinations,
                       boolean append)
    {
        //DataProcessor with compatibility assertion:
        IDataProcessor dataProcessor = getDataProcessor();
        assert dataProcessor.getClass().isAssignableFrom(dataLogger.getDefaultProcessor().getClass()) :
                "Using a Data Processor " + dataProcessor.getClass().getSimpleName() + " that is not compatible with the Data Logger "
                        + dataLogger.getClass().getSimpleName() + ". Data Processor and Data Logger must be using the same library, and " +
                        " the Data Processor must extend the Data Logger's default processor.";

        for (int i = 0; i < reportTypes.size(); i++) {
            IDataLogger.ReportType reportType = reportTypes.get(i);
            IDataLogger.ReportDestination reportDestination;
            if (reportDestinations.size() == 1) reportDestination = reportDestinations.get(0);
            else reportDestination = reportDestinations.get(i);

            if (reportType == IDataLogger.ReportType.RawData) {
                if (reportDestination == IDataLogger.ReportDestination.ToFile || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                    dataProcessor.processRawDataToFile(dataLogger, folderName, append);
                }
                if (reportDestination == IDataLogger.ReportDestination.ToConsole || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                    dataProcessor.processRawDataToConsole(dataLogger);
                }
            } else if (reportType == IDataLogger.ReportType.Summary) {
                if (reportDestination == IDataLogger.ReportDestination.ToFile || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                    dataProcessor.processSummaryToFile(dataLogger, folderName);
                }
                if (reportDestination == IDataLogger.ReportDestination.ToConsole || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                    dataProcessor.processSummaryToConsole(dataLogger);
                }
            } else if (reportType == IDataLogger.ReportType.Plot) {
                if (reportDestination == IDataLogger.ReportDestination.ToFile || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                    dataProcessor.processPlotToFile(dataLogger, folderName);
                }
                if (reportDestination == IDataLogger.ReportDestination.ToConsole || reportDestination == IDataLogger.ReportDestination.ToBoth) {
                    dataProcessor.processPlotToConsole(dataLogger);
                }
            }
        }

    }

    /**
     * Provides an object of the class that implements reporting. This class must implement the different ways to report
     * (ReportType) and the destination (ReportDestination).
     *
     * @return the data processor
     */
    public IDataProcessor getDataProcessor() {
        return dataLogger.getDefaultProcessor();
    }

    public void addColumnName(String name) {
        columnNames.add(name);
    }

    public Set<String> getColumnNames() {
        return columnNames;
    }

    public void notifyGameOver() {
        this.gamesCompleted++;
    }

    public int getGamesCompleted() {
        return gamesCompleted;
    }

    public void setDataLogger(IDataLogger logger) {
        this.dataLogger = logger;
    }

    public IDataLogger getDataLogger() {
        return dataLogger;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractMetric)) return false;
        AbstractMetric that = (AbstractMetric) o;
        return Objects.equals(eventTypes, that.eventTypes) && Arrays.equals(args, that.args) && Objects.equals(columnNames, that.columnNames);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(eventTypes, columnNames);
        result = 31 * result + Arrays.hashCode(args);
        return result;
    }
}

package evaluation.metrics;

import core.Game;
import evaluation.listeners.MetricsGameListener;
import evaluation.summarisers.TAGStatSummary;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractMetric
{
    Table data;
    Set<String> columnNames = new HashSet<>();

    // Set of event types this metric listens to, to record data when they occur
    private final Set<Event.GameEvent> eventTypes;

    public AbstractMetric() {
        this.eventTypes = getDefaultEventTypes();
        this.data = Table.create(getName());
    }

    public AbstractMetric(Event.GameEvent... args) {
        this.eventTypes = Arrays.stream(args).collect(Collectors.toSet());
        this.data = Table.create(getName());
    }

    /**
     * Initialize columns separately when we have access to the game.
     * @param game - game to initialize columns for
     */
    public void init(Game game) {
        data.addColumns(getDefaultColumns(game));
        Column<?>[] columns = getColumns(game);
        data.addColumns(columns);

        // Save column names for later, iterate through columns and find their name
        for (Column<?> column : columns) {
            columnNames.add(column.name());
        }
    }

    /**
     * !! Make sure data matches column type requested!  // TODO: maybe more verbose to make this foolproof
     *
     * @param listener - game listener object, with access to the game itself and loggers
     * @param e - event, including game event type, state, action and player ID (if these properties are relevant, they may not be set depending on event type)
     * @param records - map of data points to be filled in by the metric with recorded information
     */
    protected abstract void _run(MetricsGameListener listener, Event e, Map<String, Object> records);

    public final void run(MetricsGameListener listener, Event e) {
        // Record default column data first, custom data for each default column
        addDefaultData(e);

        // Ask for custom records from the metric and record these too
        Map<String, Object> records = new HashMap<>();

        // Fill in the map with column names and null values for the custom columns we need data for
        for (String name: columnNames) {
            records.put(name, null);
        }

        // Run the metric and fill in the map with recorded data
        _run(listener, e, records);

        // Add the recorded data to the table
        for (Map.Entry<String, Object> entry : records.entrySet()) {
            addData(entry.getKey(), entry.getValue());
        }
    }

    /**
     * @return set of game events this metric should record information for.
     */
    public abstract Set<Event.GameEvent> getDefaultEventTypes();

    /**
     * Add a piece of data to a specific column.
     * @param columnName - name of column to add data to
     * @param data - data to add
     */
    private void addData(String columnName, Object data) {
        Column<Object> column = (Column<Object>) this.data.column(columnName);
        if (data == null) {
            column.appendMissing();
        } else {
            column.append(data);
        }
    }

    /**
     * Retrieves a list of columns this metric records data for.
     * Initialize using name and type, e.g. DoubleColumn.create("MyColumn")
     * <a href="https://www.javadoc.io/static/tech.tablesaw/tablesaw-core/0.43.1/tech/tablesaw/api/package-summary.html">javadoc for column types</a>
     * @return - list of columns
     */
    public abstract Column<?>[] getColumns(Game game);
    private Column<?>[] getDefaultColumns(Game game) {
        // Create a column for: game ID, game name, player count, game seed
        return new Column<?>[] {
                IntColumn.create("GameID"),
                StringColumn.create("GameName"),
                IntColumn.create("PlayerCount"),
                LongColumn.create("GameSeed"),
                IntColumn.create("Tick"),
                IntColumn.create("Turn"),
                IntColumn.create("Round"),
        };
    }
    private void addDefaultData(Event e) {
        addData("GameID", e.state.getGameID());
        addData("GameName", e.state.getGameType().name());
        addData("PlayerCount", e.state.getNPlayers());
        addData("GameSeed", e.state.getGameParameters().getRandomSeed());
        addData("Tick", e.state.getGameTick());
        addData("Turn", e.state.getTurnCounter());
        addData("Round", e.state.getRoundCounter());
    }

    public Set<Event.GameEvent> getEventTypes() {
        return eventTypes;
    }

    /**
     * Some post-processing of metric data at the end of a game (used by metrics that record data at several points throughout the game).
     * @return a summary of the data recorded during the game by the metric (to have a single data point per game).
     * @param e - game over event, including state
     * @param recordedData - data recorded by this metric during the game
     */
    public Map<String, Object> postProcessingGameOver(Event e, TAGStatSummary recordedData) {
        // Process the recorded data during the game and return game over summarised data
        Map<String, Object> toRecord = new HashMap<>();
        Map<String, Object> summaryData = recordedData.getSummary();
        for (String k: summaryData.keySet()) {
            toRecord.put(getClass().getSimpleName() + "(" + k + ")" + ":" + e.type, summaryData.get(k));
        }
        return toRecord;
    }

    /* Final methods, not to be overridden */

    /**
     * Standard name for this metric, using the class name. If parameterized metric, different format applies.
     */
    public final String getName() {
        if (this instanceof AbstractParameterizedMetric)
            return ((AbstractParameterizedMetric)this).name();
        return this.getClass().getSimpleName();
    }

    /**
     * @return true if this metric listens to the given game event type, false otherwise.
     */
    public final boolean listens(Event.GameEvent eventType)
    {
        //by default, we listen to all types of events.
        if(eventTypes == null) return true;
        return eventTypes.contains(eventType);
    }

    public enum ReportType {
        RawData,
        Summary,
        Plot
    }
    public enum ReportDestination {
        File,
        Console,
        Both
    }

    /**
     * All games have finished. Must report, according to reporting requests.
     * @param folderName - folder to write reports to
     * @param reportTypes - types of reports to write ( raw data / summary / plot )
     * @param reportDestinations -  destinations to write reports to (console / file / both)
     */
    public void allGamesFinished(String folderName, List<ReportType> reportTypes, List<ReportDestination> reportDestinations) {
        for (int i = 0; i < reportTypes.size(); i++) {
            ReportType reportType = reportTypes.get(i);
            ReportDestination reportDestination = reportDestinations.get(i);
            if (reportType == ReportType.RawData) {
                if (reportDestination == ReportDestination.File || reportDestination == ReportDestination.Both) {
                    data.write().csv(folderName + "/" + getName() + ".csv");
                }
                if (reportDestination == ReportDestination.Console || reportDestination == ReportDestination.Both) {
                    System.out.println(data);
                }
            } else if (reportType == ReportType.Summary) {
                if (reportDestination == ReportDestination.File || reportDestination == ReportDestination.Both) {
                    System.out.println("Summary report not implemented yet");
                }
                if (reportDestination == ReportDestination.Console || reportDestination == ReportDestination.Both) {
                    List<String> summarisedData = summariseData();
                    for (String summary : summarisedData) {
                        System.out.println(summary);
                    }
                }
            } else if (reportType == ReportType.Plot) {
//                if (reportDestination == ReportDestination.File || reportDestination == ReportDestination.Both) {
//                    System.out.println("Plot report not implemented yet");
//                }
//                if (reportDestination == ReportDestination.Console || reportDestination == ReportDestination.Both) {
                    List<Figure> figures = plotData();
                    for (Figure figure : figures) {
                        Plot.show(figure);
                    }
//                }
            }
        }
    }

    /**
     * Summarise the data recorded by this metric.
     * @return a list of strings, each summarising a column of data, or other customized summary.
     */
    protected List<String> summariseData() {
        List<String> summary = new ArrayList<>();
        for (Column<?> column : data.columns()) {
            if (columnNames.contains(column.name())) {
                summary.add(data.name() + ": " + column.summary() + "\n");
            }
        }
        return summary;
    }

    /**
     * Plot the data recorded by this metric.
     * @return - a list of figures, each plotting a column of data, or some customized plots.
     */
    protected List<Figure> plotData() {
        List<Figure> figures = new ArrayList<>();
        for (Column<?> column: data.columns()) {
            if (columnNames.contains(column.name()) && column instanceof NumberColumn) {
                double[] x = new double[column.size()];
                for (int j = 0; j < column.size(); j++) {
                    x[j] = j;
                }
                double[] y = new double[column.size()];
                for (int j = 0; j < column.size(); j++) {
                    y[j] = (double) column.get(j);
                }

                figures.add(LinePlot.create(data.name(), "Data point", x, column.name(), y));
            }
        }
        return figures;
    }
}

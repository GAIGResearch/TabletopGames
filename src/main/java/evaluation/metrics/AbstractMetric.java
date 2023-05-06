package evaluation.metrics;

import core.Game;
import evaluation.listeners.MetricsGameListener;
import evaluation.summarisers.TAGNumericStatSummary;
import evaluation.summarisers.TAGStatSummary;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.io.File;
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
     * !! Don't use constructor (new DoubleColumn("MyColumn")) as this is private
     * !! If you want categorical data, make it a string column and use the string value, even if number
     * <a href="https://www.javadoc.io/static/tech.tablesaw/tablesaw-core/0.43.1/tech/tablesaw/api/package-summary.html">javadoc for column types</a>
     * @return - list of columns
     */
    public abstract Column<?>[] getColumns(Game game);
    private Column<?>[] getDefaultColumns(Game game) {
        // Create a column for: game ID, game name, player count, game seed
        return new Column<?>[] {
                StringColumn.create("GameID"),
                StringColumn.create("GameName"),
                StringColumn.create("PlayerCount"),
                StringColumn.create("GameSeed"),
                IntColumn.create("Tick"),
                IntColumn.create("Turn"),
                IntColumn.create("Round"),
        };
    }
    private void addDefaultData(Event e) {
        addData("GameID", String.valueOf(e.state.getGameID()));
        addData("GameName", e.state.getGameType().name());
        addData("PlayerCount", String.valueOf(e.state.getNPlayers()));
        addData("GameSeed", String.valueOf(e.state.getGameParameters().getRandomSeed()));
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
        ToFile,
        ToConsole,
        ToBoth
    }

    /**
     * All games have finished. Must report, according to reporting requests.
     * @param folderName - folder to write reports to
     * @param reportTypes - types of reports to write ( raw data / summary / plot )
     * @param reportDestinations -  destinations to write reports to (console / file / both)
     */
    public void allGamesFinished(String folderName, List<ReportType> reportTypes, List<ReportDestination> reportDestinations) {
        int nGames = data.column("GameID").countUnique();

        for (int i = 0; i < reportTypes.size(); i++) {
            ReportType reportType = reportTypes.get(i);
            ReportDestination reportDestination = reportDestinations.get(i);
            if (reportType == ReportType.RawData) {
                if (reportDestination == ReportDestination.ToFile || reportDestination == ReportDestination.ToBoth) {
                    data.write().csv(folderName + "/" + getName() + ".csv");
                }
                System.out.println();
                if (reportDestination == ReportDestination.ToConsole || reportDestination == ReportDestination.ToBoth) {
                    System.out.println(data);
                }
            } else if (reportType == ReportType.Summary) {
                if (reportDestination == ReportDestination.ToFile || reportDestination == ReportDestination.ToBoth) {
                    System.out.println("Summary report not implemented yet");
                }
                if (reportDestination == ReportDestination.ToConsole || reportDestination == ReportDestination.ToBoth) {
                    List<String> summarisedData;
                    if (nGames < data.column(0).size()) {
                        summarisedData = summariseDataProgression();
                    } else {
                        summarisedData = summariseData();
                    }
                    System.out.println();
                    for (String summary : summarisedData) {
                        System.out.println(summary);
                    }
                }
            } else if (reportType == ReportType.Plot) {
                // TODO: can only output to HTML (javascript) files, figure out how to make pictures

                File plotFolder = new File(folderName + "/plots");
                boolean success = true;
                if (!plotFolder.exists()) {
                    success = plotFolder.mkdir();
                }
                File plotFolderMetric = new File(folderName + "/plots/" + getName());
                if (!plotFolderMetric.exists()) {
                    success = plotFolderMetric.mkdir();
                }
                if (success) {
                    Map<String, Figure> figures;
                    if (nGames < data.column(0).size()) {
                        figures = plotDataProgression();
                    } else {
                        figures = plotData();
                    }
                    for (Map.Entry<String, Figure> figure : figures.entrySet()) {
                        Plot.show(figure.getValue(), new File(plotFolderMetric + "/" + figure.getKey() + ".html"));
                    }
                }
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
                if (column instanceof StringColumn) {
                    summary.add(data.name() + ": " + ((StringColumn) column).countByCategory() + "\n");
                } else {
                    summary.add(data.name() + ": " + column.summary() + "\n");
                }
            }
        }
        return summary;
    }

    protected List<String> summariseDataProgression() {
        int nGames = data.column("GameID").countUnique();

        List<String> summary = new ArrayList<>();
        for (Column<?> column : data.columns()) {
            if (columnNames.contains(column.name())) {
                if (column instanceof StringColumn) {
                    // Create counts of each category per game
                    Table[] tablesPerGame = new Table[nGames];
                    Set<String> categoryNames = new HashSet<>();
                    int i = 0;
                    for (Object id: data.column("GameID").unique().asObjectArray()) {
                        tablesPerGame[i] = ((StringColumn)column.where(data.stringColumn("GameID").isEqualTo((String) id))).countByCategory();
                        // Needs transposing because the output of previous is several rows with category value, count (2 columns)
                        tablesPerGame[i] = tablesPerGame[i].transpose(false, true);
                        // Save all column names for the summary table
                        categoryNames.addAll(tablesPerGame[i].columnNames());
                        i++;
                    }

                    // Create summary table with columns for each category
                    Table summaryTable = Table.create("Summary " + column.name());
                    for (String categoryName: categoryNames) {
                        summaryTable.addColumns(IntColumn.create(categoryName));
                    }

                    // Append all data to the summary table. We'll have 1 column per categorical value
                    // And 1 row per game, with the counts of each category
                    for (i = 0; i < nGames; i++) {
                        for (String category: categoryNames) {
                            if (tablesPerGame[i].columnNames().contains(category)) {
                                IntColumn categoryColumn = tablesPerGame[i].intColumn(category);
                                summaryTable.intColumn(category).append(categoryColumn);
                            } else {
                                summaryTable.intColumn(category).append(0);
                            }
                        }
                    }

                    // Make a print table with detail counts per game
                    Table printTable = summaryTable.transpose(true,false);
                    printTable.column(0).setName(column.name() + " \\ Game #");
                    summary.add(printTable + "\n");

                    // Make a print table with summary stats over all game
                    Table statsTable = summaryTable.intColumn(0).summary();
                    statsTable.column(1).setName(summaryTable.column(0).name());
                    statsTable.setName("Stats " + column.name());
                    for (i = 1; i < summaryTable.columnCount(); i++) {
                        DoubleColumn c = summaryTable.intColumn(i).summary().doubleColumn(1);
                        c.setName(summaryTable.column(i).name());
                        statsTable.addColumns(c);
                    }
                    summary.add(statsTable.transpose(true, true) + "\n");

//                    for (Column<?> c: summaryTable.columns()) {
//                        summary.add(data.name() + ":" + column.name() + ": " + c.summary() + "\n");
//                    }
//                    summary.add(data.name() + ": " + summaryTable.summary() + "\n");
                } else {
                    // This is the same as summariseData
                    summary.add(data.name() + ": " + column.summary() + "\n");
                }
            }
        }
        return summary;
    }

    /**
     * Plot the data recorded by this metric. Progression over the course of a game, averaged across the different
     * games recorded.
     * @return - a list of figures, each plotting a column of data, or some customized plots.
     */
    protected Map<String, Figure> plotDataProgression() {
        int nGames = data.column("GameID").countUnique();
        int maxTick = 0;
        Table[] tablesPerGame = new Table[nGames];

        int i = 0;
        for (Object id: data.column("GameID").unique().asObjectArray()) {  // todo game ID starts at 2???
            tablesPerGame[i] = data.where(data.stringColumn("GameID").isEqualTo((String) id));
            int maxTickThisGame = tablesPerGame[i].intColumn("Tick").size();
            if (maxTickThisGame > maxTick) {
                maxTick = maxTickThisGame;
            }
            i++;
        }

        Map<String, Figure> figures = new HashMap<>();
        for (i = 0; i < data.columnCount(); i++) {
            Column<?> column = data.column(i);
            if (columnNames.contains(column.name()) && column instanceof NumberColumn) {
                double[] x = new double[maxTick];
                for (int j = 0; j < maxTick; j++) {
                    x[j] = j;
                }
                double[] yMean = new double[column.size()];
                double[] yMeanSdMinus = new double[column.size()];
                double[] yMeanSdPlus = new double[column.size()];
                for (int j = 0; j < maxTick; j++) {
                    TAGNumericStatSummary ss = new TAGNumericStatSummary();
                    for (int k = 0; k < nGames; k++) {
                        Column<?> columnThisGame = tablesPerGame[k].column(column.name());
                        if (columnThisGame.size() > j) {
                            ss.add((double) columnThisGame.get(j));
                        }
                    }
                    double err = 0;
                    if (ss.n() > 1) err = ss.stdErr();
                    yMean[j] = ss.mean();
                    yMeanSdMinus[j] = yMean[j] - err;
                    yMeanSdPlus[j] = yMean[j] + err;
                }

                Trace yMeanSdPlusTrace = ScatterTrace.builder(x, yMeanSdPlus).name("Mean + SD")
                        .opacity(0.3)
                        .line(Line.builder().simplify(true).dash(Line.Dash.DASH_DOT).color("rgb(0, 0, 255)").build())
                        .mode(ScatterTrace.Mode.LINE).build();
                Trace yMeanTrace = ScatterTrace.builder(x, yMean).name("Mean")
                        .mode(ScatterTrace.Mode.LINE)
                        .line(Line.builder().width(2).color("rgb(0, 0, 255)").build())
                        .build();
                Trace yMeanSdMinusTrace = ScatterTrace.builder(x, yMeanSdMinus).name("Mean - SD")
                        .opacity(0.3)
                        .line(Line.builder().simplify(true).dash(Line.Dash.DASH_DOT).color("rgb(0, 0, 255)").build())
                        .mode(ScatterTrace.Mode.LINE).build();

                Figure figure = new Figure(yMeanSdPlusTrace, yMeanTrace, yMeanSdMinusTrace);

//                figures.put(column.name(), LinePlot.create(data.name(), "Data point", x, column.name(), y));
                figures.put(column.name(), figure);
            }
        }
        return figures;
    }

    /**
     * Plot the data recorded by this metric.
     * @return - a list of figures, each plotting a column of data, or some customized plots.
     */
    protected Map<String, Figure> plotData() {
        Map<String, Figure> figures = new HashMap<>();
        for (Column<?> column : data.columns()) {
            if (columnNames.contains(column.name()) && column instanceof NumberColumn) {
//                figures.add(LinePlot.create(data.name(), "Game ID", x, column.name(), y));
                figures.put(column.name(), LinePlot.create(data.name(), Table.create(column, data.column("GameID")), "GameID", column.name()));
            }
        }
        return figures;
    }
}

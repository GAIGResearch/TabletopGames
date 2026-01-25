package evaluation.metrics.tablessaw;

import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IDataLogger;
import evaluation.metrics.IDataProcessor;
import evaluation.summarisers.TAGNumericStatSummary;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.io.csv.CsvWriteOptions;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
import tech.tablesaw.plotly.components.*;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.BoxTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

import static utilities.Utils.createDirectory;

public class TableSawDataProcessor implements IDataProcessor {


    @Override
    public void processRawDataToFile(IDataLogger logger, String folderName, boolean append) {
        DataTableSaw dts = (DataTableSaw) logger;
        String filename = folderName + "/" + dts.data.name() + ".csv";
        if(!append) {
            dts.data.write().csv(filename);
        } else {
            try {
                File file = new File(filename);
                boolean headerNeeded = !file.exists();
                Writer w = new FileWriter(file, true);
                CsvWriteOptions.Builder options = CsvWriteOptions.builder(w);
                options.header(headerNeeded);
                dts.data.write().csv(options.build());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void processRawDataToConsole(IDataLogger logger) {
        DataTableSaw dts = (DataTableSaw) logger;
        System.out.println();
        System.out.println(dts.data);
    }

    private Map<String, List<Table>> getSummarisedData(DataTableSaw dts) {
        Map<String, List<Table>> summarisedData;
        if (dts.metric.getGamesCompleted() < dts.data.column(0).size()) {
            summarisedData = summariseDataProgression(dts.metric, dts.data);
        } else {
            summarisedData = summariseData(dts.metric, dts.data);
        }
        return summarisedData;
    }


    @Override
    public void processSummaryToConsole(IDataLogger logger) {
        DataTableSaw dts = (DataTableSaw) logger;
        Map<String, List<Table>> summarisedData = getSummarisedData(dts);

        for (Map.Entry<String, List<Table>> e : summarisedData.entrySet()) {
            System.out.println();
            for (Table t : e.getValue()) {
                System.out.println(t + "\n");
            }
        }
    }

    @Override
    public void processSummaryToFile(IDataLogger logger, String folderName) {
        DataTableSaw dts = (DataTableSaw) logger;
        Map<String, List<Table>> summarisedData = getSummarisedData(dts);

        File summaryFolder = new File(folderName + "/summaries");
        boolean success = true;
        if (!summaryFolder.exists()) {
            createDirectory(folderName + "/summaries");
        }
        File summaryFolderMetric = new File(folderName + "/summaries/" + dts.metric.getName());
        if (!summaryFolderMetric.exists()) {
            createDirectory(folderName + "/summaries/" + dts.metric.getName());
        }

        for (String columnSummary : summarisedData.keySet()) {
            List<Table> tables = summarisedData.get(columnSummary);
            for (Table t : tables) {
                t.write().csv(summaryFolderMetric + "/" + t.name() + ".csv");
            }
        }
    }

    @Override
    public void processPlotToFile(IDataLogger logger, String folderName) {
        DataTableSaw dts = (DataTableSaw) logger;
        File plotFolder = new File(folderName + "/plots");
        boolean success = true;
        if (!plotFolder.exists()) {
            success = plotFolder.mkdir();
        }
        File plotFolderMetric = new File(folderName + "/plots/" + dts.metric.getName());
        if (!plotFolderMetric.exists()) {
            success = plotFolderMetric.mkdir();
        }
        if (success) {
            Map<String, Figure> figures;
            if (dts.metric.getGamesCompleted() < dts.data.column(0).size()) {
                figures = plotDataProgression(dts.metric, dts.data);
            } else {
                figures = plotData(dts.metric, dts.data);
            }
            for (Map.Entry<String, Figure> figure : figures.entrySet()) {
                TAGPlot.save(figure.getValue(), new File(plotFolderMetric + "/" + figure.getKey() + ".html"));
            }
        }
    }

    @Override
    public void processPlotToConsole(IDataLogger logger) {
        System.out.println("Plot report to console not implemented yet");
    }

    /**
     * Summarise the data recorded by this metric.
     *
     * @return a mapping from column name to list of strings, each summarising a column of data,
     * or other customized summary.
     */
    protected Map<String, List<Table>> summariseData(AbstractMetric metric, Table rawData) {
        Map<String, List<Table>> allDataSummaries = new HashMap<>();
        for (Column<?> c : rawData.columns()) {
            Table filteredData = (rawData.where(rawData.column(c.name()).isNotMissing()));
            Column<?> column = filteredData.column(c.name());
            if (metric.getColumnNames().contains(column.name())) {
                Table summary;
                if (column instanceof StringColumn) {
//                    summary.add(filteredData.name() + ": " + ((StringColumn) column).countByCategory() + "\n");
                    summary = ((StringColumn) column).countByCategory();
                } else {
//                    summary.add(filteredData.name() + ": " + column.summary() + "\n");
                    summary = column.summary();
                }
                summary.setName(filteredData.name() + "_" + column.name());
                allDataSummaries.put(column.name(), Collections.singletonList(summary));
            }
        }
        return allDataSummaries;
    }

    /**
     * Summarise the data recorded by this metric, per game. Only different for categorical (StringColumn) data.
     * Same as summariseData for numerical data, even if progression through game.
     * Prints 2 tables per metric:
     * - one showing detailed counts in each game for each categorical value
     * - one showing statistics overall for each categorical value (mean, std, min, max etc.)
     *
     * @return - a list of strings, each summarising a column of data, or other customized summary.
     */
    protected Map<String, List<Table>> summariseDataProgression(AbstractMetric metric, Table rawData) {
        int nGames;
        Map<String, List<Table>> allDataSummaries = new HashMap<>();

        for (Column<?> c : rawData.columns()) {
            if (metric.getColumnNames().contains(c.name())) {

                Table filteredData = (rawData.where(rawData.column(c.name()).isNotMissing()));
                Column<?> column = filteredData.column(c.name());
                if (column.isEmpty()) continue;

                Object[] gameIds = filteredData.column("GameID").unique().asObjectArray();
                nGames = gameIds.length;

                List<Table> summary = new ArrayList<>();
                if (column instanceof StringColumn) {
                    // Create counts of each category per game
                    Table[] tablesPerGame = new Table[nGames];
                    Set<String> categoryNames = new HashSet<>();
                    int i = 0;
                    for (Object id : gameIds) {
                        tablesPerGame[i] = ((StringColumn) column.where(filteredData.stringColumn("GameID").isEqualTo((String) id))).countByCategory();
                        // Needs transposing because the output of previous is several rows with category value, count (2 columns)
                        try {
                            tablesPerGame[i] = tablesPerGame[i].transpose(false, true);
                        } catch (Exception e) {
                            System.out.println("Error transposing table: " + e);
                        }
                        // Save all column names for the summary table
                        categoryNames.addAll(tablesPerGame[i].columnNames());
                        i++;
                    }

                    // Create summary table with columns for each category
                    Table summaryTable = Table.create("Summary " + column.name());
                    for (String categoryName : categoryNames) {
                        summaryTable.addColumns(IntColumn.create(categoryName));
                    }

                    // Append all data to the summary table. We'll have 1 column per categorical value
                    // And 1 row per game, with the counts of each category
                    for (i = 0; i < nGames; i++) {
                        for (String category : categoryNames) {
                            if (tablesPerGame[i].columnNames().contains(category)) {
                                IntColumn categoryColumn = tablesPerGame[i].intColumn(category);
                                summaryTable.intColumn(category).append(categoryColumn);
                            } else {
                                summaryTable.intColumn(category).append(0);
                            }
                        }
                    }

                    if (summaryTable.isEmpty()) {
//                        System.out.println("Empty summary table for " + column.name());
                        continue;
                    }

                    // Make a print table with detail counts per game, transposed for more compact printing
                    Table printTable = summaryTable.transpose(true, false);
                    printTable.column(0).setName(column.name() + " \\ Game #");
                    // Add table to the summary to print
                    summary.add(printTable);

                    // Make a print table with summary stats over all game
                    // Taking the summary of the first category as start table. All have 2 columns, measure and value.
                    Table statsTable = summaryTable.intColumn(0).summary();
                    // Change the name of the second column ('value') to the actual name of the category
                    statsTable.column(1).setName(summaryTable.column(0).name());
                    // Name the table appropriately
                    statsTable.setName("Stats " + column.name());
                    // Add the other categories as columns, taking only the second column (value) for each and naming them appropriately, according to the category name
                    for (i = 1; i < summaryTable.columnCount(); i++) {
                        DoubleColumn dc = summaryTable.intColumn(i).summary().doubleColumn(1);
                        dc.setName(summaryTable.column(i).name());
                        statsTable.addColumns(dc);
                    }
                    // Add table to the summary to print
                    summary.add(statsTable.transpose(true, true).sortDescendingOn("Mean"));
                } else {
                    // This is the same as summariseData for numerical data
                    Table sum = column.summary();
                    sum.setName(filteredData.name() + "_" + column.name());
                    summary.add(sum);
                }

                allDataSummaries.put(column.name(), summary);
            }
        }
        return allDataSummaries;
    }

    /**
     * Plot the data recorded by this metric. Progression over the course of a game, averaged across the different
     * games recorded.
     *
     * @return - a list of figures, each plotting a column of data, or some customized plots.
     */
    protected Map<String, Figure> plotDataProgression(AbstractMetric metric, Table data) {
        Object[] gameIds = data.column("GameID").unique().asObjectArray();
        int nGames = gameIds.length;
        int maxTick = 0;
        Table[] tablesPerGame = new Table[nGames];

        int i = 0;
        for (Object id : gameIds) {  // todo game ID starts at 2???
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
            if (metric.getColumnNames().contains(column.name())) {
                //TODO Not checked that this doesn't break with missing values. If error found, it may be that! o.O
                if (column instanceof NumberColumn) {
                    // Make a line plot - actually 3 lines, mean, mean+sd, mean-sd
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
                                ss.add(Double.parseDouble(String.valueOf(columnThisGame.get(j))));
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

                    Layout layout = Layout.builder().title(data.name())
                            .height(600).width(800)
                            .yAxis(Axis.builder().title(column.name()).build())
                            .xAxis(Axis.builder().title(getLabel(metric)).build())
                            .build();

                    Figure figure = new Figure(layout, yMeanSdPlusTrace, yMeanTrace, yMeanSdMinusTrace);

//                figures.put(column.name(), LinePlot.create(data.name(), "Data point", x, column.name(), y));
                    figures.put(column.name(), figure);
                } else {
                    // Make box plots from the categorical counts across games

                    // Create counts of each category per game
                    Table[] tablesCountsPerGame = new Table[nGames];
                    int idx = 0;
                    for (Object id : data.column("GameID").unique().asObjectArray()) {
                        tablesCountsPerGame[idx] = ((StringColumn) column.where(data.stringColumn("GameID").isEqualTo((String) id)))
                                .removeMissing()
                                .countByCategory();
                        idx++;
                    }
                    Table countsPerGame = tablesCountsPerGame[0];
                    for (idx = 1; idx < nGames; idx++) {
                        countsPerGame = countsPerGame.append(tablesCountsPerGame[idx]);
                    }

                    // Create box plots from the counts
                    Layout layout = Layout.builder().title(data.name())
                            .height(600).width(800)
                            .yAxis(Axis.builder().title("Count").range(0, 30).build())  //  TODO hard-coded range
                            .xAxis(Axis.builder().title(column.name()).categoryOrder(Axis.CategoryOrder.CATEGORY_ASCENDING).build())
                            .build();
                    BoxTrace trace = BoxTrace.builder(countsPerGame.categoricalColumn("Category"), countsPerGame.nCol("Count"))
                            .build();

                    figures.put(column.name(), new Figure(layout, trace));
                }
            }
        }
        return figures;
    }

    /**
     * Defines x-axis label for a progression plot, based on the events the metric listens to.
     * Chooses the lowest level of granularity that the metric listens to.
     *
     * @param metric - the metric
     * @return - the label
     */
    protected String getLabel(AbstractMetric metric) {
        if (metric.listens(Event.GameEvent.GAME_EVENT)
                || metric.listens(Event.GameEvent.ACTION_CHOSEN)
                || metric.listens(Event.GameEvent.ACTION_TAKEN)) {
            return "Game Tick";
        } else if (metric.listens(Event.GameEvent.TURN_OVER)) {
            return "Game Turn";
        }
        return "Game Round";
    }

    /**
     * Plot the data recorded by this metric.
     *
     * @return - a list of figures, each plotting a column of data, or some customized plots.
     */
    protected Map<String, Figure> plotData(AbstractMetric metric, Table rawData) {
        Map<String, Figure> figures = new HashMap<>();

        for (Column<?> c : rawData.columns()) {
            Table filteredData = (rawData.where(rawData.column(c.name()).isNotMissing()));
            Column<?> column = filteredData.column(c.name());
            if (metric.getColumnNames().contains(column.name())) {
                if (column instanceof NumberColumn) {
                    figures.put(column.name(), LinePlot.create(filteredData.name(), Table.create(column, filteredData.column("GameID")), "GameID", column.name()));
                } else {
                    // Make a bar plot from the categorical count
                    Table t2 = ((StringColumn) column).countByCategory();
//                    t2 = t2.sortDescendingOn(t2.column(1).name()); //todo this sorts the table, but not the plot when we build it.
                    Layout layout = Layout.builder()
                            .title(filteredData.name())
                            .yAxis(Axis.builder().title(column.name()).build())
                            .xAxis(Axis.builder().categoryOrder(Axis.CategoryOrder.TRACE).build())
                            .build();
                    BarTrace trace = BarTrace.builder(t2.categoricalColumn(0), t2.numberColumn(1))
                            .build();
                    figures.put(column.name(), new Figure(layout, trace));
                }
            }
        }
        return figures;
    }

    /**
     * Overwrites functionality to only save the figures as .html files, rather than opening the browsers too.
     */
    static class TAGPlot extends Plot {
        public static void save(Figure figure, File outputFile) {
            Page page = Page.pageBuilder(figure, "target").build();
            String output = page.asJavascript();
            try (Writer writer = new OutputStreamWriter(Files.newOutputStream(outputFile.toPath()), StandardCharsets.UTF_8)) {
                writer.write(output);
            } catch (IOException var14) {
                throw new UncheckedIOException(var14);
            }
        }
    }
}

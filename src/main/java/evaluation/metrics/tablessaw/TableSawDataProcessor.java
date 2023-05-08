package evaluation.metrics.tablessaw;

import evaluation.metrics.AbstractMetric;
import evaluation.metrics.IDataLogger;
import evaluation.metrics.IDataProcessor;
import evaluation.summarisers.TAGNumericStatSummary;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;
import tech.tablesaw.plotly.Plot;
import tech.tablesaw.plotly.api.LinePlot;
import tech.tablesaw.plotly.components.Axis;
import tech.tablesaw.plotly.components.Figure;
import tech.tablesaw.plotly.components.Layout;
import tech.tablesaw.plotly.components.Line;
import tech.tablesaw.plotly.traces.BarTrace;
import tech.tablesaw.plotly.traces.BoxTrace;
import tech.tablesaw.plotly.traces.ScatterTrace;
import tech.tablesaw.plotly.traces.Trace;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class TableSawDataProcessor implements IDataProcessor {


    @Override
    public void processRawDataToFile(IDataLogger logger, String folderName) {
        DataTableSaw dts = (DataTableSaw) logger;
        dts.data.write().csv(folderName + "/" + dts.metric.getName() + ".csv");
    }

    @Override
    public void processRawDataToConsole(IDataLogger logger) {
        DataTableSaw dts = (DataTableSaw) logger;
        System.out.println();
        System.out.println(dts.data);
    }

    private HashMap<String, List<String>> getSummarisedData(DataTableSaw dts)
    {
        HashMap<String, List<String>> summarisedData;
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
        HashMap<String, List<String>> summarisedData = getSummarisedData(dts);

        for (String columnSummary : summarisedData.keySet()) {
            System.out.println();
            for(String summaryLines : summarisedData.get(columnSummary))
                System.out.println(summaryLines);
        }
    }

    @Override
    public void processSummaryToFile(IDataLogger logger, String folderName) {
        DataTableSaw dts = (DataTableSaw) logger;
        HashMap<String, List<String>> summarisedData = getSummarisedData(dts);

        File summaryFolder = new File(folderName + "/summaries");
        boolean success = true;
        if (!summaryFolder.exists()) {
            success = summaryFolder.mkdir();
        }
        File summaryFolderMetric = new File(folderName + "/summaries/" + dts.metric.getName());
        if (!summaryFolderMetric.exists()) {
            success = summaryFolderMetric.mkdir();
        }

        if (success) for (String columnSummary : summarisedData.keySet()) {
            try {
                FileWriter fw = new FileWriter(summaryFolderMetric + "/"  + columnSummary + ".txt");
                for(String summaryLines : summarisedData.get(columnSummary))
                    fw.write(summaryLines);
                fw.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
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
                Plot.show(figure.getValue(), new File(plotFolderMetric + "/" + figure.getKey() + ".html"));
            }
        }
    }

    @Override
    public void processPlotToConsole(IDataLogger logger) {
        System.out.println("Plot report to console not implemented yet");
    }


    /**
     * Summarise the data recorded by this metric.
     * @return a list of strings, each summarising a column of data, or other customized summary.
     */
    protected HashMap<String, List<String>> summariseData(AbstractMetric metric, Table data) {
        HashMap<String, List<String>>  allDataSummaries = new HashMap<>();

        for (Column<?> column : data.columns()) {
            if (metric.getColumnNames().contains(column.name())) {
                List<String> summary = new ArrayList<>();
                if (column instanceof StringColumn) {
                    summary.add(data.name() + ": " + ((StringColumn) column).countByCategory() + "\n");
                } else {
                    summary.add(data.name() + ": " + column.summary() + "\n");
                }
                allDataSummaries.put(column.name(), summary);
            }
        }
        return allDataSummaries;
    }

    /**
     * Summarise the data recorded by this metric, per game. Only different for categorical (StringColumn) data.
     * Same as summariseData for numerical data, even if progression through game.
     * Prints 2 tables per metric:
     *  - one showing detailed counts in each game for each categorical value
     *  - one showing statistics overall for each categorical value (mean, std, min, max etc.)
     * @return - a list of strings, each summarising a column of data, or other customized summary.
     */
    protected HashMap<String, List<String>> summariseDataProgression(AbstractMetric metric, Table data) {
        int nGames = data.column("GameID").countUnique();
        HashMap<String, List<String>>  allDataSummaries = new HashMap<>();

        for (Column<?> column : data.columns()) {
            if (metric.getColumnNames().contains(column.name())) {
                List<String> summary = new ArrayList<>();
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

                    // Make a print table with detail counts per game, transposed for more compact printing
                    Table printTable = summaryTable.transpose(true,false);
                    printTable.column(0).setName(column.name() + " \\ Game #");
                    // Add table to the summary to print
                    summary.add(printTable + "\n");

                    // Make a print table with summary stats over all game
                    // Taking the summary of the first category as start table. All have 2 columns, measure and value.
                    Table statsTable = summaryTable.intColumn(0).summary();
                    // Change the name of the second column ('value') to the actual name of the category
                    statsTable.column(1).setName(summaryTable.column(0).name());
                    // Name the table appropriately
                    statsTable.setName("Stats " + column.name());
                    // Add the other categories as columns, taking only the second column (value) for each and naming them appropriately, according to the category name
                    for (i = 1; i < summaryTable.columnCount(); i++) {
                        DoubleColumn c = summaryTable.intColumn(i).summary().doubleColumn(1);
                        c.setName(summaryTable.column(i).name());
                        statsTable.addColumns(c);
                    }
                    // Add table to the summary to print
                    summary.add(statsTable.transpose(true, true) + "\n");
                } else {
                    // This is the same as summariseData for numerical data
                    summary.add(data.name() + ": " + column.summary() + "\n");
                }

                allDataSummaries.put(column.name(), summary);
            }
        }
        return allDataSummaries;
    }

    /**
     * Plot the data recorded by this metric. Progression over the course of a game, averaged across the different
     * games recorded.
     * @return - a list of figures, each plotting a column of data, or some customized plots.
     */
    protected Map<String, Figure> plotDataProgression(AbstractMetric metric, Table data) {
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
            if (metric.getColumnNames().contains(column.name())) {
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
                } else {
                    // Make box plots from the categorical counts across games

                    // Create counts of each category per game
                    Table[] tablesCountsPerGame = new Table[nGames];
                    int idx = 0;
                    for (Object id: data.column("GameID").unique().asObjectArray()) {
                        tablesCountsPerGame[idx] = ((StringColumn)column.where(data.stringColumn("GameID").isEqualTo((String) id))).countByCategory();
                        idx++;
                    }
                    Table countsPerGame = tablesCountsPerGame[0];
                    for (idx = 1; idx < nGames; idx++) {
                        countsPerGame = countsPerGame.append(tablesCountsPerGame[idx]);
                    }

                    // Create box plots from the counts
                    Layout layout = Layout.builder().title(data.name())
                            .height(600).width(800)
                            .yAxis(Axis.builder().title("Count").build())
                            .xAxis(Axis.builder().title(column.name()).build())
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
     * Plot the data recorded by this metric.
     * @return - a list of figures, each plotting a column of data, or some customized plots.
     */
    protected Map<String, Figure> plotData(AbstractMetric metric, Table data) {
        Map<String, Figure> figures = new HashMap<>();
        for (Column<?> column : data.columns()) {
            if (metric.getColumnNames().contains(column.name())) {
                if (column instanceof NumberColumn) {
                    figures.put(column.name(), LinePlot.create(data.name(), Table.create(column, data.column("GameID")), "GameID", column.name()));
                } else {
                    // Make a bar plot from the categorical count
                    Table t2 = ((StringColumn)column).countByCategory();
//                    t2 = t2.sortDescendingOn(t2.column(1).name()); //todo this sorts the table, but not the plot when we build it.
                    Layout layout = Layout.builder().title(data.name()).yAxis(Axis.builder().title(column.name()).build()).build();
                    BarTrace trace = BarTrace.builder(t2.categoricalColumn(0), t2.numberColumn(1))
                            .build();
                    figures.put(column.name(), new Figure(layout, trace));
                }
            }
        }
        return figures;
    }

}

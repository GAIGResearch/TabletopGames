package evaluation.metrics.tablessaw;

import core.Game;
import core.interfaces.IGameEvent;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.IDataLogger;
import evaluation.metrics.IDataProcessor;
import tech.tablesaw.api.*;
import tech.tablesaw.columns.Column;

import java.util.*;

/**
 * Using TableSaw for data storage and visualisation:
 * <a href="https://jtablesaw.github.io/tablesaw/userguide/toc">User Guide</a>
 * <a href='https://www.javadoc.io/doc/tech.tablesaw/tablesaw-core/latest/overview-summary.html'>Java Doc</a>
 */
public class DataTableSaw implements IDataLogger {

    Table data;
    AbstractMetric metric;

    public DataTableSaw(AbstractMetric metric) {
        this.metric = metric;
        this.data = Table.create(metric.getName());
    }
    private DataTableSaw(AbstractMetric metric, Table data) {
        this.metric = metric;
        this.data = data;
    }

    /**
     * Builds a column using name and type, e.g. DoubleColumn.create("MyColumn")
     * !! Don't use constructor (new DoubleColumn("MyColumn")) as this is private
     * <a href="https://www.javadoc.io/static/tech.tablesaw/tablesaw-core/0.43.1/tech/tablesaw/api/package-summary.html">javadoc for column types</a>
     * @return - A column of the given type
     */
    private Column<?> buildColumn (String name, Class<?> c) {
        if(c == String.class)
            return StringColumn.create(name);
        else if(c == Integer.class)
            return IntColumn.create(name);
        else if(c == Double.class)
            return DoubleColumn.create(name);
        else if(c == Boolean.class)
            return BooleanColumn.create(name);
        else
            throw new AssertionError("Unknown column type");
    }

    private Column<?> buildColumn (String name, Class<?> c, List<?> values) {
        if(c == String.class) {
            List<String> stringValues = new ArrayList<>();
            for (Object o : values) {
                stringValues.add(o.toString());
            }
            return StringColumn.create(name, stringValues);
        } else if(c == Integer.class) {
            List<Integer> intValues = new ArrayList<>();
            for (Object o : values) {
                intValues.add((Integer) o);
            }
            return IntColumn.create(name, intValues.toArray(new Integer[0]));
        } else if(c == Double.class) {
            List<Double> doubleValues = new ArrayList<>();
            for (Object o : values) {
                doubleValues.add((Double) o);
            }
            return DoubleColumn.create(name, doubleValues);
        } else if(c == Boolean.class) {
            List<Boolean> booleanValues = new ArrayList<>();
            for (Object o : values) {
                booleanValues.add((Boolean) o);
            }
            return BooleanColumn.create(name, booleanValues);
        } else
            throw new AssertionError("Unknown column type");
    }

    public void reset()
    {
        this.data = Table.create(metric.getName());
    }

    public void init(Game game, int nPlayersPerGame, Set<String> playerNames) {
        // Add default columns
        Map<String, Class<?>> defaultColumns = metric.getDefaultColumns();
        for (Map.Entry<String, Class<?>> entry : defaultColumns.entrySet()) {
            if (!data.containsColumn(entry.getKey()))
                data.addColumns(buildColumn(entry.getKey(), entry.getValue()));
        }

        // Add metric-defined columns
        Map<String, Class<?>> columns = metric.getColumns(nPlayersPerGame, playerNames);
        for (Map.Entry<String, Class<?>> entry : columns.entrySet())
            if (!data.containsColumn(entry.getKey())) {
                data.addColumns(buildColumn(entry.getKey(), entry.getValue()));

                // Keep the name of the column
                metric.addColumnName(entry.getKey());
            }
    }

    /**
     * Add a piece of data to a specific column.
     * @param columnName - name of column to add data to
     * @param data - data to add
     */
    public void addData(String columnName, Object data) {
        Column<Object> column = (Column<Object>) this.data.column(columnName);
        if (data == null) {
            column.appendMissing();
        } else {
            column.append(data);
        }
    }

    @Override
    public IDataProcessor getDefaultProcessor() {
        return new TableSawDataProcessor();
    }

    @Override
    public void flush() {
        this.data = data.emptyCopy();
    }

    @Override
    public IDataLogger copy() {
        return new DataTableSaw(metric, data.copy());
    }

    @Override
    public IDataLogger emptyCopy() {
        return new DataTableSaw(metric, data.emptyCopy());
    }

    @Override
    public IDataLogger create() {
        return new DataTableSaw(metric);
    }

    /**
     * Puts together several compatible metrics into a single table.
     * @param metricGroup - List of metrics to combine
     * @param event - Event to filter by
     * @param indexingColumnName - Name of the column to use for indexing.
     *                           Assumes a single row exists for each unique value in this column per game.
     *                           e.g. ROUND_OVER metrics can be grouped by specifying "Round" as the indexing column.
     */
    public DataTableSaw(List<AbstractMetric> metricGroup, IGameEvent event, String indexingColumnName) {
        this.data = Table.create(event.name());

        // If this is true, then we don't need to worry about indexing
        boolean indexingColumnIsGameID = indexingColumnName.equals("GameID");

        // Find only the rows which were recorded for the given event
        // TODO: Apply same filtering for all other data processing, separate table into different events before reporting
        Map<AbstractMetric, Table> metricTables = new HashMap<>();
        for (AbstractMetric m : metricGroup) {
            Table metricData = ((DataTableSaw)m.getDataLogger()).data;
            if (m.filterByEventTypeWhenReporting()) {
                metricTables.put(m, metricData.where(metricData.stringColumn("Event").isEqualTo(event.name())));
            } else {
                metricTables.put(m, metricData);
            }
        }

        // Find and sort ascending all unique values in the column to use for indexing in all the metrics
        List<Integer> gameIDs = new ArrayList<>();
        for (Table metricData: metricTables.values()) {
            StringColumn c = metricData.stringColumn("GameID");
            for (String s : c.asList()) {
                int id = Integer.parseInt(s);
                if (!gameIDs.contains(id)) {
                    gameIDs.add(id);
                }
            }
        }
        Collections.sort(gameIDs);

        // Create unique columns
        int maxIndex = 0;
        Map<String, Set<String>> columnNames = new HashMap<>();
        Set<String> allColumnNames = new HashSet<>();  // Including only the default at start, the others separate for ordering
        for (Map.Entry<AbstractMetric, Table> metricTableEntry : metricTables.entrySet()) {
            Table metricData = metricTableEntry.getValue();
            AbstractMetric m = metricTableEntry.getKey();
            columnNames.put(m.getName(), m.getColumnNames());
            allColumnNames.addAll(m.getDefaultColumns().keySet());

            // Find the maximum number of rows in the indexing column filtering by game ID
            if (!indexingColumnIsGameID) {
                for (Column<?> c : metricData.columns()) {
                    if (c.name().equals(indexingColumnName)) {
                        for (int id : gameIDs) {
                            Optional<?> maxValue = metricData
                                    // Filter the table first by game ID
                                    .where(metricData.stringColumn("GameID").isEqualTo(String.valueOf(id)))
                                    // Then find the indexing column
                                    .column(c.name())
                                    // And count the number of unique values
                                    .max(Comparator.comparing((Object a) -> ((Integer) a)));
                            if (maxValue.isPresent()) {
                                int nIdx = (Integer) maxValue.get();
                                if (nIdx > maxIndex) {
                                    maxIndex = nIdx;
                                }
                            }
                        }
                    }
                }
            }
        }
        // Put default columns first
        for (String col : allColumnNames) {
            data.addColumns(StringColumn.create(col));
        }
        // Then the rest
        for (Map.Entry<String, Set<String>> entry : columnNames.entrySet()) {
            for (String s: entry.getValue()) {
                String colName = entry.getKey() + "(" + s + ")";
                if (!data.containsColumn(colName)) {
                    data.addColumns(StringColumn.create(colName));
                    // Also add the name of this column to all columns
                    allColumnNames.add(colName);
                }
            }
        }

        // Add data from all the metrics to each of the columns, row by row, checking for values being equal in colStep to account for missing values in some of the columns
        for (int id: gameIDs) {
            if (!indexingColumnIsGameID) {
                for (int idx = 0; idx <= maxIndex; idx++) {
                    filterAndRecordData(metricTables, allColumnNames, id, indexingColumnName, idx);
                }
            } else {
                filterAndRecordData(metricTables, allColumnNames, id, null, -1);
            }
        }
    }

    /**
     * Helper function for {@link #DataTableSaw(List, IGameEvent, String)}.
     * Filters the data by game ID and index, then adds the data to the table.
     * @param metricGroup - List of metrics to add data from in the table
     * @param allColumnNames - All column names in the table
     * @param gameID - Game ID to filter by
     * @param indexingColumnName - Name of the column to use for indexing. If null, then we don't filter by this column
     * @param idx - Index to filter by. If -1, then we don't filter by this index
     */
    private void filterAndRecordData(Map<AbstractMetric, Table> metricGroup, Set<String> allColumnNames, int gameID, String indexingColumnName, int idx) {
        Map<String, String> rowData = new HashMap<>();
        for (String colName : allColumnNames) {
            rowData.put(colName, null);
        }
        boolean record = false;

        // Find the row matching game ID and index in all the metric tables. If we don't find it, we add missing for the columns that metric is responsible for
        for (Map.Entry<AbstractMetric, Table> metricTableEntry : metricGroup.entrySet()) {
            AbstractMetric m = metricTableEntry.getKey();
            Table metricData = metricTableEntry.getValue();
            // Filter the table first by game ID
            Table filteredData = metricData.where(metricData.stringColumn("GameID").isEqualTo(String.valueOf(gameID)));
            // Then find the indexing column, if needed
            if (indexingColumnName != null && idx > -1) {
                filteredData = filteredData.where(filteredData.intColumn(indexingColumnName).isEqualTo(idx));
            }
            if (filteredData.rowCount() == 1) {
                // We only record if we find a row
                record = true;

                // Add the data from the row
                for (Column<?> c : metricData.columns()) {
                    Object o = filteredData.column(c.name()).get(0);
                    if (o != null) {
                        String dataPoint = o.toString();
                        if (m.getColumnNames().contains(c.name())) {
                            rowData.put(m.getName() + "(" + c.name() + ")", dataPoint);
                        } else {
                            rowData.put(c.name(), dataPoint);
                        }
                    } else {
                        rowData.put(c.name(), null);
                    }
                }
            }
        }

        // Here we should have one complete row, with or without missing values, but same size for all columns in the big table
        if (record) {
            for (Map.Entry<String, String> entry : rowData.entrySet()) {
                if (data.containsColumn(entry.getKey())) {
                    if (entry.getValue() == null) {
                        data.stringColumn(entry.getKey()).appendMissing();
                    } else {
                        data.stringColumn(entry.getKey()).append(entry.getValue());
                    }
                } else {
                    // TODO fix, final combination of tables
                }
            }
        }
    }

}
package evaluation.metrics.tablessaw;

import core.Game;
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

    /**
     * Builds a column using name and type, e.g. DoubleColumn.create("MyColumn")
     * !! Don't use constructor (new DoubleColumn("MyColumn")) as this is private
     * <a href="https://www.javadoc.io/static/tech.tablesaw/tablesaw-core/0.43.1/tech/tablesaw/api/package-summary.html">javadoc for column types</a>
     * @return - A column of the given type
     */

    private Column<?> buildColumn (String name, Class<?> c)
    {
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

    public void reset()
    {
        this.data = Table.create(metric.getName());
    }

    public void init(Game game) {

        // Add default columns
        Map<String, Class<?>> defaultColumns = metric.getDefaultColumns();
        for (Map.Entry<String, Class<?>> entry : defaultColumns.entrySet())
            data.addColumns(buildColumn(entry.getKey(), entry.getValue()));

        // Add metric-defined columns
        Map<String, Class<?>> columns = metric.getColumns(game);
        for (Map.Entry<String, Class<?>> entry : columns.entrySet())
            data.addColumns(buildColumn(entry.getKey(), entry.getValue()));

        // Iterate through columns and find their name
        for (String colName : columns.keySet())
            metric.addColumnName(colName);

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


}

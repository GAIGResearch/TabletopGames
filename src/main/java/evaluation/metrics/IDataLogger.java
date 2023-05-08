package evaluation.metrics;

import core.Game;

public interface IDataLogger
{
    enum ReportType {
        RawData,
        Summary,
        Plot
    }

    enum ReportDestination {
        ToFile,
        ToConsole,
        ToBoth
    }

    /**
     * Initialise the data logger with the metric and game. This method is called once before the game starts.
     * @param game Game that is being played
     */
    void init(Game game);

    /**
     * Add a piece of data to a specific column.
     * @param columnName - name of column to add data to
     * @param data - data to add
     */
    void addData(String columnName, Object data);

    /**
     * Returns a data processor by default that is compatible with this data logger
     * @return - A data processor
     */
    IDataProcessor getDefaultProcessor();

}

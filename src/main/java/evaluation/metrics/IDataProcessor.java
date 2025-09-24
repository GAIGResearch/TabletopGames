package evaluation.metrics;

public interface IDataProcessor
{
    /**
     * Processes the raw data and prints it to console
     * @param logger - logger that contains the raw data
     */
    void processRawDataToConsole(IDataLogger logger);

    /**
     * Creates a summary from the data and prints it to console
     * @param logger - logger that contains all the raw data
     */
    void processSummaryToConsole(IDataLogger logger);

    /**
     * Creates a plot from the data and displays it on a window.
     * @param logger - logger that contains all the raw data
     */
    void processPlotToConsole(IDataLogger logger);


    /**
     * Processes the raw data and prints it to a file located at the folderName folder
     * @param logger - logger that contains the raw data
     * @param folderName - name of the folder to save the file to
     */
    void processRawDataToFile(IDataLogger logger, String folderName, boolean append);


    /**
     * Creates a summary from the data and prints it to a file located at the folderName folder
     * @param logger - logger that contains the raw data
     * @param folderName - name of the folder to save the file to
     */
    void processSummaryToFile(IDataLogger logger, String folderName);


    /**
     * Creates a plot from the data and saves it to a file located at the folderName folder
     * @param logger - logger that contains the raw data
     * @param folderName - name of the folder to save the file to
     */
    void processPlotToFile(IDataLogger logger, String folderName);

}

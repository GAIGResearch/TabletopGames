package core.interfaces;

import evaluation.loggers.SummaryLogger;
import evaluation.summarisers.TAGStatSummary;

import java.lang.reflect.Constructor;
import java.util.*;

public interface IStatisticLogger {

    /**
     * Use to register a set of data in one go
     *
     * @param data A map of name -> value pairs
     */
    void record(Map<String, ?> data);

    /**
     * Use to record a single datum. For example
     *
     * @param key
     * @param datum
     */
    void record(String key, Object datum);

    /**
     * Trigger any specific batch processing of data by this Logger.
     * This should be called once all data has been collected. This may also, for example,
     * purge all buffers and close files/database connections.
     */
    void processDataAndFinish();

    void processDataAndNotFinish();

    /**
     * This should return a Map with one entry for each type of data
     *
     * @return A summary of the data
     */
    Map<String, TAGStatSummary> summary();
    IStatisticLogger emptyCopy(String id);

    static IStatisticLogger createLogger(String loggerClass, String logFile) {
        if (logFile.isEmpty())
            throw new IllegalArgumentException("Must specify logFile");
        IStatisticLogger logger = new SummaryLogger();
        try {
            Class<?> clazz = Class.forName(loggerClass);

            Constructor<?> constructor;
            try {
                constructor = clazz.getConstructor(String.class);
                logger = (IStatisticLogger) constructor.newInstance(logFile);
            } catch (NoSuchMethodException e) {
                constructor = clazz.getConstructor();
                logger = (IStatisticLogger) constructor.newInstance();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return logger;
    }
}
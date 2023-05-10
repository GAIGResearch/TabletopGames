package evaluation.listeners;

import core.Game;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.*;
import utilities.Utils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;

public interface IGameListener {

    /**
     * Manages all events.
     * @param event  Event has information about its type and data fields for game, state, action and player.
     *               It's not guaranteed that the data fields are different to null, so a check is necessary.
     */
    void onEvent(Event event);


    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     * <p>
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    void allGamesFinished();

    default boolean setOutputDirectory(String out, String time, String players) {return true;}


    void setGame(Game game);

    Game getGame();

    /**
     * Create listener based on given class, logger and metrics class. TODO: more than 1 metrics class
     * @param listenerClass - class of listener, full path (e.g. evaluation.metrics.MetricsGameListener)
     * @param logger - class of logger (e.g. evaluation.loggers.SummaryLogger)
     * @param metricsClass - class of metrics, full path (e.g. evaluation.metrics.GameMetrics)
     * @return - GameListener instance to be attached to a game
     */
    static IGameListener createListener(String listenerClass, IStatisticLogger logger, String metricsClass) {
        // first we check to see if listenerClass is a file or not
        IGameListener listener = null;
        ArrayList<AbstractMetric> metrics = new ArrayList<>();
        File listenerDetails = new File(listenerClass);
        if (listenerDetails.exists()) {
            // in this case we construct from file
            listener = Utils.loadClassFromFile(listenerClass);
        } else {
            if (metricsClass != null && !metricsClass.equals("")) {
                try {
                    String[] classPaths = metricsClass.split(",");
                    for (String c: classPaths) {
                        Class<?> clazz = Class.forName(c);
                        Constructor<?> constructor;
                        try {
                            constructor = clazz.getConstructor();
                            IMetricsCollection metricsInstance = (IMetricsCollection) constructor.newInstance();
                            metrics.addAll(Arrays.asList(metricsInstance.getAllMetrics()));
                        } catch (NoSuchMethodException e) {
                            return createListener(listenerClass);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if (!listenerClass.equals("")) {
                try {
                    Class<?> clazz = Class.forName(listenerClass);
                    Constructor<?> constructor;
                    try {
                        constructor = clazz.getConstructor(IStatisticLogger.class, AbstractMetric[].class);
                        listener = (IGameListener) constructor.newInstance(logger, metrics.toArray(new AbstractMetric[0]));
                    } catch (NoSuchMethodException e) {
                        return createListener(listenerClass);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(listener == null) {
            // default
            AbstractMetric[] ms = new GameMetrics().getAllMetrics();
            return new MetricsGameListener(logger, ms);
        }

        return listener;
    }


    /**
     * @return empty game listener given class, no logger, no metrics
     */
    static IGameListener createListener(String listenerClass) {
        IGameListener listener = null;
        try {
            Class<?> clazz = Class.forName(listenerClass);
            Constructor<?> constructor;
            constructor = clazz.getConstructor();
            listener = (IGameListener) constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(listener == null)
            return new MetricsGameListener(null, new AbstractMetric[0]);
        return listener;
    }

    default void reset() {}

    default void init(Game game) {}
}

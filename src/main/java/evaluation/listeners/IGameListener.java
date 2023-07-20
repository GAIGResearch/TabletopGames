package evaluation.listeners;

import core.Game;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameMetrics;
import evaluation.metrics.IMetricsCollection;
import org.apache.commons.lang3.reflect.ConstructorUtils;
import utilities.JSONUtils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;

public interface IGameListener {

    /**
     * Manages all events.
     *
     * @param event Event has information about its type and data fields for game, state, action and player.
     *              It's not guaranteed that the data fields are different to null, so a check is necessary.
     */
    void onEvent(Event event);


    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     * <p>
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    void report();

    default boolean setOutputDirectory(String... nestedDirectories) {
        return true;
    }

    void setGame(Game game);

    Game getGame();

    /**
     * Create listener based on given class, logger and metrics class. TODO: more than 1 metrics class
     *
     * @param listenerClass - class of listener, full path (e.g. evaluation.metrics.MetricsGameListener)
     * @param metricsClass  - class of metrics, full path (e.g. evaluation.metrics.GameMetrics)
     * @return - GameListener instance to be attached to a game
     */
    static IGameListener createListener(String listenerClass, String metricsClass) {
        // We must always have a listenerClas specified; a metrics class is optional
        if (listenerClass == null || listenerClass.isEmpty())
            throw new IllegalArgumentException("A listenerClass must be specified");
        // first we check to see if listenerClass is a file or not
        IGameListener listener = null;
        ArrayList<AbstractMetric> metrics = new ArrayList<>();
        File listenerDetails = new File(listenerClass);
        if (listenerDetails.exists()) {
            // in this case we construct from file
            listener = JSONUtils.loadClassFromFile(listenerClass);
        } else {
            // In this case we first check if we have a Metrics class
            // And if we do, we extract all the metrics from the class
            if (metricsClass != null && !metricsClass.isEmpty()) {
                try {
                    Class<?> clazz = Class.forName(metricsClass);
                    Constructor<?> constructor;
                    try {
                        constructor = clazz.getConstructor();
                        IMetricsCollection metricsInstance = (IMetricsCollection) constructor.newInstance();
                        metrics.addAll(Arrays.asList(metricsInstance.getAllMetrics()));
                    } catch (NoSuchMethodException e) {
                        // No constructor for MetricClass
                        System.out.println(metricsClass + " does not have an available constructor. Using the no-arg constructor for " + listenerClass + " instead.");
                        return createListener(listenerClass);
                    }
                } catch (Exception e) {
                    System.out.println("Failed to instantiate " + listenerClass + " using " + metricsClass + ": " + e.getMessage());
                }
            }
            // We can now go on to instantiate the listener
            AbstractMetric[] mArray = metrics.toArray(new AbstractMetric[0]);
            try {
                Class<?> clazz = Class.forName(listenerClass);
                Constructor<?> constructor;
                if (mArray.length > 0) {
                    try {
                        constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, AbstractMetric[].class);
                        listener = (IGameListener) constructor.newInstance((Object) mArray);
                    } catch (Exception e) {
                        try {
                            constructor = ConstructorUtils.getMatchingAccessibleConstructor(clazz, mArray.getClass());
                            listener = (IGameListener) constructor.newInstance((Object) mArray);
                        } catch (Exception e2) {
                            System.out.println("Failed to instantiate " + listenerClass + " with Metrics: " + metricsClass);
                            System.out.println("Defaulting to no-arg constructor for " + listenerClass);
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("Error occurred trying to instantiate listener " + listenerClass + ": " + e.getMessage() + " : " + e.toString());
            }
        }
        if (listener == null) {
            // default option if all else failed
            System.out.println("Unable to instantiate listener/metrics - so defaulting to MetricsGameListener(GameMetrics.class)");
            AbstractMetric[] ms = new GameMetrics().getAllMetrics();
            return new MetricsGameListener(ms);
        }
        return listener;
    }


    /**
     * @return empty game listener given class, no logger, no metrics
     */
    static IGameListener createListener(String listenerClass) {
        return createListener(listenerClass, "");
    }

    default void reset() {
    }

    default void init(Game game, int nPlayersPerGame, Set<String> playerNames) {}

}

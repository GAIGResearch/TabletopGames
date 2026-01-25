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

import static utilities.JSONUtils.loadClass;

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
     * @param listenerName - class of listener, full path (e.g. evaluation.metrics.MetricsGameListener)
     * @return - GameListener instance to be attached to a game
     */
    static IGameListener createListener(String listenerName) {
        // We must always have a listenerClas specified; a metrics class is optional
        if (listenerName == null || listenerName.isEmpty())
            throw new IllegalArgumentException("A listenerName must be specified");
        return loadClass(listenerName);
    }

    default void reset() {
    }

    default void init(Game game, int nPlayersPerGame, Set<String> playerNames) {}

}

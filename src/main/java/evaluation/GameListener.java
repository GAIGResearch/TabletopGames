package evaluation;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import evaluation.metrics.GameStatisticsListener;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
public class GameListener {

    //Default logger for the listener.
    protected IStatisticLogger logger;

    //List of metrics we are going to extract.
    HashMap<String, IGameMetric> metrics;

    public GameListener(IStatisticLogger logger, Pair<String, IGameMetric>[] metrics) {
        this.logger = logger;
        this.metrics = new HashMap<>();
        for(Pair<String, IGameMetric> p : metrics)
            this.metrics.put(p.a,p.b);
    }
    /**
     * Manages all events.
     * @param event  Event has information about its type and data fields for game, state, action and player.
     *               It's not guaranteed that the data fields are different to null, so a check is necessary.
     */
    public void onEvent(Event event) { }


    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     * <p>
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    public void allGamesFinished() {
        if (logger != null)
            logger.processDataAndFinish();
    }

    public IStatisticLogger getLogger() {
        return logger;
    }

    public static GameListener createListener(String listenerClass, IStatisticLogger logger) {
        // first we check to see if listenerClass is a file or not
        GameListener listener = null;
        File listenerDetails = new File(listenerClass);
        if (listenerDetails.exists()) {
            // in this case we construct from file
            listener = Utils.loadClassFromFile(listenerClass);
        } else {
            if (!listenerClass.equals("")) {
                try {
                    Class<?> clazz = Class.forName(listenerClass);
                    Constructor<?> constructor;
                    try {
                        constructor = clazz.getConstructor(IStatisticLogger.class);
                        listener = (GameListener) constructor.newInstance(logger);
                    } catch (NoSuchMethodException e) {
                        return createListener(listenerClass);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(listener == null) return new GameStatisticsListener(logger); //default

        return listener;
    }

    static GameListener createListener(String listenerClass) {
        GameListener listener = null;
        try {
            Class<?> clazz = Class.forName(listenerClass);
            Constructor<?> constructor;
            constructor = clazz.getConstructor();
            listener = (GameListener) constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(listener == null)
            return new GameStatisticsListener();
        return listener;
    }

    public static void main(String args[])
    {
        Utils.loadClassFromFile("data/metrics/loveletter.json");
    }


}

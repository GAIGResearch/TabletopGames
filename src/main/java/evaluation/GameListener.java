package evaluation;

import core.Game;
import core.interfaces.IGameMetric;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import evaluation.metrics.GameStatisticsListener;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
public class GameListener {

    //Default logger for the listener.
    protected IStatisticLogger logger;

    //List of metrics we are going to extract.
    HashMap<String, IGameMetric> metrics;

    //Game this listener listens to
    protected Game game;

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
    public void onEvent(Event event) {
        Map<String, Object> data = new TreeMap<>();
        for (String attrStr : metrics.keySet()) {
            data.put(attrStr, metrics.get(attrStr).get(this, event));
        }
        logger.record(data);
    }

    /**
     * Computes all the metrics passed in the array, triggered by the
     * event "e".
     * @param gameMetrics Set of metrics to be recorded.
     * @param e Event that triggered this recording process.
     * @param log True if the metrics are to be recorded into this listener's logger.
     */
    protected void getMetrics(IGameMetric[] gameMetrics, Event e, boolean log)
    {
        Map<String, Object> data = new TreeMap<>();
        for (IGameMetric gameMetric : gameMetrics) {
            if (gameMetric.listens(e.type)) {
                Object result = gameMetric.get(this, e);
                if (log)
                    data.put(gameMetric.name(), result);
            }
        }
        if(log)
            logger.record(data);
    }


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
    public void setGame(Game game) { this.game = game; }
    public Game getGame() { return game; }

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

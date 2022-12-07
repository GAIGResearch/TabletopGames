package evaluation.metrics;

import core.Game;
import core.interfaces.IStatisticLogger;
import utilities.Utils;
import evaluation.summarisers.TAGStatSummary;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.*;

public class GameListener {

    // One logger per event type for this listener
    protected HashMap<Event.GameEvent, IStatisticLogger> loggers;

    //List of metrics we are going to extract.
    protected HashMap<String, AbstractMetric> metrics;

    //Game this listener listens to
    protected Game game;

    public GameListener() {}

    public GameListener(IStatisticLogger logger, AbstractMetric[] metrics) {
        setup(logger);
        for (AbstractMetric m : metrics) {
            this.metrics.put(m.name(), m);
        }
    }
//
//    public GameListener(IStatisticLogger logger, IGameMetric[] metrics) {
//        setup(logger);
//        for (IGameMetric p : metrics) {
//            this.enumMetrics.put(p.name(), p);
//        }
//    }
//
//    public GameListener(IStatisticLogger logger, Pair<String, IGameMetric>[] metrics) {
//        setup(logger);
//        for (Pair<String, IGameMetric> p : metrics) {
//            this.enumMetrics.put(p.a, p.b);
//        }
//    }

    /**
     * Initializes loggers based on the type provided. Initializes metrics hashmap.
     * @param logger - instance of logger class to be used for this listener.
     */
    private void setup(IStatisticLogger logger) {
        this.loggers = new HashMap<>();
        for (Event.GameEvent event: Event.GameEvent.values()) {
            this.loggers.put(event, logger.emptyCopy(event.name()));
        }
        this.metrics = new HashMap<>();
    }

    /**
     * Manages all events.
     * @param event  Event has information about its type and data fields for game, state, action and player.
     *               It's not guaranteed that the data fields are different to null, so a check is necessary.
     */
    public void onEvent(Event event) {
        Map<String, Object> data = new TreeMap<>();
        for (String attrStr : metrics.keySet()) {
            AbstractMetric metric = metrics.get(attrStr);
            if (metric.listens(event.type)) {
                // Apply metric
                if (metric.isRecordedPerPlayer()) {
                    for (int i = 0; i < event.state.getNPlayers(); i++) {
                        // TODO: separate this data to be able to get per-player summaries?
                        event.playerID = i;
                        data.put(event.type + ":" + i + ":" + attrStr, metric.run(this, event));
                    }
                } else {
                    data.put(event.type + ":" + attrStr, metric.run(this, event));
                }
            }
        }
        loggers.get(event.type).record(data);

        // Process data from events recorded multiple times during game
        if (event.type == Event.GameEvent.GAME_OVER) {
            IStatisticLogger gameOverLogger = loggers.get(Event.GameEvent.GAME_OVER);
            if (gameOverLogger != null) {
                for (Event.GameEvent e : loggers.keySet()) {
                    if (!e.isOncePerGame()) {
                        // Summarise at the end
                        IStatisticLogger logger = loggers.get(e);
                        Map<String, TAGStatSummary> dataLogged = logger.summary();
                        for (String key : dataLogged.keySet()) {
                            TAGStatSummary dataLoggedKey = dataLogged.get(key);
                            processMetricGameOver(key, dataLoggedKey, gameOverLogger);
                        }
                    }
                }
            }
        }
    }

    protected void processMetricGameOver(String key, TAGStatSummary dataLogged, IStatisticLogger gameOverLogger) {
        gameOverLogger.record(dataLogged.getSummary());
    }

    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     * <p>
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    public void allGamesFinished() {
        if (loggers != null)
            for (IStatisticLogger logger: loggers.values()) {
                logger.processDataAndFinish();
            }
    }

    public HashMap<Event.GameEvent, IStatisticLogger> getLoggers() {
        return loggers;
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
        if(listener == null) return new GameListener(logger, new AbstractMetric[0]); //default

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
            return new GameListener(null, new AbstractMetric[0]);
        return listener;
    }

    public static void main(String args[])
    {
        Utils.loadClassFromFile("data/metrics/loveletter2.json");
    }

}

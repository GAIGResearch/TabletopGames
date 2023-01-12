package evaluation.metrics;

import core.AbstractGameState;
import core.Game;
import core.interfaces.IStatisticLogger;
import evaluation.summarisers.TAGNumericStatSummary;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import evaluation.summarisers.TAGTimeSeriesSummary;
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
        Map<String, TAGStatSummary> aggregators = new HashMap<>();

        for (String attrStr : metrics.keySet()) {
            AbstractMetric metric = metrics.get(attrStr);
            if (metric.listens(event.type)) {
                // Apply metric
                if (metric.isRecordedPerPlayer()) {
                    ArrayList<Object> metricResults = new ArrayList<>();
                    for (int i = 0; i < event.state.getNPlayers(); i++) {
                        event.playerID = i;
                        Object metricResult = metric.run(this, event);
                        if(metricResult != null){
                            metricResults.add(metricResult);
                            data.put(attrStr + ":" + i + ":" + event.type, metricResult);
                        }
                    }
                    //Aggregates per-player metrics for all players.
                    if(metric.aggregate())
                        aggregators.put(attrStr + ":All:" + event.type, aggregate(metricResults));
                }
                else
                {
                    Object metricResult = metric.run(this, event);
                    if(metricResult != null) data.put(attrStr + ":" + event.type, metricResult);
                }
            }
        }

        loggers.get(event.type).record(data);

        if(aggregators.size() > 0)
        {
            for(String k : aggregators.keySet())
            {
                TAGStatSummary ss = aggregators.get(k);
                if(ss.type == TAGStatSummary.StatType.Numeric)
                {
                    ArrayList<Double> aggData = ((TAGNumericStatSummary) ss).getElements();
                    for(Double d : aggData)
                        loggers.get(event.type).record(k, d);
                }else if(ss.type == TAGStatSummary.StatType.Occurrence)
                {
                    HashMap<Object, Integer> aggData = ((TAGOccurrenceStatSummary) ss).getElements();
                    for(Object o : aggData.keySet())
                        loggers.get(event.type).record(k, o);
                }else if(ss.type == TAGStatSummary.StatType.Time)
                {
                    ArrayList<TimeStamp> aggData = (ArrayList<TimeStamp>) ss.getElements();
                    for(TimeStamp t : aggData)
                        loggers.get(event.type).record(k, t);
                }
            }
        }

        // Process data from events recorded multiple times during game
        if (event.type == Event.GameEvent.GAME_OVER) {
            IStatisticLogger gameOverLogger = loggers.get(Event.GameEvent.GAME_OVER);
            if (gameOverLogger != null) {
                for (Event.GameEvent e : loggers.keySet()) {
                    if (!e.isOncePerGame()) {
                        // Summarise at the end
                        IStatisticLogger logger = loggers.get(e);
                        Map<String, TAGStatSummary> dataLogged = logger.summary();
                        ArrayList<String> keyDeletes = new ArrayList<>();
                        for (String key : dataLogged.keySet()) {
                            TAGStatSummary dataLoggedKey = dataLogged.get(key);
                            processMetricGameOver(metrics.get(key.split(":")[0]), event, dataLoggedKey, gameOverLogger);
//                            if(key.contains(":All:"))
                                keyDeletes.add(key);
                        }
                        for(String kDel : keyDeletes)
                            dataLogged.remove(kDel);
                        // TODO reset for next game?
                    }
                }
            }
        }
    }

    /**
     * @param metric - metric that has to post-process
     * @param event - event type where the metric was logged
     * @param dataLogged - data logged for this metric for this game
     * @param gameOverLogger - logger in which to record the summarised version of this data for one data point per game
     */
    protected void processMetricGameOver(AbstractMetric metric, Event event, TAGStatSummary dataLogged, IStatisticLogger gameOverLogger) {
//        gameOverLogger.record(dataLogged.getSummary());
        Map<String, Object> toRecord = metric.postProcessingGameOver(event, dataLogged);
        gameOverLogger.record(toRecord);
    }

    private TAGStatSummary aggregate(ArrayList<Object> metricsData)
    {
        if(metricsData.size() > 0) {
            if (metricsData.get(0) instanceof Number) {
                TAGNumericStatSummary ss = new TAGNumericStatSummary();
                for (Object metricsDatum : metricsData) {
                    if (metricsDatum instanceof Integer) ss.add((Integer) metricsDatum);
                    if (metricsDatum instanceof Double) ss.add((Double) metricsDatum);
                }
                return ss;
            }else if (metricsData.get(0) instanceof String) {
                TAGOccurrenceStatSummary ss = new TAGOccurrenceStatSummary();
                for (Object metricsDatum : metricsData)
                    ss.add(metricsDatum);
                return ss;
            }else if (metricsData.get(0) instanceof TimeStamp) {
                //This is a time series.
                TAGTimeSeriesSummary ss = new TAGTimeSeriesSummary();
                for (Object metricsDatum : metricsData) {
                    TimeStamp timeStamp = (TimeStamp) metricsDatum;
                    ss.append(timeStamp.x, timeStamp.v);
                }
                return ss;
            }
        }
        return null;
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

    public static GameListener createListener(String listenerClass, IStatisticLogger logger, String metricsClass) {
        // first we check to see if listenerClass is a file or not
        GameListener listener = null;
        AbstractMetric[] metrics = null;
        File listenerDetails = new File(listenerClass);
        if (listenerDetails.exists()) {
            // in this case we construct from file
            listener = Utils.loadClassFromFile(listenerClass);
        } else {
            if (!metricsClass.equals("")) {
                try {
                    Class<?> clazz = Class.forName(metricsClass);
                    Constructor<?> constructor;
                    try {
                        constructor = clazz.getConstructor();
                        IMetricsCollection metricsInstance = (IMetricsCollection) constructor.newInstance();
                        metrics = metricsInstance.getAllMetrics();
                    } catch (NoSuchMethodException e) {
                        return createListener(listenerClass);
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
                        listener = (GameListener) constructor.newInstance(logger, metrics);
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
            metrics = new GameMetrics().getAllMetrics();
            return new GameListener(logger, metrics);
        }

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
        Utils.loadClassFromFile("data/metrics/loveletter.json");
    }

}

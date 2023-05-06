package evaluation.listeners;

import core.Game;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import evaluation.summarisers.TAGNumericStatSummary;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import evaluation.summarisers.TAGStatSummary;
import evaluation.summarisers.TAGTimeSeriesSummary;
import utilities.TimeStamp;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;

import static evaluation.metrics.AbstractMetric.ReportDestination.ToConsole;
import static evaluation.metrics.AbstractMetric.ReportType.Summary;

/**
 * Main Game Listener class. An instance can be attached to a game, which will then cause registered metrics in this
 * class to record data about the game when specific game events occur, see {@link evaluation.metrics.Event.GameEvent}.
 * ---
 * Subclasses can be implemented for custom functionality, but they are not necessary. All that is necessary is to
 * set up a metrics class that implements the interface {@link IMetricsCollection}, check this for more information.
 * See {@link games.sushigo.metrics.SushiGoMetrics} for an example of a metric collection.
 * See {@link games.terraformingmars.stats.TMStatsVisualiser} for an example of a visualiser of metrics.
 */
public class MetricsGameListener implements IGameListener {

    // One logger per event type for this listener
    protected Map<Event.GameEvent, IStatisticLogger> loggers;

    // List of metrics we are going to extract.
    protected Map<String, AbstractMetric> metrics;

    protected Set<Event.GameEvent> eventsOfInterest = new HashSet<>();

    // Game this listener listens to
    protected Game game;

    public MetricsGameListener() {}
    public MetricsGameListener(IStatisticLogger logger, AbstractMetric[] metrics) {
        this.metrics = new LinkedHashMap<>();
        this.loggers = new HashMap<>();
        for (AbstractMetric m : metrics) {
            this.metrics.put(m.getName(), m);
            eventsOfInterest.addAll(m.getEventTypes());
        }
//        for (Event.GameEvent event: eventsOfInterest) {
//            this.loggers.put(event, logger.emptyCopy(event.name()));
//        }
    }

    /**
     * Manages all events.
     * @param event  Event has information about its type and data fields for game, state, action and player.
     *               It's not guaranteed that the data fields are different to null, so a check is necessary.
     */
    public void onEvent(Event event) {
        if (!eventsOfInterest.contains(event.type))
            return;

        // Use of LinkedHashMap so that data is stored in the same order it is listed in the json config file

        for (String attrStr : metrics.keySet()) {
            AbstractMetric metric = metrics.get(attrStr);
            if (metric.listens(event.type)) {
                // Apply metric
                metric.run(this, event);
            }
        }

        // Process data from events recorded multiple times during game
//        if (event.type == Event.GameEvent.GAME_OVER) {
//            IStatisticLogger gameOverLogger = loggers.get(Event.GameEvent.GAME_OVER);
//            if (gameOverLogger != null) {
//                for (Event.GameEvent e : loggers.keySet()) {
//                    if (!e.isOncePerGame()) {
//                        // Summarise at the end
//                        IStatisticLogger logger = loggers.get(e);
//                        Map<String, TAGStatSummary> dataLogged = logger.summary();
//                        ArrayList<String> keyDeletes = new ArrayList<>();
//                        for (String key : dataLogged.keySet()) {
//                            TAGStatSummary dataLoggedKey = dataLogged.get(key);
//                            processMetricGameOver(metrics.get(key.split(":")[0]), event, dataLoggedKey, gameOverLogger);
////                            if(key.contains(":All:"))
//                                keyDeletes.add(key);
//                        }
//                        for(String kDel : keyDeletes)
//                            dataLogged.remove(kDel);
//                        // TODO Check correct reset for next game?
//                    }
//                }
//            }
//        }
    }

    /**
     * @param metric - metric that has to post-process
     * @param event - event type where the metric was logged
     * @param dataLogged - data logged for this metric for this game
     * @param gameOverLogger - logger in which to record the summarised version of this data for one data point per game
     */
    protected void processMetricGameOver(AbstractMetric metric, Event event, TAGStatSummary dataLogged, IStatisticLogger gameOverLogger) {
//        gameOverLogger.record(dataLogged.getSummary());
        Map<String, Object> toRecord;
        if (metric != null) {
            toRecord = metric.postProcessingGameOver(event, dataLogged);
        } else {
            // Default post-processing
            toRecord = new HashMap<>();
            Map<String, Object> summaryData = dataLogged.getSummary();
            for (String k: summaryData.keySet()) {
                toRecord.put(getClass().getSimpleName() + "(" + k + ")" + ":" + event.type, summaryData.get(k));
            }
        }
        gameOverLogger.record(toRecord);
    }

    private TAGStatSummary aggregate(ArrayList<Object> metricsData)
    {
        if(metricsData.size() > 0) {
            if (metricsData.get(0) instanceof Number) {  // TODO might want this as occurrence stat summary instead
                TAGNumericStatSummary ss = new TAGNumericStatSummary();
                for (Object metricsDatum : metricsData) {
                    if (metricsDatum instanceof Integer) ss.add((Integer) metricsDatum);
                    if (metricsDatum instanceof Double) ss.add((Double) metricsDatum);
                }
                return ss;
            }else if (metricsData.get(0) instanceof TimeStamp) {
                //This is a time series.
                TAGTimeSeriesSummary ss = new TAGTimeSeriesSummary();
                for (Object metricsDatum : metricsData) {
                    TimeStamp timeStamp = (TimeStamp) metricsDatum;
                    ss.append(timeStamp.x, timeStamp.v);
                }
                return ss;
            } else {
                TAGOccurrenceStatSummary ss = new TAGOccurrenceStatSummary();
                for (Object metricsDatum : metricsData)
                    ss.add(metricsDatum);
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
        List<AbstractMetric.ReportType> reportTypes = Arrays.asList(Summary);
        List<AbstractMetric.ReportDestination> reportDestinations = Arrays.asList(ToConsole);

        boolean success = true;
        String folderName = "metrics/out/" + game.getGameType().name() + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        if (reportDestinations.contains(AbstractMetric.ReportDestination.ToFile)) {
            // Create a folder for all files to be put in, with the game name and current timestamp
            File folder = new File(folderName);
            if (!folder.exists()) {
                success = folder.mkdir();
            }
        }

        // All metrics report themselves
        if (success) {
            for (AbstractMetric metric : metrics.values()) {
                metric.allGamesFinished(folderName,
                        reportTypes,
                        reportDestinations);
            }
        }

//        if (loggers != null)
//            for (IStatisticLogger logger: loggers.values()) {
//                logger.processDataAndFinish();
//            }
    }

    /* Getters, setters */
    public final Map<Event.GameEvent, IStatisticLogger> getLoggers() {
        return loggers;
    }
    public final void setGame(Game game) {
        this.game = game;
    }
    public final Game getGame() { return game; }

    @Override
    public void init(Game game) {
        this.game = game;

        for (AbstractMetric metric : metrics.values()) {
            metric.init(game);
        }
    }
}

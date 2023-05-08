package evaluation.listeners;

import core.Game;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IDataLogger;
import evaluation.metrics.IMetricsCollection;
import evaluation.metrics.tablessaw.DataTableSaw;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import static evaluation.metrics.IDataLogger.ReportDestination.*;
import static evaluation.metrics.IDataLogger.ReportType.*;

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

    List<IDataLogger.ReportType> reportTypes = Arrays.asList(Summary);  //todo this needs to be read from JSON

    List<IDataLogger.ReportDestination> reportDestinations = Arrays.asList(ToFile); //todo this needs to be read from JSON

    String destFolder = "metrics/out/"; //todo this needs to be read from JSON

    public MetricsGameListener() {}
    public MetricsGameListener(IStatisticLogger logger, AbstractMetric[] metrics) {
        this.metrics = new LinkedHashMap<>();
        this.loggers = new HashMap<>();
        for (AbstractMetric m : metrics) {
            m.setDataLogger(new DataTableSaw(m)); //todo this logger needs to be read from JSON
            this.metrics.put(m.getName(), m);
            eventsOfInterest.addAll(m.getEventTypes());
        }
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

            if(event.type == Event.GameEvent.GAME_OVER)
                metric.notifyGameOver();
        }

    }

    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     * <p>
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    public void allGamesFinished() {

        boolean success = true;

        // If the "metrics/out/" does not exist, create it
        File outFolder = new File(destFolder);
        if (!outFolder.exists()) {
            success = outFolder.mkdir();
        }

        String folderName = destFolder + game.getGameType().name() + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
        if (reportDestinations.contains(ToFile)) {
            // Create a folder for all files to be put in, with the game name and current timestamp
            File folder = new File(folderName);
            if (!folder.exists()) {
                success = folder.mkdir();
            }
        }

        // All metrics report themselves
        if (success) {
            for (AbstractMetric metric : metrics.values()) {
                metric.processFinishedGames(folderName,
                        reportTypes,
                        reportDestinations);
            }
        }

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

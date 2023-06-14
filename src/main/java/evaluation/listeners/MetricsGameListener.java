package evaluation.listeners;

import core.Game;
import evaluation.metrics.*;
import evaluation.metrics.tablessaw.DataTableSaw;
import shapeless.ops.nat;

import java.io.File;
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

    // List of metrics we are going to extract.
    protected Map<String, AbstractMetric> metrics;

    // Events the metrics in this listener respond to. Game over is always added.
    protected Set<Event.GameEvent> eventsOfInterest = new HashSet<>();

    // Game this listener listens to
    protected Game game;

    // Types of reports to generate: RawData, Summary, Plot
    List<IDataLogger.ReportType> reportTypes = new ArrayList<>();

    // Where to send the reports: ToConsole, ToFile or ToBoth
    List<IDataLogger.ReportDestination> reportDestinations;

    // Destination directory for the reports
    String destDir = "metrics/out/"; //by default

    public MetricsGameListener() {
    }

    public MetricsGameListener(AbstractMetric[] metrics) {
        this(ToBoth, metrics);
    }

    public MetricsGameListener(IDataLogger.ReportDestination logTo, AbstractMetric[] metrics) {
        this(logTo, new IDataLogger.ReportType[]{RawData, Summary, Plot}, metrics);
    }

    public MetricsGameListener(IDataLogger.ReportDestination logTo, IDataLogger.ReportType[] dataTypes, AbstractMetric[] metrics) {
        reportDestinations = Collections.singletonList(logTo);
        this.reportTypes = Arrays.asList(dataTypes);
        this.metrics = new LinkedHashMap<>();
        for (AbstractMetric m : metrics) {
            m.setDataLogger(new DataTableSaw(m)); //todo this logger needs to be read from JSON
            this.metrics.put(m.getName(), m);
            eventsOfInterest.addAll(m.getEventTypes());
        }
        eventsOfInterest.add(Event.GameEvent.GAME_OVER);
    }

    /**
     * Manages all events.
     *
     * @param event Event has information about its type and data fields for game, state, action and player.
     *              It's not guaranteed that the data fields are different to null, so a check is necessary.
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

            if (event.type == Event.GameEvent.GAME_OVER)
                metric.notifyGameOver();
        }
    }

    @Override
    public boolean setOutputDirectory(String... nestedDirectories) {

        boolean success = true;

        if (reportDestinations.contains(ToFile) || reportDestinations.contains(ToBoth)) {
            // If the "metrics/out/" does not exist, create it
            String folder = "";
            for (String nestedDir : nestedDirectories) {
                folder = folder + nestedDir + File.separator;
                File outFolder = new File(folder);
                if (!outFolder.exists()) {
                    success = outFolder.mkdir();
                }
                if (!success)
                    throw new AssertionError("Unable to create output directory" + outFolder.getAbsolutePath());
            }

            destDir = new File(folder).getAbsolutePath() + File.separator;
        }
        return success;
    }

    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     * <p>
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    public void allGamesFinished() {
        boolean success = true;

        if (reportDestinations.contains(ToFile) || reportDestinations.contains(ToBoth)) {
            // Create a folder for all files to be put in, with the game name and current timestamp
            File folder = new File(destDir);
            if (!folder.exists()) {
                success = folder.mkdir();
            }
        }

        // All metrics report themselves
        if (success) {
            for (AbstractMetric metric : metrics.values()) {
                metric.processFinishedGames(destDir, reportTypes, reportDestinations);
            }

            // We also create raw data files for groups of metrics responding to the same event
            for (Event.GameEvent event : eventsOfInterest) {
                List<AbstractMetric> eventMetrics = new ArrayList<>();
                for (AbstractMetric metric : metrics.values()) {
                    if (metric.listens(event)) {
                        eventMetrics.add(metric);
                    }
                }
                if (eventMetrics.size() > 1) {
                    IDataLogger dataLogger = new DataTableSaw(eventMetrics, event, eventToIndexingColumn(event));
                    dataLogger.getDefaultProcessor().processRawDataToFile(dataLogger, destDir);
                }
            }
        }
    }

    private String eventToIndexingColumn(Event.GameEvent e) {
        switch (e) {
            case ABOUT_TO_START:
            case GAME_OVER:
                return "GameID";
            case ROUND_OVER:
                return "Round";
            case TURN_OVER:
                return "Turn";
            case ACTION_CHOSEN:
            case ACTION_TAKEN:
            case GAME_EVENT:
                return "Tick";
        }
        return null;
    }

    /* Getters, setters */
    public final void setGame(Game game) {
        this.game = game;
    }

    public final Game getGame() {
        return game;
    }

    public void reset() {
        for (AbstractMetric metric : metrics.values()) {
            metric.reset();
        }
    }

    @Override
    public void init(Game game) {
        this.game = game;

        for (AbstractMetric metric : metrics.values()) {
            metric.init(game);
        }
    }
}

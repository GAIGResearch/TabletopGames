package evaluation.metrics;
import evaluation.listeners.GameListener;
import evaluation.summarisers.TAGStatSummary;

import java.util.*;

public abstract class AbstractMetric
{
    // Set of event types this metric listens to, to record data when they occur
    private final Set<Event.GameEvent> eventTypes;

    public AbstractMetric() {
        this.eventTypes = getEventTypes();
    }

    /**
     * @return a data point recorded by this metric, given:
     * @param listener - game listener object, with access to the game itself and loggers
     * @param e - event, including game event type, state, action and player ID (if these properties are relevant, they may not be set depending on event type)
     */
    public abstract Object run(GameListener listener, Event e);

    /**
     * @return set of game events this metric should record information for.
     */
    public abstract Set<Event.GameEvent> getEventTypes();

    /**
     * @return true if metric recorded for each player independently. Overwrite in subclass if true.
     */
    public boolean isRecordedPerPlayer() {
        return false;
    }

    /**
     * @return true if metric gets aggregated for all players. Overwrite in subclass if false.
     */
    public boolean isAggregate() {
        return true;
    }

    /**
     * Some post-processing of metric data at the end of a game (used by metrics that record data at several points throughout the game).
     * @return a summary of the data recorded during the game by the metric (to have a single data point per game).
     * @param e - game over event, including state
     * @param recordedData - data recorded by this metric during the game
     */
    public Map<String, Object> postProcessingGameOver(Event e, TAGStatSummary recordedData) {
        // Process the recorded data during the game and return game over summarised data
        Map<String, Object> toRecord = new HashMap<>();
        Map<String, Object> summaryData = recordedData.getSummary();
        for (String k: summaryData.keySet()) {
            toRecord.put(getClass().getSimpleName() + "(" + k + ")" + ":" + e.type, summaryData.get(k));
        }
        return toRecord;
    }

    /* Final methods, not to be overridden */

    /**
     * Standard name for this metric, using the class name. If parameterized metric, different format applies.
     */
    public final String getName() {
        if (this instanceof AbstractParameterizedMetric)
            return ((AbstractParameterizedMetric)this).name();
        return this.getClass().getSimpleName();
    }

    /**
     * @return true if this metric listens to the given game event type, false otherwise.
     */
    public final boolean listens(Event.GameEvent eventType)
    {
        //by default, we listen to all types of events.
        if(eventTypes == null) return true;
        return eventTypes.contains(eventType);
    }
}

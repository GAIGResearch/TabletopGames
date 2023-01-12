package evaluation.metrics;
import evaluation.summarisers.TAGOccurrenceStatSummary;
import evaluation.summarisers.TAGStatSummary;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard;
import utilities.Pair;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public abstract class AbstractMetric
{
    private HashSet<Event.GameEvent> eventTypes;

    protected boolean recordPerPlayer = false;

    protected boolean aggregate = true;

    public abstract Object run(GameListener listener, Event e);

    public final boolean listens(Event.GameEvent eventType)
    {
        //by default, we listen to all types of events.
        if(eventTypes == null) return true;
        return eventTypes.contains(eventType);
    }

    public final void addEventType(Event.GameEvent eventType)
    {
        if(eventTypes == null)
            eventTypes = new HashSet<>();
        eventTypes.add(eventType);
    }

    public boolean isRecordedPerPlayer() {
        return recordPerPlayer;
    }

    public boolean aggregate() {return aggregate;}

    public String name()
    {
        return this.getClass().getSimpleName();
    }

    public Object[] getAllowedParameters() { return new String[0];}

    public Map<String, Object> postProcessingGameOver(Event e, TAGStatSummary recordedData) {
        // Process the recorded data during the game and return game over summarised data
        Map<String, Object> toRecord = new HashMap<>();
        Map<String, Object> summaryData = recordedData.getSummary();
        for (String k: summaryData.keySet()) {
            toRecord.put(name() + "(" + k + ")" + ":" + e.type, summaryData.get(k));
        }
        return toRecord;
    }
}

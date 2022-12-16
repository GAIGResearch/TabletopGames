package evaluation.metrics;
import java.util.HashSet;
public abstract class AbstractMetric
{
//    public String name;
    private HashSet<Event.GameEvent> eventTypes;

    protected boolean recordPerPlayer = false;

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

    public String name()
    {
        return this.getClass().getSimpleName();
    }

    public Object[] getAllowedParameters() { return new String[0];}
}

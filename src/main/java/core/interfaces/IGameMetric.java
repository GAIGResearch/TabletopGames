package core.interfaces;

import evaluation.GameListener;
import evaluation.metrics.Event;

public interface IGameMetric {

    /**
     * A simple interface to gather data on game-specific attributes
     * @param listener Object that gathers data for a particular purpose
     * @param event Event that has been create with the type and data.
     * @return the metric computed, to be stored elsewhere
     */
    default Object get(GameListener listener, Event event) {
        return 0;
    }

    /**
     * Indicates if this metric must do something when receiving an event of the specified type
     * @param eventType Type to maybe listen to
     * @return true if this metric should be executed when an eventType event is received.
     */
    default boolean listens(Event.GameEvent eventType) { return true; }


    // Returns the name of the metric
    String name();

}

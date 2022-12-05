package core.interfaces;

import evaluation.metrics.GameListener;
import evaluation.metrics.Event;

import java.util.HashSet;

public interface IGameMetric {

    /**
     * A simple interface to gather data on game-specific attributes
     * @param listener Object that gathers data for a particular purpose
     * @param event Event that has been create with the type and data.
     * @return the metric computed, to be stored elsewhere
     */
    Object get(GameListener listener, Event event);

    /**
     * Indicates if this metric must do something when receiving an event of the specified type
     * @param eventType Type to maybe listen to
     * @return true if this metric should be executed when an eventType event is received.
     */
    boolean listens(Event.GameEvent eventType);

    /**
     * Indicates if the metric should record one value per player
     * @return true if metric should be executed once for each player, or just once per event it listens to
     */
    boolean isRecordedPerPlayer();

    // Returns the name of the metric
    String name();

}

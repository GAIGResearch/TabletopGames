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

    // Returns the name of the metric
    String name();

}

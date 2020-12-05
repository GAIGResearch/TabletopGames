package core.interfaces;

import com.sun.org.apache.xpath.internal.operations.Bool;
import core.AbstractGameState;

public interface IGameAttribute<T> {

    /**
     * A simple interface to gather data on game-specific attributes
     * Mostly used for logging of game trajectories
     *
     * @param state The game state
     * @return The value of whatever the attribute is in this state
     */
    T get(AbstractGameState state);

    default String getAsString(AbstractGameState state) {
        T value = get(state);
        if (value instanceof Double) {
            return String.format("%.4g", value);
        }
        if (value instanceof Integer) {
            return String.format("%d", value);
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? "1" : "0";
        }
        return value.toString();
    }

    String name();

}

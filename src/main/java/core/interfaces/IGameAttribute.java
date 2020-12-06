package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface IGameAttribute {

    /**
     * A simple interface to gather data on game-specific attributes
     * Mostly used for logging of game trajectories
     *
     * @param state The game state
     * @return The value of whatever the attribute is in this state
     */
    Object get(AbstractGameState state, AbstractAction action);

    default String getAsString(AbstractGameState state, AbstractAction action) {
        Object value = get(state, action);
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

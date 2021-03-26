package core.interfaces;

import core.AbstractGameState;
import core.actions.AbstractAction;

public interface IActionAttribute<T> {

    /**
     * A simple interface to gather data on action-specific attributes
     * Mostly used for logging of game trajectories
     *
     * @param state The game state
     * @param action The action about to be applied to the game state
     * @return The value of the relevant attribute
     */
    T get(AbstractGameState state, AbstractAction action);

    String name();

}

package core.interfaces;

import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;

public interface IGameAttribute {

    /**
     * A simple interface to gather data on game-specific attributes
     * Mostly used for logging of game trajectories
     *
     * @param state The game state
     * @return The value of whatever the attribute is in this state
     */
    default Object get(AbstractGameState state, AbstractAction action) {
        return 0;
    }

    /**
     *
     * @param state The game state
     * @param player The player for whom we are reporting
     * @return
     */
    default Object get(AbstractGameState state, int player) {
        return 0;
    }

    default Object get(Game game) { return 0; }

    String name();

}

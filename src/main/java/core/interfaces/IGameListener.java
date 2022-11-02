package core.interfaces;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import utilities.GameStatisticsListener;
import utilities.Utils;

import java.io.File;
import java.lang.reflect.Constructor;

public interface IGameListener {

    /**
     * This is used to register Game Start and Game Over events
     *
     * It provides a link to the Game, from which the state can be obtained, or
     * details of the players. Any changes to the State will then apply in the game.
     *
     * @param type Either ABOUT_TO_START or GAME_OVER
     * @param game The Game
     */
    void onGameEvent(CoreConstants.GameEvents type, Game game);

    /**
     * Registers all other event types.
     * The state will always be provided, but action will be null except for ACTION_CHOSEN events
     *
     * The state provided is *deliberately* not a copy, but the actual state to avoid performance overheads
     * Hence it is *vital* that any implementation of this method only reads data from the state
     * and does not modify it!
     *
     * @param type   The GameEvent
     * @param state  The current Game state
     * @param action The Action that have just been Chosen (if relevant; else null)
     */
    // for all other event types
    void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action);

    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     *
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    default void allGamesFinished() {
        // default is to do nothing

    }


    static IGameListener createListener(String listenerClass, IStatisticLogger logger) {
        // first we check to see if listenerClass is a file or not
        IGameListener listener = new GameStatisticsListener(logger);
        File listenerDetails = new File(listenerClass);
        if (listenerDetails.exists()) {
            // in this case we construct from file
            listener = Utils.loadClassFromFile(listenerClass);
        } else {
            if (!listenerClass.equals("")) {
                try {
                    Class<?> clazz = Class.forName(listenerClass);
                    Constructor<?> constructor;
                    try {
                        constructor = clazz.getConstructor(IStatisticLogger.class);
                        listener = (IGameListener) constructor.newInstance(logger);
                    } catch (NoSuchMethodException e) {
                        return createListener(listenerClass);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return listener;
    }

    static IGameListener createListener(String listenerClass) {
        IGameListener listener = new GameStatisticsListener();
        try {
            Class<?> clazz = Class.forName(listenerClass);
            Constructor<?> constructor;
            constructor = clazz.getConstructor();
            listener = (IGameListener) constructor.newInstance();

        } catch (Exception e) {
            e.printStackTrace();
        }
        return listener;
    }
}

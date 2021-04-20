package core.interfaces;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import utilities.GameReportListener;

import java.lang.reflect.Constructor;

public interface IGameListener {

    /**
     * This is used to register Game Start and Game Over events
     *
     * @param type Either ABOUT_TO_START or GAME_OVER
     * @param game The Game
     */
    void onGameEvent(CoreConstants.GameEvents type, Game game);

    /**
     * Registers all other event types.
     * The state will always be provided, but action will be null except for ACTION_CHOSEN events
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
        IGameListener listener = new GameReportListener(logger);
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
        return listener;
    }

    static IGameListener createListener(String listenerClass) {
        IGameListener listener = new GameReportListener();
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

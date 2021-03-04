package core.interfaces;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import utilities.GameReportListener;

import java.lang.reflect.Constructor;
import java.util.Map;

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
     * IGameListener is responsible for collecting data.
     * IStatisticsLogger is then used to store or further process data.
     * This method is the usual means of communicating between them, and complements IStatisticsLogger.record()
     *
     * @return A Map of all the data collected by the IGameListener so far
     */
    Map<String, Object> getAllData();

    /**
     * This clears out all data collected so far and resets the GameListener.
     * This may be used if one Listener listens to several games and we do not want to merge data extracted across games
     */
    void clear();

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

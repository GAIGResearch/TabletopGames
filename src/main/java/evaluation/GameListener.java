package evaluation;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import core.interfaces.IStatisticLogger;
import utilities.GameStatisticsListener;
import utilities.Pair;
import utilities.Utils;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
public class GameListener {

    //Default logger for the listener.
    protected IStatisticLogger logger;

    //List of metrics we are going to extract.
    HashMap<String, IGameMetric> metrics;

    public GameListener(IStatisticLogger logger, Pair<String, IGameMetric>[] metrics) {
        this.logger = logger;
        this.metrics = new HashMap<>();
        for(Pair<String, IGameMetric> p : metrics)
            this.metrics.put(p.a,p.b);


    }
    /**
     * This is used to register Game Start and Game Over events
     * <p>
     * It provides a link to the Game, from which the state can be obtained, or
     * details of the players. Any changes to the State will then apply in the game.
     *
     * @param type Either ABOUT_TO_START or GAME_OVER
     * @param game The Game
     */
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            AbstractGameState state = game.getGameState();
            Map<String, Object> data = new TreeMap<>();
            for (String attrStr : metrics.keySet()) {
                switch (metrics.get(attrStr).getType())
                {
                    case STATE_ACTION: data.put(attrStr, metrics.get(attrStr).get(state, null));
                        break;
                    case STATE_PLAYER: data.put(attrStr, metrics.get(attrStr).get(state, -1));
                        break;
                    case GAME: data.put(attrStr, metrics.get(attrStr).get(game));
                        break;
                }
            }
            logger.record(data);
        }
    }
    /**
     * Registers all other event types.
     * The state will always be provided, but action will be null except for ACTION_CHOSEN events
     * <p>
     * The state provided is *deliberately* not a copy, but the actual state to avoid performance overheads
     * Hence it is *vital* that any implementation of this method only reads data from the state
     * and does not modify it!
     *
     * @param type   The GameEvent
     * @param state  The current Game state
     * @param action The Action that have just been Chosen (if relevant; else null)
     */
    // for all other event types
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
    }
    /**
     * This is called when all processing is finished, for example after running a sequence of games
     * As such, no state is provided.
     * <p>
     * This is useful for Listeners that are just interested in aggregate data across many runs
     */
    public void allGamesFinished() {
        if (logger != null)
            logger.processDataAndFinish();
    }
    public IStatisticLogger getLogger() {
        return logger;
    }
    public static GameListener createListener(String listenerClass, IStatisticLogger logger) {
        // first we check to see if listenerClass is a file or not
        GameListener listener = null;
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
                        listener = (GameListener) constructor.newInstance(logger);
                    } catch (NoSuchMethodException e) {
                        return createListener(listenerClass);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(listener == null) return new GameStatisticsListener(logger); //default

        return listener;
    }
    static GameListener createListener(String listenerClass) {
        GameListener listener = null;
        try {
            Class<?> clazz = Class.forName(listenerClass);
            Constructor<?> constructor;
            constructor = clazz.getConstructor();
            listener = (GameListener) constructor.newInstance();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(listener == null)
            return new GameStatisticsListener();
        return listener;
    }
    public static void main(String args[])
    {
        Utils.loadClassFromFile("data/metrics/loveletter.json");
    }


}

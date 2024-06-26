package evaluation.metrics;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.LogEvent;
import core.interfaces.IGameEvent;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Event
{
    public enum GameEvent implements IGameEvent {
        ABOUT_TO_START(true), GAME_OVER(true),
        ROUND_OVER(false), TURN_OVER(false),
        ACTION_CHOSEN(false),
        ACTION_TAKEN(false),
        GAME_EVENT(false);
        // Mostly self-explanatory, except:
        // GAME_EVENT is some game-specific event that is worthy of notice and is not linked directly to a player action
        //     An example might be the draw of a Epidemic card in Pandemic
        // ACTION_CHOSEN is triggered immediately after a decision is made, but before it is implemented.
        //     Hence it contains the Game State used to make the decision - this is important in Expert Iteration for example.
        // ACTION_TAKEN is triggered after a decision is implemented. The state hence contains the results of the action.
        //     This is useful if we want to update a GUI or similar.

        final boolean oncePerGame;
        GameEvent(boolean oncePerGame) {this.oncePerGame = oncePerGame;}
        public boolean isOncePerGame() {
            return oncePerGame;
        }

        @Override
        public Set<IGameEvent> getValues() {
            return new HashSet<>(Arrays.asList(GameEvent.values()));
        }
    }

    private Event() {}

    public IGameEvent type;
    public AbstractGameState state;
    public AbstractAction action;
    public int playerID;

    public static Event createEvent(IGameEvent type,
                                    AbstractGameState gameState,
                                    AbstractAction action,
                                    int playerID)
    {
        Event e = new Event();
        e.type = type;
        e.state = gameState;
        e.action = action;
        e.playerID = playerID;
        return e;
    }

    public static Event createEvent(IGameEvent type)
    {
        return Event.createEvent(type, null, null, -1);
    }

    public static Event createEvent(IGameEvent type, AbstractGameState state)
    {
        return Event.createEvent(type, state, null, -1);
    }

    public static Event createEvent(IGameEvent type, AbstractGameState state, LogEvent action)
    {
        return Event.createEvent(type, state, action, -1);
    }

    public static Event createEvent(IGameEvent type, AbstractGameState state, int playerID)
    {
        return Event.createEvent(type, state, null, playerID);
    }
}

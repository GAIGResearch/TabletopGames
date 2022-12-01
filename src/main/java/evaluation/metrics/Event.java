package evaluation.metrics;
import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;

public class Event
{
    public enum GameEvent {
        ABOUT_TO_START, GAME_OVER,
        ROUND_OVER, TURN_OVER,
        ACTION_CHOSEN,
        ACTION_TAKEN,
        GAME_EVENT
        // Mostly self-explanatory, except:
        // GAME_EVENT is some game-specific event that is worthy of notice and is not linked directly to a player action
        //     An example might be the draw of a Epidemic card in Pandemic
        // ACTION_CHOSEN is triggered immediately after a decision is made, but before it is implemented.
        //     Hence it contains the Game State used to make the decision - this is important in Expert Iteration for example.
        // ACTION_TAKEN is triggered after a decision is implemented. The state hence contains the results of the action.
        //     This is useful if we want to update a GUI or similar.
    }

    public GameEvent type;
    public Game game;
    public AbstractGameState state;
    public AbstractAction action;
    public int playerID;


    public static Event createEvent(GameEvent type,
                                    Game game,
                                    AbstractGameState gameState,
                                    AbstractAction action,
                                    int playerID)
    {
        Event e = new Event();
        e.type = type;
        e.game = game;
        e.state = gameState;
        e.action = action;
        e.playerID = playerID;
        return e;
    }


    public static Event createEvent(GameEvent type, Game game)
    {
        return Event.createEvent(type, game, null, null, -1);
    }

    public static Event createEvent(GameEvent type, Game game, AbstractAction action)
    {
        return Event.createEvent(type, game, null, action, -1);
    }

    public static Event createEvent(GameEvent type, AbstractGameState state)
    {
        return Event.createEvent(type, null, state, null, -1);
    }

    public static Event createEvent(GameEvent type, AbstractGameState state, AbstractAction action)
    {
        return Event.createEvent(type, null, state, action, -1);
    }

    public static Event createEvent(GameEvent type, AbstractGameState state, int playerID)
    {
        return Event.createEvent(type, null, state, null, playerID);
    }
}

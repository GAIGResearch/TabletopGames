package evaluation.metrics;

import core.interfaces.IGameMetric;

import java.util.function.BiFunction;

/**
 * This is a stripped down set of IGameAttributes that is generic to any game
 * and is the default set used by ActionListener to generate a game log
 *
 * When working on a specific game it is advisable to use this as a starting point, and
 * add in relevant game-specific items of data
 */
public enum ActionSimpleAttributes implements IGameMetric {

    GAME_ID((l, e) -> e.state.getGameID()),
    ROUND((l, e) -> (e.state.getTurnOrder()).getRoundCounter()),
    TURN((l, e) -> (e.state.getTurnOrder()).getTurnCounter()),
    PLAYER((l, e) -> e.state.getCurrentPlayer()),
    PLAYER_SCORE((l, e) -> e.state.getGameScore(e.state.getCurrentPlayer())),
    ACTION_TYPE((l, e) -> e.action == null ? "NONE" : e.action.getClass().getSimpleName()),
    ACTION_DESCRIPTION((l, e) -> e.action == null ? "NONE" : e.action.getString(e.state))
    ;

    private final BiFunction<GameListener, Event, Object> lambda;

    ActionSimpleAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return eventType == Event.GameEvent.ACTION_CHOSEN || eventType == Event.GameEvent.GAME_EVENT;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }

}

package utilities;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import evaluation.metrics.Event;

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

    private final BiFunction<ActionListener, Event, Object> lambda;

    ActionSimpleAttributes(BiFunction<ActionListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply((ActionListener)listener, event);
    }

}

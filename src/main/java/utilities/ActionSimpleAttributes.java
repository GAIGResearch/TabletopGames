package utilities;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;

import java.util.function.BiFunction;

/**
 * This is a stripped down set of IGameAttributes that is generic to any game
 * and is the default set used by ActionListener to generate a game log
 *
 * When working on a specific game it is advisable to use this as a starting point, and
 * add in relevant game-specific items of data
 */
public enum ActionSimpleAttributes implements IGameAttribute {

    GAME_ID((s, a) -> s.getGameID()),
    ROUND((s, a) -> (s.getTurnOrder()).getRoundCounter()),
    TURN((s, a) -> (s.getTurnOrder()).getTurnCounter()),
    PLAYER((s, a) -> s.getCurrentPlayer()),
    PLAYER_SCORE((s, a) -> s.getGameScore(s.getCurrentPlayer())),
    ACTION_TYPE((s, a) -> a == null ? "NONE" : a.getClass().getSimpleName()),
    ACTION_DESCRIPTION((s, a) -> a == null ? "NONE" : a.getString(s))
    ;

    private final BiFunction<AbstractGameState, AbstractAction, Object> lambda;

    ActionSimpleAttributes(BiFunction<AbstractGameState, AbstractAction, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        return lambda.apply(state, action);
    }

}

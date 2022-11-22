package games.sushigo;

import core.interfaces.IGameMetric;
import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.function.*;

public enum SGGameAttributes implements IGameMetric {
    GAME_ID((SGGameState s, AbstractAction a) -> s.getGameID()),
    PLAYER((SGGameState s, AbstractAction a) -> s.getCurrentPlayer());
    //WINNING_PLAYER((SGGameState s, AbstractAction a) -> s.getWinningPlayer());


    private final BiFunction<SGGameState, AbstractAction, Object> lambda;

    SGGameAttributes(BiFunction<SGGameState, AbstractAction, Object> lambda){
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        return lambda.apply((SGGameState) state, action);
    }

    @Override
    public Type getType() {
        return Type.STATE_ACTION;
    }
}


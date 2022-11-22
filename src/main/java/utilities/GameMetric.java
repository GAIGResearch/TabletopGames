package utilities;
import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;

import java.util.function.BiFunction;
import java.util.function.Function;
public enum GameMetric implements IGameMetric {

    TICKS((g) -> {
        return g.getTick(); // I know, there are cooler ways. Bear with me.
    });

    final private Type type;
    private Function<Game, Object> lambda_g;
    private BiFunction<AbstractGameState, Integer, Object> lambda_sp;
    private BiFunction<AbstractGameState, AbstractAction, Object> lambda_sa;

    GameMetric(BiFunction<AbstractGameState, AbstractAction, Object> lambda) {
        this.lambda_sa = lambda;
        type = Type.STATE_ACTION;
    }

    GameMetric(BiFunction<AbstractGameState, Integer, Object> lambda, boolean player) { //ugly dummy argument to differentiate from above.
        this.lambda_sp = lambda;
        type = Type.STATE_PLAYER;
    }

    GameMetric(Function<Game, Object> lambda) {
        this.lambda_g = lambda;
        type = Type.GAME;
    }

    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        if(lambda_sa != null)
            return lambda_sa.apply(state, action);
        return null;
    }

    @Override
    public Object get(AbstractGameState state, int playerID) {
        if(lambda_sp != null)
            return lambda_sp.apply(state, playerID);
        return null;
    }


    @Override
    public Object get(Game game) {
        if(lambda_g != null)
            return lambda_g.apply(game);
        return null;
    }


    @Override
    public Type getType() {
        return type;
    }
}

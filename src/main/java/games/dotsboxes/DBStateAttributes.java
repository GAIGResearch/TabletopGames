package games.dotsboxes;

import core.AbstractGameState;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;

import java.util.function.BiFunction;

public enum DBStateAttributes implements IGameAttribute {

    GAME_ID((s, p) -> s.getGameID()),
    TURN((s,p) -> s.getTurnOrder().getTurnCounter()),
    PLAYER((s, p) -> p),
    CURRENT_PLAYER((s, p) -> s.getCurrentPlayer())

    ;


    private final BiFunction<DBGameState, Integer, Object> lambda;

    DBStateAttributes(BiFunction<DBGameState, Integer, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(AbstractGameState state, int player) {
        return lambda.apply((DBGameState) state, player);
    }
}

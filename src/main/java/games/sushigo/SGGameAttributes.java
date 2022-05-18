package games.sushigo;

import core.interfaces.IGameAttribute;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import games.dominion.DominionGameState;
import games.sushigo.cards.SGCard;
import java.util.function.*;

public enum SGGameAttributes implements IGameAttribute {
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
}


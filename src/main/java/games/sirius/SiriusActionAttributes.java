package games.sirius;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import games.sirius.actions.*;

import java.util.function.BiFunction;
import java.util.function.Supplier;

public class SiriusActionAttributes implements Supplier<IGameAttribute> {

    int count = -1;

    @Override
    public IGameAttribute get() {
        if (count == SAA.values().length - 1)
            return null;
        count++;
        return SAA.values()[count];
    }

    public enum SAA implements IGameAttribute {

        GAME_ID((s, a) -> s.getGameID()),
        ROUND((s, a) -> s.getTurnOrder().getRoundCounter()),
        TURN((s, a) -> s.getTurnOrder().getTurnCounter()),
        PLAYER((s, a) -> s.getCurrentPlayer()),
        ACTION_TYPE((s, a) -> a == null ? "NONE" : a.getClass().getSimpleName()),
        ACTION_DESCRIPTION((s, a) -> a == null ? "NONE" : a.getString(s)),
        LOCATION((s, a) -> s.getMoon(s.getLocationIndex(s.getCurrentPlayer())).getComponentName()),
        THING((s, a) -> {
            if (a == null) return "";
            if (a instanceof SellCards) return ((SellCards) a).salesType;
            return "";
        }),
        VALUE((s, a) -> {
            if (a == null) return 0;
            if (a instanceof SellCards) return ((SellCards) a).getTotalValue();
            return 0;
        });

        private final BiFunction<SiriusGameState, AbstractAction, Object> lambda;

        SAA(BiFunction<SiriusGameState, AbstractAction, Object> lambda) {
            this.lambda = lambda;
        }

        @Override
        public Object get(AbstractGameState state, AbstractAction action) {
            return lambda.apply((SiriusGameState) state, action);
        }
    }
}
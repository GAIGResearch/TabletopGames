package games.loveletter.stats;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.DrawCard;

import java.util.function.BiFunction;

public enum LLGameAttributes implements IGameMetric {
    DISCARDED_CARDS((s, a) -> {
        int nCards = 0;
        for (int i = 0; i < s.getNPlayers(); i++) {
            nCards += s.getPlayerDiscardCards().get(i).getSize();
        }
        return nCards;
    });
    private final BiFunction<LoveLetterGameState, DrawCard, Object> lambda_sa;
    LLGameAttributes(BiFunction<LoveLetterGameState, DrawCard, Object> lambda) {
        this.lambda_sa = lambda;
    }
    @Override
    public Object get(AbstractGameState state, AbstractAction action) {
        return lambda_sa.apply((LoveLetterGameState) state, (DrawCard) action);
    }

    @Override
    public Type getType() {
        return Type.STATE_ACTION;
    }
}

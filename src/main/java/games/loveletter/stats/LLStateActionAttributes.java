package games.loveletter.stats;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import evaluation.metrics.Event;
import games.loveletter.LoveLetterGameState;
import games.loveletter.actions.DrawCard;

import java.util.function.BiFunction;

public enum LLStateActionAttributes implements IGameMetric {
    DISCARDED_CARDS((l, e) -> {
        int nCards = 0;
        LoveLetterGameState llgs = (LoveLetterGameState) e.state;
        for (int i = 0; i < e.state.getNPlayers(); i++) {
            nCards += llgs.getPlayerDiscardCards().get(i).getSize();
        }
        return nCards;
    });

    private final BiFunction<LLPlayerListener, Event, Object> lambda_sa;
    LLStateActionAttributes(BiFunction<LLPlayerListener, Event, Object> lambda) {
        this.lambda_sa = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda_sa.apply((LLPlayerListener) listener, event);
    }

}

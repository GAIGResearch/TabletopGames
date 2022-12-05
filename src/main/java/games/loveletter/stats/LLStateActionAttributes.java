package games.loveletter.stats;
import core.interfaces.IGameMetric;
import evaluation.metrics.GameListener;
import evaluation.metrics.Event;
import games.loveletter.LoveLetterGameState;

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

    private final BiFunction<GameListener, Event, Object> lambda_sa;
    LLStateActionAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda_sa = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda_sa.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return eventType == Event.GameEvent.GAME_OVER;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }

}

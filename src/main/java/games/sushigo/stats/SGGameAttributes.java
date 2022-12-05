package games.sushigo.stats;

import core.interfaces.IGameMetric;
import evaluation.metrics.GameListener;
import evaluation.metrics.Event;

import java.util.function.*;

public enum SGGameAttributes implements IGameMetric {
    GAME_ID((l, e) -> e.state.getGameID()),
    PLAYER((l, e) -> e.state.getCurrentPlayer());
    //WINNING_PLAYER((SGGameState s, AbstractAction a) -> s.getWinningPlayer());

    private final BiFunction<GameListener, Event,  Object> lambda;

    SGGameAttributes(BiFunction<GameListener, Event, Object> lambda){
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
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


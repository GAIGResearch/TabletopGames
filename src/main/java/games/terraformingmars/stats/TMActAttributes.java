package games.terraformingmars.stats;

import core.interfaces.IGameMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.TMAction;

import java.util.function.BiFunction;

public enum TMActAttributes implements IGameMetric {
    GAME_ID((l, e) -> e.state.getGameID()),
    GENERATION((l, e) -> ((TMGameState) e.state).getGeneration()),
    PLAYER((l, e) -> e.state.getCurrentPlayer()),
    ACTION_TYPE((l, e) -> {
        if (e.action == null) return "NONE";
        TMAction tma = (TMAction) e.action;
        if (tma instanceof PayForAction)
            return ((PayForAction) tma).action.getClass().getSimpleName();
        if (tma.pass) return "Pass";
        return tma.getClass().getSimpleName() + "(" + tma.actionType + ")";
    });
//    ACTION_DESCRIPTION((l, e) ->  e.action == null ? "NONE" : e.action.getString(s)),;

    private final BiFunction<GameListener, Event, Object> lambda;

    TMActAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return eventType == Event.GameEvent.ACTION_CHOSEN;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }

}

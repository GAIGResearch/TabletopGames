package games.terraformingmars.stats;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import games.terraformingmars.TMGameState;
import games.terraformingmars.actions.PayForAction;
import games.terraformingmars.actions.TMAction;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TMActListener extends GameListener {

    public TMActListener(IStatisticLogger logger) {
        super(logger, null);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ACTION_CHOSEN) {
            AbstractGameState state = event.state;
            Map<String, Object> data = Arrays.stream(TMActAttributes.values())
                    .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
            logger.record(data);
        }
    }

    public enum TMActAttributes implements IGameMetric {
        GAME_ID((l, e) -> e.state.getGameID()),
        GENERATION((l, e) -> ((TMGameState)e.state).getGeneration()),
        PLAYER((l, e) -> e.state.getCurrentPlayer()),
        ACTION_TYPE((l, e) -> {
            if(e.action == null) return "NONE";
            TMAction tma = (TMAction) e.action;
            if(tma instanceof PayForAction)
                return ((PayForAction) tma).action.getClass().getSimpleName();
            if(tma.pass) return "Pass";
            return tma.getClass().getSimpleName() + "(" + tma.actionType + ")";
        });
//    ACTION_DESCRIPTION((l, e) ->  e.action == null ? "NONE" : e.action.getString(s)),;

        private final BiFunction<TMActListener, Event, Object> lambda;

        TMActAttributes(BiFunction<TMActListener, Event, Object> lambda) {
            this.lambda = lambda;
        }

        @Override
        public Object get(GameListener listener, Event event) {
            return lambda.apply((TMActListener) listener, event);
        }

    }
}

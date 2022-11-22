package games.terraformingmars.stats;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
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
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == CoreConstants.GameEvents.ACTION_CHOSEN) {
            Map<String, Object> data = Arrays.stream(TMActAttributes.values())
                    .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(state, action)));
            logger.record(data);
        }
    }

    public enum TMActAttributes implements IGameMetric {
        GAME_ID((s, a) -> s.getGameID()),
        GENERATION((s, a) -> s.getGeneration()),
        PLAYER((s, a) -> s.getCurrentPlayer()),
        ACTION_TYPE((s, a) -> a == null ? "NONE" : (a instanceof PayForAction ? ((PayForAction)a).action.getClass().getSimpleName() : (a.pass? "Pass" : a.getClass().getSimpleName())) + "(" + a.actionType + ")");
//    ACTION_DESCRIPTION((s, a) ->  a == null ? "NONE" : a.getString(s)),;

        private final BiFunction<TMGameState, TMAction, Object> lambda;

        TMActAttributes(BiFunction<TMGameState, TMAction, Object> lambda) {
            this.lambda = lambda;
        }

        @Override
        public Object get(AbstractGameState state, AbstractAction action) {
            return lambda.apply((TMGameState) state, (TMAction) action);
        }

        @Override
        public Type getType() {
            return Type.STATE_ACTION;
        }
    }
}

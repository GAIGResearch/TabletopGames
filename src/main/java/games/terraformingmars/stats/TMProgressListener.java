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
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TMProgressListener extends GameListener {

    public TMProgressListener(IStatisticLogger logger) {
        super(logger, null);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ROUND_OVER) {
            Map<String, Object> data = Arrays.stream(TMProgressAttributes.values())
                    .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
            logger.record(data);
        }
    }

    public enum TMProgressAttributes implements IGameMetric {
        GAME_ID((l, e) -> e.state.getGameID()),
        GENERATION((l, e) -> ((TMGameState)e.state).getGeneration()),
        N_POINTS_PROGRESS((l, e) -> {

            TMGameState s = ((TMGameState)e.state);
            String ss = "[";
            for(int i = 0; i < s.getNPlayers(); i++) {
//            ss += s.countPoints(i) + ",";
                ss += "(" + s.getPlayerResources()[i].get(TMTypes.Resource.TR).getValue() + "," +
                        s.countPointsMilestones(i) + "," +
                        s.countPointsAwards(i) + "," +
                        s.countPointsBoard(i) + "," +
                        s.countPointsCards(i) + "),";
            }
            ss += "]";
            return ss.replace(",]", "]");
        });

        private final BiFunction<TMProgressListener, Event, Object> lambda;

        TMProgressAttributes(BiFunction<TMProgressListener, Event, Object> lambda) {
            this.lambda = lambda;
        }

        @Override
        public Object get(GameListener listener, Event event) {
            return lambda.apply((TMProgressListener) listener, event);
        }

    }
}

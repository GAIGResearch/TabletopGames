package games.terraformingmars.stats;

import core.interfaces.IGameMetric;
import evaluation.metrics.Event;
import evaluation.metrics.GameListener;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;

import java.util.function.BiFunction;

public enum TMProgressAttributes implements IGameMetric {
    GAME_ID((l, e) -> e.state.getGameID()),
    GENERATION((l, e) -> ((TMGameState) e.state).getGeneration()),
    N_POINTS_PROGRESS((l, e) -> {

        TMGameState s = ((TMGameState) e.state);
        String ss = "[";
        for (int i = 0; i < s.getNPlayers(); i++) {
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

    private final BiFunction<GameListener, Event, Object> lambda;

    TMProgressAttributes(BiFunction<GameListener, Event, Object> lambda) {
        this.lambda = lambda;
    }

    @Override
    public Object get(GameListener listener, Event event) {
        return lambda.apply(listener, event);
    }

    @Override
    public boolean listens(Event.GameEvent eventType) {
        return eventType == Event.GameEvent.ROUND_OVER;
    }

    @Override
    public boolean isRecordedPerPlayer() {
        return false;
    }

}

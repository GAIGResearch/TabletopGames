package games.terraformingmars.stats;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;
import games.terraformingmars.TMGameState;
import games.terraformingmars.TMTypes;
import games.terraformingmars.actions.TMAction;

import java.util.Arrays;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public class TMProgressListener implements IGameListener {

    IStatisticLogger logger;
    public TMProgressListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == CoreConstants.GameEvents.ROUND_OVER) {
            Map<String, Object> data = Arrays.stream(TMProgressAttributes.values())
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, action)));
            logger.record(data);
        }
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }


    public enum TMProgressAttributes implements IGameAttribute {
        GAME_ID((s, a) -> s.getGameID()),
        GENERATION((s, a) -> s.getGeneration()),
        N_POINTS_PROGRESS((s,a) -> {
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

        private final BiFunction<TMGameState, TMAction, Object> lambda;

        TMProgressAttributes(BiFunction<TMGameState, TMAction, Object> lambda) {
            this.lambda = lambda;
        }

        @Override
        public Object get(AbstractGameState state, AbstractAction action) {
            return lambda.apply((TMGameState) state, (TMAction) action);
        }

    }
}

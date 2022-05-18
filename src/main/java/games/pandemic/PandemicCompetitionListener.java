package games.pandemic;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class PandemicCompetitionListener implements IGameListener {
    IStatisticLogger logger;
    public PandemicCompetitionListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            Map<String, Object> data = Arrays.stream(PandemicCompetitionRankingAttributes.values())
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(game)));
            logger.record(data);
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        //nothing
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }
}

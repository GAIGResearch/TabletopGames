package games.terraformingmars.stats;

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

public class TMListener implements IGameListener {

    IStatisticLogger logger;
    public TMListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            AbstractGameState state = game.getGameState();
            Map<String, Object> data = Arrays.stream(TMGameAttributes.values())
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, null)));
            logger.record(data);
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }
}

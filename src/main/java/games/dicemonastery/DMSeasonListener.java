package games.dicemonastery;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameAttribute;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DMSeasonListener implements IGameListener {

    IStatisticLogger logger;

    public DMSeasonListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        // nothing
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == CoreConstants.GameEvents.ROUND_OVER) {
            for (int p = 0; p < state.getNPlayers(); p++) {
                int finalP = p;
                Map<String, Object> data = Arrays.stream(DiceMonasteryStateAttributes.values())
                        .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, finalP), (a, b) -> a, LinkedHashMap::new));
                logger.record(data);
            }
        }
    }

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }
}

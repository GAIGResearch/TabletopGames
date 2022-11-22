package games.dicemonastery;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DMSeasonListener extends GameListener {

    public DMSeasonListener(IStatisticLogger logger) {
        super(logger, null);
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
                        .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(state, finalP), (a, b) -> a, LinkedHashMap::new));
                logger.record(data);
            }
        }
    }
}

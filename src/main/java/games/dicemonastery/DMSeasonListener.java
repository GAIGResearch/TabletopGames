package games.dicemonastery;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class DMSeasonListener extends GameListener {

    public DMSeasonListener(IStatisticLogger logger) {
        super(logger, null);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ROUND_OVER) {
            for (int p = 0; p < event.state.getNPlayers(); p++) {
                Map<String, Object> data = Arrays.stream(DiceMonasteryStateAttributes.values())
                        .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event), (a, b) -> a, LinkedHashMap::new));
                logger.record(data);
            }
        }
    }
}

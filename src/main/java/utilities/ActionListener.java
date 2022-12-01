package utilities;

import core.AbstractGameState;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


public class ActionListener extends GameListener {

    IGameMetric[] attributesToRecord;

    public ActionListener(IStatisticLogger logger) {
        this(logger, ActionSimpleAttributes.values());
    }

    public ActionListener(IStatisticLogger logger, IGameMetric... attributes) {
        super(logger, null);
        this.attributesToRecord = attributes;
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.ACTION_CHOSEN || event.type == Event.GameEvent.GAME_EVENT) {
            AbstractGameState state = event.game.getGameState();
            Map<String, Object> data = Arrays.stream(attributesToRecord)
                    .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
            logger.record(data);
        }
    }
}

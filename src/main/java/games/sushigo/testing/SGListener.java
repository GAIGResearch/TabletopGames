package games.sushigo.testing;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;
import games.sushigo.SGGameAttributes;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


public class SGListener extends GameListener {

    public SGListener(IStatisticLogger logger){
        super(logger, null);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.GAME_OVER) {
            Map<String, Object> data = Arrays.stream(SGGameAttributes.values())
                    .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
            logger.record(data);
        }
    }


}

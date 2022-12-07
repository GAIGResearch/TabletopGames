package games.terraformingmars.stats;

import evaluation.metrics.AbstractMetric;
import evaluation.metrics.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;

import java.util.*;
public class TMPlayerListener extends GameListener {

    IStatisticLogger[] loggerArray;

    public TMPlayerListener(IStatisticLogger[] loggerArray, IStatisticLogger aggregate) {
        super(aggregate, new AbstractMetric[]{});
        this.loggerArray = loggerArray;
    }

    @Override
    public void onEvent(Event event) {
        if(event.type == Event.GameEvent.GAME_OVER) {
            for (int i = 0; i < event.state.getNPlayers(); i++) {
                final int player = i;
                Map<String, Object> data = new HashMap<>();
//                Map<String, Object> data = Arrays.stream(TMPlayerAttributes.values()) //TODO: This had to go.
//                        .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
                loggerArray[i].record(data);
                loggers.get(event.type).record(data);
            }
        }
    }

    @Override
    public void allGamesFinished() {
        for (IStatisticLogger log : loggerArray) {
            log.processDataAndFinish();
        }
        super.allGamesFinished();
    }

}

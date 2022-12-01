package games.pandemic;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameMetric;
import evaluation.GameListener;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class PandemicCompetitionListener extends GameListener {

    public PandemicCompetitionListener(IStatisticLogger logger) {
        super(logger, null);
    }

    @Override
    public void onEvent(Event event) {
        if (event.type == Event.GameEvent.GAME_OVER) {
            Map<String, Object> data = Arrays.stream(PandemicCompetitionRankingAttributes.values())
                    .collect(Collectors.toMap(IGameMetric::name, attr -> attr.get(this, event)));
            logger.record(data);
        }
    }


}

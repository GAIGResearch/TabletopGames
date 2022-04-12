package utilities;

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

import static core.CoreConstants.GameEvents.ACTION_CHOSEN;
import static core.CoreConstants.GameEvents.GAME_EVENT;

public class ActionListener implements IGameListener {

    IStatisticLogger logger;
    IGameAttribute[] attributesToRecord;

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }

    public ActionListener(IStatisticLogger logger) {
        this(logger, ActionSimpleAttributes.values());
    }

    public ActionListener(IStatisticLogger logger, IGameAttribute... attributes) {
        this.logger = logger;
        this.attributesToRecord = attributes;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        // Here we do nothing, as we are only interested in Action events
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == ACTION_CHOSEN || type == GAME_EVENT) {
            Map<String, Object> data = Arrays.stream(attributesToRecord)
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, action)));
            logger.record(data);
        }
    }

}

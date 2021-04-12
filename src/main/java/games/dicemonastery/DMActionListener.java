package games.dicemonastery;

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

public class DMActionListener implements IGameListener {

    IStatisticLogger logger;

    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }

    public DMActionListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
// Nothing
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == CoreConstants.GameEvents.ACTION_CHOSEN) {
            Map<String, Object> data = Arrays.stream(DiceMonasteryActionAttributes.values())
                    .collect(Collectors.toMap(IGameAttribute::name, attr -> attr.get(state, action)));
            logger.record(data);
        }
    }
}

package utilities;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;
import core.interfaces.IStatisticLogger;

import java.util.HashMap;
import java.util.Map;

import static core.CoreConstants.GameEvents.ACTION_CHOSEN;
import static core.CoreConstants.GameEvents.GAME_EVENT;

public class ActionListener implements IGameListener {

    private IStatisticLogger logger;

    public ActionListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        // nothing
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        if (type == ACTION_CHOSEN || type == GAME_EVENT) {
            Map<String, Object> data = new HashMap<>();
            data.put("GameID", state.getGameID());
            data.put("Player", state.getCurrentPlayer());
            data.put("Turn", state.getTurnOrder().getTurnCounter());
            data.put("Round", state.getTurnOrder().getRoundCounter());
            data.put("Action", action.getClass().getSimpleName());
            data.put("Description", action.getString(state));
            logger.record(data);
        }
    }


}

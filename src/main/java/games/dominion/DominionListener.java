package games.dominion;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.interfaces.IGameListener;

import java.util.HashMap;
import java.util.Map;

public class DominionListener implements IGameListener {
    Map<String, Object> data = new HashMap<>();

    @Override
    public void onGameEvent(CoreConstants.GameEvents type, Game game) {
        if (type == CoreConstants.GameEvents.GAME_OVER) {
            AbstractGameState state = game.getGameState();
            for (DominionGameAttributes attribute : DominionGameAttributes.values()) {
                data.put(attribute.name(), attribute.get(state, null));
            }
        }
    }

    @Override
    public void onEvent(CoreConstants.GameEvents type, AbstractGameState state, AbstractAction action) {
        // nothing
    }

    @Override
    public Map<String, Object> getAllData() {
        return data;
    }

    @Override
    public void clear() {
        data = new HashMap<>();
    }
}

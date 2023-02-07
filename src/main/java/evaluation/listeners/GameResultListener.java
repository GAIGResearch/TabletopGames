package evaluation.listeners;

import core.AbstractGameState;
import core.Game;
import core.interfaces.IStatisticLogger;
import evaluation.metrics.Event;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.GAME_OVER;

public class GameResultListener implements IGameListener {

    Map<String, Object> collectedData = new LinkedHashMap<>();
    IStatisticLogger logger;
    Game game;

    public GameResultListener(IStatisticLogger logger) {
        this.logger = logger;
    }

    public GameResultListener() {
        this.logger = null;
    }

    @Override
    public void onEvent(Event event) {
        Event.GameEvent type = event.type;
        if (type == GAME_OVER) {
            AbstractGameState state = game.getGameState();
            collectedData.put("Game", game.getGameType().name());
            collectedData.put("GameID", game.getGameState().getGameID());
            collectedData.put("Players", state.getNPlayers());
            collectedData.put("Rounds", state.getRoundCounter());
            collectedData.put("Turns", state.getTurnCounter());
            collectedData.put("Ticks", game.getTick());
            for (int p = 0; p < 9; p++) {
                if (p >= state.getNPlayers()) {
                    collectedData.put(String.format("P%d_Score", p), 0);
                    collectedData.put(String.format("P%d_Ordinal", p), 0);
                    collectedData.put(String.format("P%d_Type", p), "");
                } else {
                    collectedData.put(String.format("P%d_Score", p), state.getGameScore(p));
                    collectedData.put(String.format("P%d_Ordinal", p), state.getOrdinalPosition(p));
                    collectedData.put(String.format("P%d_Type", p), game.getPlayers().get(p).toString());
                }
            }

            logger.record(collectedData);
            collectedData = new HashMap<>();
        }
    }


    @Override
    public void allGamesFinished() {
        logger.processDataAndFinish();
    }

    @Override
    public void setGame(Game game) {
        this.game = game;
    }

    @Override
    public Game getGame() {
        return game;
    }

}

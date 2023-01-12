package evaluation.metrics;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import utilities.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
public class MiscGameMetrics implements IMetricsCollection {

    public static class GameStartMetrics extends AbstractMetric{

        public GameStartMetrics() {addEventType(Event.GameEvent.ABOUT_TO_START);}

        @Override
        public Object run(GameListener listener, Event e) {
            Game game = listener.getGame();
            AbstractGameState state = game.getGameState();
            AbstractForwardModel fm = game.getForwardModel();

            Map<String, Object> collectedData = new HashMap<>();
            collectedData.put("Game", game.getGameState().getGameType().name());
            collectedData.put("GameID", game.getGameState().getGameID());
            collectedData.put("Players", String.valueOf(game.getGameState().getNPlayers()));
            long s = System.nanoTime();
            fm.setup(state);
            long postS = System.nanoTime() - s;
            collectedData.put("TimeSetup", postS / 1e3);

            Pair<Integer, int[]> components = GameMetrics.countComponents(state);
            collectedData.put("AvgHiddenInfo", Arrays.stream(components.b).sum() / (double) components.a / state.getNPlayers());

            return collectedData;
        }
    }
    public static class ActionSample extends AbstractMetric{

        public ActionSample(){
            addEventType(Event.GameEvent.ACTION_CHOSEN);
            addEventType(Event.GameEvent.GAME_EVENT);
        }

        @Override
        public Object run(GameListener listener, Event e) {
            Map<String, Object> collectedData = new HashMap<>();
            collectedData.put("Game ID", e.state.getGameID());
            collectedData.put("Round",  (e.state.getTurnOrder()).getRoundCounter());
            collectedData.put("Turn",  (e.state.getTurnOrder()).getTurnCounter());
            collectedData.put("Player",  e.state.getCurrentPlayer());
            collectedData.put("Player Score", e.state.getGameScore(e.state.getCurrentPlayer()));
            collectedData.put("Action Type", e.action == null ? "NONE" : e.action.getClass().getSimpleName());
            collectedData.put("Action Description", e.action == null ? "NONE" : e.action.getString(e.state));
            return collectedData;
        }
    }
}

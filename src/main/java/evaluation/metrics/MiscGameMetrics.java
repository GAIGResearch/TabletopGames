//package evaluation.metrics;
//import core.AbstractForwardModel;
//import core.AbstractGameState;
//import core.Game;
//import evaluation.listeners.MetricsGameListener;
//import utilities.Pair;
//
//import java.util.*;
//
//@SuppressWarnings("unused")
//public class MiscGameMetrics implements IMetricsCollection {
//
//    public static class GameStartMetrics extends AbstractMetric{
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            Game game = listener.getGame();
//            AbstractGameState state = game.getGameState();
//            AbstractForwardModel fm = game.getForwardModel();
//
//            Map<String, Object> collectedData = new HashMap<>();
//            collectedData.put("Game", game.getGameState().getGameType().name());
//            collectedData.put("GameID", game.getGameState().getGameID());
//            collectedData.put("Players", String.valueOf(game.getGameState().getNPlayers()));
//            long s = System.nanoTime();
//            fm.setup(state);
//            long postS = System.nanoTime() - s;
//            collectedData.put("TimeSetup", postS / 1e3);
//
//            Pair<Integer, int[]> components = GameMetrics.countComponents(state);
//            collectedData.put("AvgHiddenInfo", Arrays.stream(components.b).sum() / (double) components.a / state.getNPlayers());
//
//            return collectedData;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
//        }
//    }
//
//    public static class ActionSample extends AbstractMetric{
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            Map<String, Object> collectedData = new HashMap<>();
//            collectedData.put("Game ID", e.state.getGameID());
//            collectedData.put("Round",  e.state.getRoundCounter());
//            collectedData.put("Turn",  e.state.getTurnCounter());
//            collectedData.put("Player",  e.state.getCurrentPlayer());
//            collectedData.put("Player Score", e.state.getGameScore(e.state.getCurrentPlayer()));
//            collectedData.put("Action Type", e.action == null ? "NONE" : e.action.getClass().getSimpleName());
//            collectedData.put("Action Description", e.action == null ? "NONE" : e.action.getString(e.state));
//            return collectedData;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.GAME_EVENT));
//        }
//    }
//}

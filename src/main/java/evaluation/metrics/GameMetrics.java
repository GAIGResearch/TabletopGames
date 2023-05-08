package evaluation.metrics;

import core.AbstractGameState;
import core.CoreConstants;
import core.Game;
import core.interfaces.IComponentContainer;
import evaluation.listeners.MetricsGameListener;
import core.AbstractForwardModel;
import utilities.Pair;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.GAME_OVER;

@SuppressWarnings("unused")
public class GameMetrics implements IMetricsCollection
{
    public static class GameScore extends AbstractMetric{
        public GameScore(){super();}
        public GameScore(Event.GameEvent... args ){super(args);}

        @Override
        public void _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            double sum = 0;
            int leaderID = -1;
            int secondID = -1;
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                double score = e.state.getGameScore(i);
                sum += score;
                records.put("Player-" + i, score);
                if (e.state.getOrdinalPosition(i) == 1) leaderID = i;
                if (e.state.getNPlayers() > 1 && e.state.getOrdinalPosition(i) == 2) secondID = i;
            }
            records.put("Average", sum/e.state.getNPlayers());
            if (secondID != -1) {
                records.put("LeaderGap", e.state.getGameScore(leaderID) - e.state.getGameScore(secondID));
            } else {
                records.put("LeaderGap", 0.0);
            }
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, GAME_OVER));
        }
        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < game.getPlayers().size(); i++)
                columns.put("Player-" + i, Double.class);
            columns.put("Average", Double.class);
            columns.put("LeaderGap", Double.class);
            return columns;
        }
    }

    public static class FinalScore extends AbstractMetric{
        public FinalScore(){super();}
        public FinalScore(Event.GameEvent... args ){super(args);}

        @Override
        public void _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("Player-" + i, e.state.getGameScore(i));
            }
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < game.getPlayers().size(); i++)
                columns.put("Player-" + i, Double.class);
            return columns;
        }
    }

    public static class ActionSpace extends AbstractMetric{
        @Override
        public void _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Game g = listener.getGame();
            AbstractForwardModel fm = g.getForwardModel();
            records.put("Size", fm.computeAvailableActions(e.state).size());
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        public Map<String, Class<?>> getColumns(Game game) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Size", Integer.class);
            return columns;
        }
    }

//    public static class StateSize extends AbstractMetric{
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            int components = countComponents(e.state).a;
//            return (double) components;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.ABOUT_TO_START));
//        }
//    }
//
//    public static class CurrentPlayer extends AbstractMetric{
//        public CurrentPlayer(){super();}
//        public CurrentPlayer(Event.GameEvent... args ){super(args);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return e.state.getCurrentPlayer();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ROUND_OVER);
//        }
//    }
//
//    public static class CurrentPlayerVisibility extends AbstractMetric{
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            AbstractGameState gs = e.state;
//            int player = gs.getCurrentPlayer();
//            Pair<Integer, int[]> allComp = countComponents(gs);
//            return (allComp.b[player] / (double) allComp.a);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
//        }
//    }
//
//    public static class ComputationTimes extends AbstractMetric{
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            Map<String, Object> collectedData = new LinkedHashMap<>();
//            collectedData.put("TimeNext", listener.getGame().getNextTime() / 1e3);
//            collectedData.put("TimeCopy", listener.getGame().getCopyTime() / 1e3);
//            collectedData.put("TimeActionCompute", listener.getGame().getActionComputeTime() / 1e3);
//            collectedData.put("TimeAgent", listener.getGame().getAgentTime() / 1e3);
//            return collectedData;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class GameDuration extends AbstractMetric{
//        public GameDuration(){super();}
//        public GameDuration(Event.GameEvent... args ){super(args);}
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            Map<String, Object> collectedData = new LinkedHashMap<>();
//            collectedData.put("Turns", listener.getGame().getGameState().getTurnCounter());
//            collectedData.put("Ticks", listener.getGame().getTick());
//            collectedData.put("Rounds", listener.getGame().getGameState().getRoundCounter());
//            return collectedData;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }
//
//    public static class OrdinalPosition extends AbstractMetric{
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return e.state.getOrdinalPosition(e.playerID);
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class PlayerType extends AbstractMetric{
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            return listener.getGame().getPlayers().get(e.playerID).toString();
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//        @Override
//        public boolean isRecordedPerPlayer() {
//            return true;
//        }
//    }
//
//    public static class Decisions extends AbstractMetric {
//        @Override
//        public Object run(MetricsGameListener listener, Event e) {
//            List<Pair<Integer, Integer>> actionSpaceRecord = listener.getGame().getActionSpaceSize();
//            TAGStatSummary statsDecisionsAll = actionSpaceRecord.stream()
//                    .map(r -> r.b)
//                    .collect(new TAGSummariser());
//            TAGStatSummary statsDecisions = actionSpaceRecord.stream()
//                    .map(r -> r.b)
//                    .filter(size -> size > 1)
//                    .collect(new TAGSummariser());
//
//            Map<String, Object> collectedData = new LinkedHashMap<>();
//            collectedData.put("ActionsPerTurnSum", listener.getGame().getNActionsPerTurn());
//            collectedData.put("Decisions", statsDecisions.n());
//            collectedData.put("DecisionPointsMean", statsDecisions.n() * 1.0 / statsDecisionsAll.n());
//            return collectedData;
//        }
//        @Override
//        public Set<Event.GameEvent> getDefaultEventTypes() {
//            return Collections.singleton(Event.GameEvent.GAME_OVER);
//        }
//    }

    public static class ActionTypes extends AbstractMetric {
        public ActionTypes(){super();}
        public ActionTypes(Event.GameEvent... args ){super(args);}

        @Override
        public void _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("ActionsType", e.action == null ? "NONE" : e.action.getClass().getSimpleName());
            records.put("ActionsDescription", e.action == null ? "NONE" : e.action.getString(e.state));
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(Game game) {
            return new HashMap<String, Class<?>>() {{
                put("ActionsType", String.class);
                put("ActionsDescription", String.class);
            }};
        }
    }

    public static class Winner extends AbstractMetric {
        public Winner(){super();}
        public Winner(Event.GameEvent... args ){super(args);}

        @Override
        public void _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            // iterate through player results in game state and find the winner
            int winner = -1;
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                if (e.state.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) {
                    winner = i;
                    break;
                }
            }
            records.put("PlayerIdx", String.valueOf(winner));
        }

        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public HashMap<String, Class<?>> getColumns(Game game) {
            return new HashMap<String, Class<?>>() {{
                put("PlayerIdx", String.class);
            }};
        }
    }

    /**
     * Returns the total number of components in the state as the first element of the returned value
     * and an array of the counts that are hidden to each player
     * <p>
     *
     * @param state where components are to be counted.
     * @return The total number of components
     */
    public static Pair<Integer, int[]> countComponents(AbstractGameState state) {
        int[] hiddenByPlayer = new int[state.getNPlayers()];
        // we do not include containers in the count...just the lowest-level items
        // open to debate on this. But we are consistent across State Size and Hidden Information stats
        int total = (int) state.getAllComponents().stream().filter(c -> !(c instanceof IComponentContainer)).count();
        for (int p = 0; p < hiddenByPlayer.length; p++)
            hiddenByPlayer[p] = state.getUnknownComponentsIds(p).size();
        return new Pair<>(total, hiddenByPlayer);
    }
}

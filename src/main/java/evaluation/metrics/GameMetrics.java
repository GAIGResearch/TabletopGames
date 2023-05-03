package evaluation.metrics;
import core.AbstractForwardModel;
import core.AbstractGameState;
import core.Game;
import core.interfaces.IComponentContainer;
import evaluation.listeners.MetricsGameListener;
import evaluation.summarisers.TAGStatSummary;
import evaluation.summarisers.TAGSummariser;
import utilities.Pair;

import java.util.*;

@SuppressWarnings("unused")
public class GameMetrics implements IMetricsCollection
{
    public static class GameID extends AbstractMetric{
        public GameID(){super();}
        public GameID(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return e.state.getGameID();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }
    }

    public static class GameName extends AbstractMetric{
        public GameName(){super();}
        public GameName(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return listener.getGame().getGameType().name();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }
    }

    public static class PlayerCount extends AbstractMetric{
        public PlayerCount(){super();}
        public PlayerCount(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return e.state.getNPlayers();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }
    }

    public static class GameSeeds extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return e.state.getGameParameters().getRandomSeed();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ABOUT_TO_START);
        }
    }

    public static class GameStatus extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return e.state.getGameStatus();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

    public static class GameScore extends AbstractMetric{
        public GameScore(){super();}
        public GameScore(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            int player = e.state.getCurrentPlayer();
            return e.state.getGameScore(player);
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class FinalScore extends AbstractMetric{
        public FinalScore(){super();}
        public FinalScore(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            int player = e.state.getCurrentPlayer();
            return e.state.getGameScore(player);
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        @Override
        public boolean isRecordedPerPlayer() {
            return true;
        }
    }

    public static class ActionSpace extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            Game g = listener.getGame();
            AbstractForwardModel fm = g.getForwardModel();
            return fm.computeAvailableActions(g.getGameState()).size();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class StateSize extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            int components = countComponents(e.state).a;
            return (double) components;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.ACTION_CHOSEN, Event.GameEvent.ABOUT_TO_START));
        }
    }

    public static class CurrentPlayer extends AbstractMetric{
        public CurrentPlayer(){super();}
        public CurrentPlayer(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return e.state.getCurrentPlayer();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }
    }

    public static class CurrentPlayerVisibility extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            AbstractGameState gs = e.state;
            int player = gs.getCurrentPlayer();
            Pair<Integer, int[]> allComp = countComponents(gs);
            return (allComp.b[player] / (double) allComp.a);
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
    }

    public static class ComputationTimes extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("TimeNext", listener.getGame().getNextTime() / 1e3);
            collectedData.put("TimeCopy", listener.getGame().getCopyTime() / 1e3);
            collectedData.put("TimeActionCompute", listener.getGame().getActionComputeTime() / 1e3);
            collectedData.put("TimeAgent", listener.getGame().getAgentTime() / 1e3);
            return collectedData;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

    public static class GameDuration extends AbstractMetric{
        public GameDuration(){super();}
        public GameDuration(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("Turns", listener.getGame().getGameState().getTurnCounter());
            collectedData.put("Ticks", listener.getGame().getTick());
            collectedData.put("Rounds", listener.getGame().getGameState().getRoundCounter());
            return collectedData;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

    public static class OrdinalPosition extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return e.state.getOrdinalPosition(e.playerID);
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        @Override
        public boolean isRecordedPerPlayer() {
            return true;
        }
    }

    public static class PlayerType extends AbstractMetric{
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            return listener.getGame().getPlayers().get(e.playerID).toString();
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
        @Override
        public boolean isRecordedPerPlayer() {
            return true;
        }
    }

    public static class Decisions extends AbstractMetric {
        @Override
        public Object run(MetricsGameListener listener, Event e) {
            List<Pair<Integer, Integer>> actionSpaceRecord = listener.getGame().getActionSpaceSize();
            TAGStatSummary statsDecisionsAll = actionSpaceRecord.stream()
                    .map(r -> r.b)
                    .collect(new TAGSummariser());
            TAGStatSummary statsDecisions = actionSpaceRecord.stream()
                    .map(r -> r.b)
                    .filter(size -> size > 1)
                    .collect(new TAGSummariser());

            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("ActionsPerTurnSum", listener.getGame().getNActionsPerTurn());
            collectedData.put("Decisions", statsDecisions.n());
            collectedData.put("DecisionPointsMean", statsDecisions.n() * 1.0 / statsDecisionsAll.n());
            return collectedData;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

    public static class ActionTypes extends AbstractMetric {
        public ActionTypes(){super();}
        public ActionTypes(Event.GameEvent... args ){super(args);}
        @Override
        public Object run(MetricsGameListener listener, Event e) {

            Map<String, Object> collectedData = new LinkedHashMap<>();
            collectedData.put("ActionsType", e.action == null ? "NONE" : e.action.getClass().getSimpleName());
            collectedData.put("ActionsDescription", e.action == null ? "NONE" : e.action.getString(e.state));
            return collectedData;
        }
        @Override
        public Set<Event.GameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
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

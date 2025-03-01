package evaluation.metrics;

import core.*;
import core.actions.AbstractAction;
import core.interfaces.IComponentContainer;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.summarisers.TAGStatSummary;
import evaluation.summarisers.TAGSummariser;
import utilities.Pair;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.*;

@SuppressWarnings("unused")
public class GameMetrics implements IMetricsCollection {
    public static class GameScore extends AbstractMetric {
        public GameScore() {
            super();
        }

        public GameScore(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            double sum = 0;
            int leaderID = -1;
            int secondID = -1;
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                double score = e.state.getGameScore(i);
                sum += score;
                records.put("Player-" + i, score);
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
                if (e.state.getOrdinalPosition(i) == 1) leaderID = i;
                if (e.state.getNPlayers() > 1 && e.state.getOrdinalPosition(i) == 2) secondID = i;
            }
            records.put("Average", sum / e.state.getNPlayers());
            if (secondID != -1) {
                records.put("LeaderGap", e.state.getGameScore(leaderID) - e.state.getGameScore(secondID));
            } else {
                records.put("LeaderGap", 0.0);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(ACTION_CHOSEN, ROUND_OVER, GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Double.class);
                columns.put("PlayerName-" + i, String.class);
            }
            columns.put("Average", Double.class);
            columns.put("LeaderGap", Double.class);
            return columns;
        }
    }

    public static class FinalScore extends AbstractMetric {
        public FinalScore() {
            super();
        }

        public FinalScore(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("Player-" + i, e.state.getGameScore(i));
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Double.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }
    }

    public static class RoundCounter extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            //records.put("RoundCounter: ", e.state.getRoundCounter());
            System.out.println("Round: " + e.state.getRoundCounter());  // TODO just for debug
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("RoundCounter", Integer.class);
            return columns;
        }
    }


    public static class StateSpace extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("Size", countComponents(e.state).a);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(ACTION_CHOSEN, Event.GameEvent.ABOUT_TO_START));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Size", Integer.class);
            return columns;
        }
    }

    public static class PlayerType extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("PlayerType-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Collections.singletonList(GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("PlayerType-" + i, String.class);
            }

            return columns;
        }
    }


    public static class CurrentPlayerVisibility extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractGameState gs = e.state;
            int player = gs.getCurrentPlayer();
            Pair<Integer, int[]> allComp = countComponents(gs);
            records.put("Percentage", (allComp.b[player] / (double) allComp.a) * 100.0);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("Percentage", Double.class);
            }};
        }
    }


    public static class ComputationTimes extends AbstractMetric {

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("Next (ms)", Double.class);
                put("Copy (ms)", Double.class);
                put("Actions Available Compute (ms)", Double.class);
                put("Agent (ms)", Double.class);
                put("Agent", String.class);
                put("Player", Integer.class);
            }};
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            // the times are all recorded in nanoseconds, so we convert to milliseconds
            records.put("Next (ms)", listener.getGame().getNextTime() / 1e6);
            records.put("Copy (ms)", listener.getGame().getCopyTime() / 1e6);
            records.put("Actions Available Compute (ms)", listener.getGame().getActionComputeTime() / 1e6);
            records.put("Agent (ms)", listener.getGame().getAgentTime() / 1e6);
            records.put("Agent", listener.getGame().getPlayers().get(e.playerID).toString());
            records.put("Player", e.playerID);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            // ACTION_CHOSEN would be fine except for the recording of the time for 'next'
            return Collections.singleton(Event.GameEvent.ACTION_TAKEN);
        }
    }

    public static class OrdinalPosition extends AbstractMetric {
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, Integer.class);
                columns.put("Player-" + i + " rank", String.class);
                columns.put("PlayerName-" + i, String.class);
            }
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("Player-" + i, e.state.getOrdinalPosition(i));
                records.put("Player-" + i + " rank", String.valueOf(e.state.getOrdinalPosition(i)));
                records.put("PlayerName-" + i, listener.getGame().getPlayers().get(i).toString());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

    }

    public static class Decisions extends AbstractMetric {

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("ActionsPerTurn (Sum)", Integer.class);
                put("Decisions", Integer.class);
                put("DecisionPoints (Mean)", Double.class);
            }};
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {

            List<Pair<Integer, Integer>> actionSpaceRecord = listener.getGame().getActionSpaceSize();
            TAGStatSummary statsDecisionsAll = actionSpaceRecord.stream()
                    .map(r -> r.b)
                    .collect(new TAGSummariser());
            TAGStatSummary statsDecisions = actionSpaceRecord.stream()
                    .map(r -> r.b)
                    .filter(size -> size > 1)
                    .collect(new TAGSummariser());

            records.put("ActionsPerTurn (Sum)", listener.getGame().getNActionsPerTurn());
            records.put("Decisions", statsDecisions.n());
            records.put("DecisionPoints (Mean)", statsDecisions.n() * 1.0 / statsDecisionsAll.n());
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }
    }

    public static class Actions extends AbstractMetric {
        Set<String> playerNames;
        public Actions() {
            super();
        }

        public Actions(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Game g = listener.getGame();
            AbstractForwardModel fm = g.getForwardModel();
            AbstractAction a = e.action.copy();
            AbstractPlayer currentPlayer = g.getPlayers().get(e.playerID);
            int size = fm.computeAvailableActions(e.state, currentPlayer.getParameters().actionSpace).size();

            if (e.state.isActionInProgress()) {
                e.action = null;
            }

            records.put("Player-" + e.playerID, e.action == null ? null : e.action.toString());
            records.put(currentPlayer.toString(), e.action == null ? null : e.action.toString());
            records.put("Size-" + currentPlayer, size);

            records.put("Actions Played", e.action == null ? null : e.action.toString());
            records.put("Actions Played Description", e.action == null ? null : e.action.getString(e.state));
            records.put("Action Space Size", size);

            e.action = a;
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            this.playerNames = playerNames;
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("Player-" + i, String.class);
            }
            for (String playerName : playerNames) {
                columns.put(playerName, String.class);
                columns.put("Size-" + playerName, Integer.class);
            }
            columns.put("Actions Played", String.class);
            columns.put("Actions Played Description", String.class);
            columns.put("Action Space Size", Integer.class);
            return columns;
        }
    }


    /**
     * Records the actions taken during the game
     * This is 'Reduced' compared to the Actions metric in that it does not have separate columns for each player
     * Instead of having separate columns for 'Player-2', 'Player-3' etc, it has a single column 'Player' with
     * the player ID taking the action. (And similarly for PlayerName)
     * This simplifies data analysis in some circumstances (in others the Actions metric is more useful)
     */
    public static class ActionsReduced extends AbstractMetric {
        public ActionsReduced() {
            super();
        }

        public ActionsReduced(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Game g = listener.getGame();
            AbstractForwardModel fm = g.getForwardModel();
            AbstractPlayer currentPlayer = g.getPlayers().get(e.playerID);
            int size = fm.computeAvailableActions(e.state, currentPlayer.getParameters().actionSpace).size();

            records.put("Player", e.playerID);
            records.put("PlayerType", currentPlayer.toString());
            records.put("Size", size);

            records.put("Action", e.action == null ? null : e.action.toString());
            records.put("ActionClass", e.action.getClass().getSimpleName());
            records.put("ActionDescription", e.action == null ? null : e.action.getString(e.state));
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Player", Integer.class);
            columns.put("PlayerType", String.class);
            columns.put("Size", Integer.class);
            columns.put("Action", String.class);
            columns.put("ActionClass", String.class);
            columns.put("ActionDescription", String.class);
            return columns;
        }
    }



    public static class Winner extends AbstractMetric {
        public Winner() {
            super();
        }

        public Winner(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            // iterate through player results in game state and find the winner
            int winner = -1;
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                if (e.state.getPlayerResults()[i] == CoreConstants.GameResult.WIN_GAME) {
                    winner = i;
                    break;
                }
            }
            records.put("PlayerIdx", String.valueOf(winner));
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public HashMap<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
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
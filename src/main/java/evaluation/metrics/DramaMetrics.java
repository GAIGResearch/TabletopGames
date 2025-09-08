package evaluation.metrics;

import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import games.GameType;
import players.mcts.MCTSParams;
import players.mcts.MCTSPlayer;
import utilities.JSONUtils;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.*;

public class DramaMetrics implements IMetricsCollection {


    public static class StateEstimate extends AbstractMetric {

        private final MCTSPlayer oracle;
        private final AbstractForwardModel fm;

        public StateEstimate(String gameType, MCTSParams oracleDetails) {
            super(ACTION_CHOSEN);
            GameType game = GameType.valueOf(gameType);
            oracle = oracleDetails.instantiate();
            fm = game.createForwardModel(null, 0);
            oracle.setForwardModel(fm);
            // params and nPlayers are only used for Pandemic and the AbstractRuleForwardModel
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            double[] oracleActionValues = new double[e.state.getNPlayers()];
            double[] oracleHeuristicValues = new double[e.state.getNPlayers()];
            if (oracle != null) {
                // use oracle to calculate state values
                List<AbstractAction> actions = fm.computeAvailableActions(e.state);
                if (actions.size() >1) {
                    AbstractAction oracleAction = oracle._getAction(e.state, actions);
                    records.put("OracleAction", oracleAction.toString());
                    records.put("OracleActionDescription", oracleAction.getString(e.state));
                    Map<AbstractAction, Map<String, Object>> stats = oracle.getDecisionStats();
                    Map<String, Object> actionStats = stats.get(oracleAction);
                    oracleActionValues = (double[]) actionStats.get("nodeValue");
                    oracleHeuristicValues = (double[]) actionStats.get("heuristicValue");
                } else {
                    return true; // no choice to be made
                }
            }
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("ScoreP" + i, e.state.getGameScore(i));
                records.put("HeuristicP" + i, e.state.getHeuristicScore(i));
                if (oracle != null) {
                    records.put("HeuristicP" + i, oracleHeuristicValues[i]);  // overrides heuristic with oracle heuristic
                    records.put("OracleP" + i, oracleActionValues[i]);
                }
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                columns.put("ScoreP" + i, Double.class);
                columns.put("HeuristicP" + i, Double.class);
                if (oracle != null)
                    columns.put("OracleP" + i, Double.class);
            }
            if (oracle != null) {
                columns.put("OracleAction", String.class);
                columns.put("OracleActionDescription", String.class);
            }
            return columns;
        }
    }
}

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


        public StateEstimate(String gameType, String oracleDetails, Event.GameEvent... args) {
            super(args);
            GameType game = GameType.valueOf(gameType);
            oracle = JSONUtils.loadClassFromFile(oracleDetails);
            fm = game.createForwardModel(null, 0);
            // params and nPlayers are only used for Pandemic and the AbstractRuleForwardModel
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (oracle != null) {
                // use oracle to calculate state values
                int currentPlayer = e.state.getCurrentPlayer();
                List<AbstractAction> actions = fm.computeAvailableActions(e.state);
                AbstractAction oracleAction = oracle._getAction(e.state, actions);
                records.put("OracleAction", oracleAction.getString(e.state));
            }
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("ScoreP" + i, e.state.getGameScore(i));
                records.put("HeuristicP" + i, e.state.getHeuristicScore(i));
                if (oracle != null) {
                    // TODO: Refactor MCTSPlayer.getDecisionStats to also return the
                    // values for players other than the decision player
                    records.put("OracleP" + i, oracle.getDecisionStats());
                }
            }
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
            if (oracle != null)
                columns.put("OracleAction", String.class);
            return columns;
        }
    }
}

package games.saboteur;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.saboteur.components.RoleCard;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SaboteurMetrics implements IMetricsCollection {

    public static class PlayerStatus extends AbstractMetric {

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Set.of(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int p = 0; p < nPlayersPerGame; p++) {
                columns.put("Player" + p + "_Role", String.class);
            }
            columns.put("WinnerType", String.class);
            return columns;
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            SaboteurGameState sgs = (SaboteurGameState) e.state;
            for (int p = 0; p < sgs.getNPlayers(); p++) {
                RoleCard card = (RoleCard) sgs.roleDeck.get(p);
                records.put("Player" + p + "_Role", card.type.name());
            }
            records.put("WinnerType", sgs.minersWinByRound[sgs.getRoundCounter()] ? "Miners" : "Saboteurs");
            return true;
        }

    }
}

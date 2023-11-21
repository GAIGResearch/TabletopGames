package games.monopolydeal;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.monopolydeal.actions.actioncards.*;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.ACTION_CHOSEN;
import static evaluation.metrics.Event.GameEvent.GAME_OVER;

public class MonopolyDealMetrics implements IMetricsCollection {
    public static class GameDuration extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            records.put("Rounds", e.state.getRoundCounter());
            return true;
        }
        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(GAME_OVER);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Rounds", Integer.class);
            return columns;
        }
    }

    public static class CardsUsed extends AbstractMetric {
        public CardsUsed() {
            super();
            counters = new int[9][5];
        }

        public CardsUsed(Event.GameEvent... args) {
            super(args);
            counters = new int[9][5];
        }
        // Counters
        int[][] counters;
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (ACTION_CHOSEN.equals(e.type)) {
                if (e.action instanceof PassGoAction)
                    counters[0][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof SlyDealAction)
                    counters[1][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof ForcedDealAction)
                    counters[2][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof DebtCollectorAction)
                    counters[3][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof ItsMyBirthdayAction)
                    counters[4][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof MulticolorRentAction)
                    counters[5][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof PropertyRentAction)
                    counters[6][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof DealBreakerAction)
                    counters[7][e.state.getCurrentPlayer()]++;
                else if (e.action instanceof JustSayNoAction)
                    counters[8][e.state.getCurrentPlayer()]++;
                return false;
            } else if (GAME_OVER.equals(e.type)) {
                int winner = e.state.getWinners().iterator().next();
                records.put("PassGoByWinner", counters[0][winner]);
                records.put("SlyDealByWinner", counters[1][winner]);
                records.put("ForcedDealByWinner", counters[2][winner]);
                records.put("DebtCollectorByWinner", counters[3][winner]);
                records.put("ItsMyBirthdayByWinner", counters[4][winner]);
                records.put("MulticolorRentByWinner", counters[5][winner]);
                records.put("PropertyRentByWinner", counters[6][winner]);
                records.put("DealBreakerByWinner", counters[7][winner]);
                records.put("JustSayNoByWinner", counters[8][winner]);
                records.put("PassGoUsed", Arrays.stream(counters[0]).sum());
                records.put("SlyDealUsed", Arrays.stream(counters[1]).sum());
                records.put("ForcedDealUsed", Arrays.stream(counters[2]).sum());
                records.put("DebtCollectorUsed", Arrays.stream(counters[3]).sum());
                records.put("ItsMyBirthdayUsed", Arrays.stream(counters[4]).sum());
                records.put("MulticolorRentUsed", Arrays.stream(counters[5]).sum());
                records.put("PropertyRentUsed", Arrays.stream(counters[6]).sum());
                records.put("DealBreakerUsed", Arrays.stream(counters[7]).sum());
                records.put("JustSayNoUsed", Arrays.stream(counters[8]).sum());
                return true;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(ACTION_CHOSEN, GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("PassGoByWinner", Integer.class);
            columns.put("SlyDealByWinner", Integer.class);
            columns.put("ForcedDealByWinner", Integer.class);
            columns.put("DebtCollectorByWinner", Integer.class);
            columns.put("ItsMyBirthdayByWinner", Integer.class);
            columns.put("MulticolorRentByWinner", Integer.class);
            columns.put("PropertyRentByWinner", Integer.class);
            columns.put("DealBreakerByWinner", Integer.class);
            columns.put("JustSayNoByWinner", Integer.class);
            columns.put("PassGoUsed", Integer.class);
            columns.put("SlyDealUsed", Integer.class);
            columns.put("ForcedDealUsed", Integer.class);
            columns.put("DebtCollectorUsed", Integer.class);
            columns.put("ItsMyBirthdayUsed", Integer.class);
            columns.put("MulticolorRentUsed", Integer.class);
            columns.put("PropertyRentUsed", Integer.class);
            columns.put("DealBreakerUsed", Integer.class);
            columns.put("JustSayNoUsed", Integer.class);
            return columns;
        }

    }

    public static class FinalHeuristicScore extends AbstractMetric {
        public FinalHeuristicScore() {
            super();
        }

        public FinalHeuristicScore(Event.GameEvent... args) {
            super(args);
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                records.put("Player-" + i, e.state.getHeuristicScore(i));
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
}

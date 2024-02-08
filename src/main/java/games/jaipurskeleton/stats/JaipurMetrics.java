package games.jaipurskeleton.stats;

import core.actions.AbstractAction;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.jaipurskeleton.JaipurGameState;
import games.jaipurskeleton.actions.TakeCards;
import games.jaipurskeleton.components.JaipurCard;

import java.util.*;

@SuppressWarnings("unused")
public class JaipurMetrics implements IMetricsCollection {

    public static class RoundScoreDifference extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            JaipurGameState gs = (JaipurGameState) e.state;
            double scoreDiff = 0;
            for (int i = 0; i < gs.getNPlayers()-1; i++) {
                scoreDiff += Math.abs(gs.getPlayerScores().get(i).getValue() - gs.getPlayerScores().get(i+1).getValue());
            }
            records.put("ScoreDiff", scoreDiff/(gs.getNPlayers()-1));

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("ScoreDiff", Double.class);
            return columns;
        }
    }

    public static class PurchaseFromMarket extends AbstractMetric {

        JaipurCard.GoodType[] goodTypes;

        public PurchaseFromMarket(){
            super();
            goodTypes = JaipurCard.GoodType.values();
        }
        public PurchaseFromMarket(String[] args) {
            super(args);
            goodTypes = new JaipurCard.GoodType[args.length];
            for (int i = 0; i < args.length; i++) {
                goodTypes[i] = JaipurCard.GoodType.valueOf(args[i]);
            }
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractAction action = e.action;
            for (JaipurCard.GoodType type : goodTypes) {
                if (action instanceof TakeCards tc) {
                    if (tc.howManyPerTypeTakeFromMarket.containsKey(type))
                        records.put("Purchase-"+type.name(), tc.howManyPerTypeTakeFromMarket.get(type));
                }
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (JaipurCard.GoodType type : goodTypes) {
                columns.put("Purchase-"+type.name(), Integer.class);
            }
            return columns;
        }
    }

    public static class PurchaseFromMarketTot extends AbstractMetric {

        JaipurCard.GoodType[] goodTypes;

        public PurchaseFromMarketTot(){
            super();
            goodTypes = JaipurCard.GoodType.values();
        }
        public PurchaseFromMarketTot(String[] args) {
            super(args);
            goodTypes = new JaipurCard.GoodType[args.length];
            for (int i = 0; i < args.length; i++) {
                goodTypes[i] = JaipurCard.GoodType.valueOf(args[i]);
            }
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractAction action = e.action;
            if (action instanceof TakeCards tc) {
                String s = "";
                for (JaipurCard.GoodType type: tc.howManyPerTypeTakeFromMarket.keySet()) {
                    s += type.name() + "-";
                }
                s += "]";
                records.put("Purchase", s.replace("-]", ""));
                return true;
            } else return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Purchase", String.class);
            return columns;
        }
    }
}

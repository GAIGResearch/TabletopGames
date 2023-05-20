package games.dicemonastery.stats;

import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IDataProcessor;
import evaluation.metrics.IMetricsCollection;
import games.dicemonastery.DiceMonasteryConstants;
import games.dicemonastery.DiceMonasteryGameState;
import games.dicemonastery.actions.*;
import games.dicemonastery.components.Monk;

import java.util.*;
import java.util.function.BiFunction;

import static games.dicemonastery.DiceMonasteryConstants.ActionArea.STOREROOM;

@SuppressWarnings("unused")
public class DiceMonasteryMetrics implements IMetricsCollection {

    public static class Timing extends AbstractMetric {
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("Season", String.class);
                put("Year", Integer.class);
            }};
        }

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            records.put("Season", s.getSeason().name());
            records.put("Year", s.getYear());
            return true;
        }
    }

    public static class Piety extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.action == null) records.put("Piety", 0);
            else if (e.action instanceof ChooseMonk) records.put("Piety", ((ChooseMonk) e.action).piety);
            else if (e.action instanceof PromoteMonk) records.put("Piety", ((PromoteMonk) e.action).pietyLevelToPromote);
            else records.put("Piety", 0);
            return true;
        }
        
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("Piety", Integer.class);
            }};
        }
    }

    public static class Location extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            if (e.action instanceof PlaceMonk) records.put("Location", ((PlaceMonk) e.action).destination.name());
            else if (e.action instanceof ChooseMonk) records.put("Location", ((ChooseMonk) e.action).destination.name());
            else if (e.action instanceof PromoteMonk) records.put("Location", ((PromoteMonk) e.action).location.name());
            else records.put("Location", s.getCurrentArea().name());
            return true;
        }
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("Location", String.class);
            }};
        }
    }

    public static class Thing extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.action == null) records.put("Thing", "");
            else if (e.action instanceof Buy) records.put("Thing", ((Buy) e.action).resource.name());
            else if (e.action instanceof Sell) records.put("Thing", ((Sell) e.action).resource.name());
            else if (e.action instanceof BuyTreasure) records.put("Thing", ((BuyTreasure) e.action).treasure.getComponentName());
            else if (e.action instanceof WriteText) records.put("Thing", ((WriteText) e.action).textType.getComponentName());
            else if (e.action instanceof TakeToken) records.put("Thing", ((TakeToken) e.action).token.name());
            else if (e.action instanceof GoOnPilgrimage) records.put("Thing", ((GoOnPilgrimage) e.action).destination.destination);
            else records.put("Thing", "");
            return true;
        }
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("Thing", String.class);
            }};
        }
    }

    public static class Value extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records){
            if (e.action == null) records.put("Value", 0);
            else if (e.action instanceof Buy) records.put("Value", ((Buy) e.action).cost);
            else if (e.action instanceof Sell) records.put("Value", ((Sell) e.action).price);
            else if (e.action instanceof SummerBid)
                records.put("Value", ((SummerBid) e.action).beer + 2 * ((SummerBid) e.action).mead);
            else if (e.action instanceof Pray) records.put("Value", ((Pray) e.action).prayerCount);
            else if (e.action instanceof TakeToken)
                records.put("Value", 2 - ((DiceMonasteryGameState)e.state).availableBonusTokens(((TakeToken) e.action).fromArea).size());
            else if (e.action instanceof UseMonk) records.put("Value", ((UseMonk) e.action).getActionPoints());
            else records.put("Value", 0);
            return true;
        }
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("Value", Integer.class);
            }};
        }
    }

    public static class ActionPointsLeft extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records){
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            records.put("ActionPointsLeft", s.getActionPointsLeft());
            return true;
        }
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }
        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("ActionPointsLeft", Integer.class);
            }};
        }
    }

    public static class Turn extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records){
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            records.put("DM Turn", (s.getYear() - 1) * 4 + s.getSeason().ordinal() + 1);
            return true;
        }
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            return new HashMap<String, Class<?>>() {{
                put("DM Turn", Integer.class);
            }};
        }
    }

    public static class ActionAreas extends AbstractMetric {
        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records){
            DiceMonasteryGameState s = ((DiceMonasteryGameState)e.state);
            for (DiceMonasteryConstants.ActionArea a : DiceMonasteryConstants.ActionArea.values()) {
                for (int i = 0; i < s.getNPlayers(); i++) {
                    records.put(a.name() + " - # Monks (Player " + i + ")", s.monksIn(a, i).size());
                    records.put(a.name() + " - # AP (Player " + i + ")", s.monksIn(a, i).stream().mapToDouble(Monk::getPiety).sum());
                    for (DiceMonasteryConstants.Resource r : DiceMonasteryConstants.Resource.values())
                        records.put(a.name() + " - # " + r.name() + " (Player " + i + ")", s.getResource(i, r, a));
                }
            }
            return true;
        }
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> columns = new HashMap<>();
            for (DiceMonasteryConstants.ActionArea a : DiceMonasteryConstants.ActionArea.values()) {
                for (int i = 0; i < nPlayersPerGame; i++) {
                    columns.put(a.name() + " - # Monks (Player " + i + ")", Integer.class);
                    columns.put(a.name() + " - # AP (Player " + i + ")", Double.class);
                    for (DiceMonasteryConstants.Resource r : DiceMonasteryConstants.Resource.values())
                        columns.put(a.name() + " - # " + r.name() +  " (Player " + i + ")", Integer.class);
                }
            }
            return columns;
        }
    }

    public static class RoundOverCounts extends AbstractMetric {
        public Map<String, BiFunction<Event, Integer, Integer>> allCategories = new HashMap<String, BiFunction<Event, Integer, Integer>>() {{
            put("Writing Sets", (e, p) -> Math.min(((DiceMonasteryGameState)e.state).getResource(p, DiceMonasteryConstants.Resource.VELLUM, STOREROOM),
                    ((DiceMonasteryGameState)e.state).getResource(p, DiceMonasteryConstants.Resource.CANDLE, STOREROOM)));
            put("Pigments", (e, p) -> ((DiceMonasteryGameState)e.state).getStores(p, r -> r.isPigment).size());
            put("Inks", (e, p) -> ((DiceMonasteryGameState)e.state).getStores(p, r -> r.isInk).size());
            put("Monks Count", (e, p) -> ((DiceMonasteryGameState)e.state).monksIn(null, p).size());
            put("Piety Sum", (e, p) -> ((DiceMonasteryGameState)e.state).monksIn(null, p).stream().mapToInt(Monk::getPiety).sum());
            put("Victory Points", (e, p) -> ((DiceMonasteryGameState)e.state).getVictoryPoints(p));
            put("Treasure Sum", (e, p) -> ((DiceMonasteryGameState)e.state).getTreasures(p).stream().mapToInt(t -> t.vp).sum());
        }};
        int nPlayers;

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records){
            DiceMonasteryGameState s = (DiceMonasteryGameState) e.state;
            nPlayers = s.getNPlayers();
            for (int i = 0; i < nPlayers; i++) {
                for (String categ: allCategories.keySet()) {
                    records.put(categ + " - Player " + i, allCategories.get(categ).apply(e, i));
                }
            }
            return true;
        }

        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ROUND_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (int i = 0; i < nPlayersPerGame; i++) {
                for (String categ: allCategories.keySet()) {
                    columns.put(categ + " - Player " + i, Integer.class);
                }
            }
            return columns;
        }

        @Override
        public IDataProcessor getDataProcessor() {
            return new TSDPRoundOverCounts();
        }
    }
}

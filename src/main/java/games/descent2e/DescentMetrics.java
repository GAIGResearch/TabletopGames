package games.descent2e;

import core.CoreConstants;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.descent2e.components.Figure;
import games.descent2e.components.Monster;

import java.util.*;

public class DescentMetrics implements IMetricsCollection {

    public static class HealthPoints extends AbstractMetric {

        public HealthPoints() {
            super();
        }

        public HealthPoints(Event.GameEvent... args) {
            super(args);
        }

        public HealthPoints(Set<IGameEvent> events) {
            super(events);
        }

        public HealthPoints(String[] args) {
            super(args);
        }

        public HealthPoints(String[] args, Event.GameEvent... events) {
            super(args, events);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.state.getGameStatus() == CoreConstants.GameResult.WIN_GAME ||
                    e.state.getGameStatus() == CoreConstants.GameResult.TIMEOUT ||
                    e.state.getGameStatus() == CoreConstants.GameResult.LOSE_GAME) {
                DescentGameState dgs = (DescentGameState) e.state;
                for (int i = 0; i < dgs.getHeroes().size(); i++) {
                    records.put("Hero " + (i + 1) + " HP", Integer.toString(dgs.getHeroes().get(i).getAttributeValue(Figure.Attribute.Health)));
                }
                for (int i = dgs.getHeroes().size(); i < 4; i++) {
                    records.put("Hero " + (i + 1) + " HP", "N/A");
                }
                records.put("Overlord Fatigue", Integer.toString(dgs.getOverlord().getAttributeValue(Figure.Attribute.Fatigue)));

                List<Monster> goblins = dgs.getMonsters().get(0);
                List<Monster> barghests = dgs.getMonsters().get(1);

                int index = 0;
                records.put("Goblin Master HP", "Dead");

                for (Monster goblin: goblins)
                {
                    if (goblin.getName().contains("master"))
                    {
                        records.put("Goblin Master HP", Integer.toString(goblin.getAttributeValue(Figure.Attribute.Health)));
                    }
                    else
                    {
                        records.put("Goblin Minion " + (index + 1) + " HP", Integer.toString(goblin.getAttributeValue(Figure.Attribute.Health)));
                        index++;
                    }
                }

                for (int i = index; i < dgs.getOriginalMonsters().get(0).size() - 1; i++)
                {
                    records.put("Goblin Minion " + (i + 1) + " HP", "Dead");
                }

                index = 0;
                records.put("Barghest Master HP", "0");

                for (Monster barghest: barghests)
                {
                    if (barghest.getName().contains("master"))
                    {
                        records.put("Barghest Master HP", Integer.toString(barghest.getAttributeValue(Figure.Attribute.Health)));
                    }
                    else
                    {
                        records.put("Barghest Minion " + (index + 1) + " HP", Integer.toString(barghest.getAttributeValue(Figure.Attribute.Health)));
                        index++;
                    }
                }

                for (int i = index; i < dgs.getOriginalMonsters().get(1).size() - 1; i++)
                {
                    records.put("Barghest Minion " + (i + 1) + " HP", "Dead");
                }
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            retVal.put("Hero 1 HP", String.class);
            retVal.put("Hero 2 HP", String.class);
            retVal.put("Hero 3 HP", String.class);
            retVal.put("Hero 4 HP", String.class);
            retVal.put("Overlord Fatigue", String.class);
            retVal.put("Goblin Master HP", String.class);
            retVal.put("Goblin Minion 1 HP", String.class);
            retVal.put("Goblin Minion 2 HP", String.class);
            retVal.put("Goblin Minion 3 HP", String.class);
            retVal.put("Goblin Minion 4 HP", String.class);
            retVal.put("Barghest Master HP", String.class);
            retVal.put("Barghest Minion 1 HP", String.class);
            retVal.put("Barghest Minion 2 HP", String.class);
            retVal.put("Barghest Minion 3 HP", String.class);
            return retVal;
        }
    }

        public static class WinnerType extends AbstractMetric {

        public WinnerType() {
            super();
        }

        public WinnerType(Event.GameEvent... args) {
            super(args);
        }
        public WinnerType(Set<IGameEvent> events) {
            super(events);
        }

        public WinnerType(String[] args) {
            super(args);
        }

        public WinnerType(String[] args, Event.GameEvent... events) {
            super(args, events);
        }


        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.state.getPlayerResults()[0] == CoreConstants.GameResult.WIN_GAME)
                records.put("WinnerType", "overlord");
            else if (e.state.getPlayerResults()[0] == CoreConstants.GameResult.TIMEOUT)
                records.put("WinnerType", "timeout");
            else
                records.put("WinnerType", "hero");
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            retVal.put("WinnerType", String.class);
            return retVal;
        }
    }

}

package games.descent2e;

import core.CoreConstants;
import core.actions.LogEvent;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.descent2e.components.Figure;
import games.descent2e.components.Hero;
import games.descent2e.components.Monster;
import utilities.Pair;
import utilities.Utils;
import utilities.Vector2D;

import java.util.*;

import static evaluation.metrics.Event.GameEvent.*;
import static games.descent2e.components.Figure.Attribute.Health;

public class DescentMetrics implements IMetricsCollection {

    public static class Heuristics extends AbstractMetric
    {
        public Heuristics() {
            super();
        }
        public Heuristics(Event.GameEvent... args) {
            super(args);
        }
        public Heuristics(Set<IGameEvent> events) {
            super(events);
        }
        public Heuristics(String[] args) {
            super(args);
        }
        public Heuristics(String[] args, Event.GameEvent... events) {
            super(args, events);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DescentGameState dgs = (DescentGameState) e.state;
            int player = dgs.getHeroes().get(0).getOwnerId();
            List<Double> heuristics = dgs.getHeuristicValues(player);
            records.put("FACTOR_HEROES_HP", heuristics.get(0));
            records.put("FACTOR_HEROES_DEFEATED", heuristics.get(1));
            records.put("FACTOR_OVERLORD_FATIGUE", heuristics.get(2));
            records.put("FACTOR_OVERLORD_THREAT", heuristics.get(3));
            records.put("FACTOR_MONSTERS_HP", heuristics.get(4));
            records.put("FACTOR_MONSTERS_DEFEATED", heuristics.get(5));
            records.put("FACTOR_HEROES_THREAT", heuristics.get(6));

            double sum = 0.0;

            if (e.type == GAME_OVER)
                sum = dgs.getPlayerResults()[player].value;
            else {
                sum = heuristics.stream().mapToDouble(Double::doubleValue).sum();
                Utils.clamp(sum, -0.99, 0.99);
            }

            records.put("HEURISTICS SCORE", sum);

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(ABOUT_TO_START);
                add(Event.GameEvent.ROUND_OVER);
                add(GAME_OVER);
            }};
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            retVal.put("FACTOR_HEROES_HP", Double.class);
            retVal.put("FACTOR_HEROES_DEFEATED", Double.class);
            retVal.put("FACTOR_OVERLORD_FATIGUE", Double.class);
            retVal.put("FACTOR_OVERLORD_THREAT", Double.class);
            retVal.put("FACTOR_MONSTERS_HP", Double.class);
            retVal.put("FACTOR_MONSTERS_DEFEATED", Double.class);
            retVal.put("FACTOR_HEROES_THREAT", Double.class);
            retVal.put("HEURISTICS SCORE", Double.class);
            return retVal;
        }

    }
    public static class Distances extends AbstractMetric
    {
        public Distances() {
            super();
        }

        public Distances(Event.GameEvent... args) {
            super(args);
        }

        public Distances(Set<IGameEvent> events) {
            super(events);
        }

        public Distances(String[] args) {
            super(args);
        }

        public Distances(String[] args, Event.GameEvent... events) {
            super(args, events);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DescentGameState dgs = (DescentGameState) e.state;
            List<Figure> heroes = new ArrayList<>(dgs.getHeroes());
            List<Figure> goblins = new ArrayList<>(dgs.getMonsters().get(0));
            List<Figure> barghests = new ArrayList<>(dgs.getMonsters().get(1));
            for (int i = 0; i < dgs.getHeroes().size(); i++) {
                Figure hero = heroes.get(i);
                records.put("Hero " + (i + 1) + " Name", hero.getName().replace("Hero: ", ""));
                records.put("Hero " + (i + 1) + " Position", hero.getPosition().toString());
                records.put("Hero " + (i + 1) + " Nearest Hero", (DescentHelper.distanceFromNearestAlly(dgs, hero, heroes)));
                records.put("Hero " + (i + 1) + " Farthest Hero", (DescentHelper.distanceFromFurthestAlly(dgs, hero, heroes)));
                records.put("Hero " + (i + 1) + " Average Hero", (DescentHelper.averageDistanceFromAllies(dgs, hero, heroes)));
                records.put("Hero " + (i + 1) + " Nearest Goblin", (DescentHelper.distanceFromNearestAlly(dgs, hero, goblins)));
                records.put("Hero " + (i + 1) + " Farthest Goblin", (DescentHelper.distanceFromFurthestAlly(dgs, hero, goblins)));
                records.put("Hero " + (i + 1) + " Average Goblin", (DescentHelper.averageDistanceFromAllies(dgs, hero, goblins)));
                records.put("Hero " + (i + 1) + " Nearest Barghest", (DescentHelper.distanceFromNearestAlly(dgs, hero, barghests)));
                records.put("Hero " + (i + 1) + " Farthest Barghest", (DescentHelper.distanceFromFurthestAlly(dgs, hero, barghests)));
                records.put("Hero " + (i + 1) + " Average Barghest", (DescentHelper.averageDistanceFromAllies(dgs, hero, barghests)));
            }

            for (int i = 0; i < goblins.size(); i++)
            {
                Figure goblin = goblins.get(i);
                if (goblin.getName().contains("master"))
                {
                    records.put("Goblin Master Position", goblin.getPosition().toString());
                    records.put("Goblin Master Nearest Hero", (DescentHelper.distanceFromNearestAlly(dgs, goblin, heroes)));
                    records.put("Goblin Master Farthest Hero", (DescentHelper.distanceFromFurthestAlly(dgs, goblin, heroes)));
                    records.put("Goblin Master Average Hero", (DescentHelper.averageDistanceFromAllies(dgs, goblin, heroes)));
                    records.put("Goblin Master Nearest Goblin", (DescentHelper.distanceFromNearestAlly(dgs, goblin, goblins)));
                    records.put("Goblin Master Farthest Goblin", (DescentHelper.distanceFromFurthestAlly(dgs, goblin, goblins)));
                    records.put("Goblin Master Average Goblin", (DescentHelper.averageDistanceFromAllies(dgs, goblin, goblins)));
                    records.put("Goblin Master Nearest Barghest", (DescentHelper.distanceFromNearestAlly(dgs, goblin, barghests)));
                    records.put("Goblin Master Farthest Barghest", (DescentHelper.distanceFromFurthestAlly(dgs, goblin, barghests)));
                    records.put("Goblin Master Average Barghest", (DescentHelper.averageDistanceFromAllies(dgs, goblin, barghests)));
                }
                else
                {
                    int index = Integer.parseInt(goblin.getName().replace("Goblin Archer minion ", ""));
                    records.put("Goblin Minion " + index + " Position", goblin.getPosition().toString());
                    records.put("Goblin Minion " + index + " Nearest Hero", (DescentHelper.distanceFromNearestAlly(dgs, goblin, heroes)));
                    records.put("Goblin Minion " + index + " Farthest Hero", (DescentHelper.distanceFromFurthestAlly(dgs, goblin, heroes)));
                    records.put("Goblin Minion " + index + " Average Hero", (DescentHelper.averageDistanceFromAllies(dgs, goblin, heroes)));
                    records.put("Goblin Minion " + index + " Nearest Goblin", (DescentHelper.distanceFromNearestAlly(dgs, goblin, goblins)));
                    records.put("Goblin Minion " + index + " Farthest Goblin", (DescentHelper.distanceFromFurthestAlly(dgs, goblin, goblins)));
                    records.put("Goblin Minion " + index + " Average Goblin", (DescentHelper.averageDistanceFromAllies(dgs, goblin, goblins)));
                    records.put("Goblin Minion " + index + " Nearest Barghest", (DescentHelper.distanceFromNearestAlly(dgs, goblin, barghests)));
                    records.put("Goblin Minion " + index + " Farthest Barghest", (DescentHelper.distanceFromFurthestAlly(dgs, goblin, barghests)));
                    records.put("Goblin Minion " + index + " Average Barghest", (DescentHelper.averageDistanceFromAllies(dgs, goblin, barghests)));
                }
            }

            for (int i = 0; i < barghests.size(); i++)
            {
                Figure barghest = barghests.get(i);
                if (barghest.getName().contains("master"))
                {
                    records.put("Barghest Master Position", barghest.getPosition().toString());
                    records.put("Barghest Master Nearest Hero", (DescentHelper.distanceFromNearestAlly(dgs, barghest, heroes)));
                    records.put("Barghest Master Farthest Hero", (DescentHelper.distanceFromFurthestAlly(dgs, barghest, heroes)));
                    records.put("Barghest Master Average Hero", (DescentHelper.averageDistanceFromAllies(dgs, barghest, heroes)));
                    records.put("Barghest Master Nearest Goblin", (DescentHelper.distanceFromNearestAlly(dgs, barghest, goblins)));
                    records.put("Barghest Master Farthest Goblin", (DescentHelper.distanceFromFurthestAlly(dgs, barghest, goblins)));
                    records.put("Barghest Master Average Goblin", (DescentHelper.averageDistanceFromAllies(dgs, barghest, goblins)));
                    records.put("Barghest Master Nearest Barghest", (DescentHelper.distanceFromNearestAlly(dgs, barghest, barghests)));
                    records.put("Barghest Master Farthest Barghest", (DescentHelper.distanceFromFurthestAlly(dgs, barghest, barghests)));
                    records.put("Barghest Master Average Barghest", (DescentHelper.averageDistanceFromAllies(dgs, barghest, barghests)));
                }
                else
                {
                    int index = Integer.parseInt(barghest.getName().replace("Barghest minion ", ""));
                    records.put("Barghest Minion " + index + " Position", barghest.getPosition().toString());
                    records.put("Barghest Minion " + index + " Nearest Hero", (DescentHelper.distanceFromNearestAlly(dgs, barghest, heroes)));
                    records.put("Barghest Minion " + index + " Farthest Hero", (DescentHelper.distanceFromFurthestAlly(dgs, barghest, heroes)));
                    records.put("Barghest Minion " + index + " Average Hero", (DescentHelper.averageDistanceFromAllies(dgs, barghest, heroes)));
                    records.put("Barghest Minion " + index + " Nearest Goblin", (DescentHelper.distanceFromNearestAlly(dgs, barghest, goblins)));
                    records.put("Barghest Minion " + index + " Farthest Goblin", (DescentHelper.distanceFromFurthestAlly(dgs, barghest, goblins)));
                    records.put("Barghest Minion " + index + " Average Goblin", (DescentHelper.averageDistanceFromAllies(dgs, barghest, goblins)));
                    records.put("Barghest Minion " + index + " Nearest Barghest", (DescentHelper.distanceFromNearestAlly(dgs, barghest, barghests)));
                    records.put("Barghest Minion " + index + " Farthest Barghest", (DescentHelper.distanceFromFurthestAlly(dgs, barghest, barghests)));
                    records.put("Barghest Minion " + index + " Average Barghest", (DescentHelper.averageDistanceFromAllies(dgs, barghest, barghests)));
                }
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(Event.GameEvent.ROUND_OVER);
                add(GAME_OVER);
            }};
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            for (int i = 0; i < 4; i++) {
                retVal.put("Hero " + (i + 1) + " Name", String.class);
                retVal.put("Hero " + (i + 1) + " Position", String.class);
                retVal.put("Hero " + (i + 1) + " Nearest Hero", Double.class);
                retVal.put("Hero " + (i + 1) + " Farthest Hero", Double.class);
                retVal.put("Hero " + (i + 1) + " Average Hero", Double.class);
                retVal.put("Hero " + (i + 1) + " Nearest Goblin", Double.class);
                retVal.put("Hero " + (i + 1) + " Farthest Goblin", Double.class);
                retVal.put("Hero " + (i + 1) + " Average Goblin", Double.class);
                retVal.put("Hero " + (i + 1) + " Nearest Barghest", Double.class);
                retVal.put("Hero " + (i + 1) + " Farthest Barghest", Double.class);
                retVal.put("Hero " + (i + 1) + " Average Barghest", Double.class);
            }

            retVal.put("Goblin Master Position", String.class);
            retVal.put("Goblin Master Nearest Hero", Double.class);
            retVal.put("Goblin Master Farthest Hero", Double.class);
            retVal.put("Goblin Master Average Hero", Double.class);
            retVal.put("Goblin Master Nearest Goblin", Double.class);
            retVal.put("Goblin Master Farthest Goblin", Double.class);
            retVal.put("Goblin Master Average Goblin", Double.class);
            retVal.put("Goblin Master Nearest Barghest", Double.class);
            retVal.put("Goblin Master Farthest Barghest", Double.class);
            retVal.put("Goblin Master Average Barghest", Double.class);

            for (int i = 0; i < 4; i++)
            {
                retVal.put("Goblin Minion " + (i + 1) + " Position", String.class);
                retVal.put("Goblin Minion " + (i + 1) + " Nearest Hero", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Farthest Hero", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Average Hero", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Nearest Goblin", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Farthest Goblin", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Average Goblin", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Nearest Barghest", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Farthest Barghest", Double.class);
                retVal.put("Goblin Minion " + (i + 1) + " Average Barghest", Double.class);
            }

            retVal.put("Barghest Master Position", String.class);
            retVal.put("Barghest Master Nearest Hero", Double.class);
            retVal.put("Barghest Master Farthest Hero", Double.class);
            retVal.put("Barghest Master Average Hero", Double.class);
            retVal.put("Barghest Master Nearest Goblin", Double.class);
            retVal.put("Barghest Master Farthest Goblin", Double.class);
            retVal.put("Barghest Master Average Goblin", Double.class);
            retVal.put("Barghest Master Nearest Barghest", Double.class);
            retVal.put("Barghest Master Farthest Barghest", Double.class);
            retVal.put("Barghest Master Average Barghest", Double.class);

            for (int i = 0; i < 3; i++)
            {
                retVal.put("Barghest Minion " + (i + 1) + " Position", String.class);
                retVal.put("Barghest Minion " + (i + 1) + " Nearest Hero", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Farthest Hero", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Average Hero", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Nearest Goblin", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Farthest Goblin", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Average Goblin", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Nearest Barghest", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Farthest Barghest", Double.class);
                retVal.put("Barghest Minion " + (i + 1) + " Average Barghest", Double.class);
            }

            return retVal;
        }
    }

    public static class Heroes extends AbstractMetric
    {
        public Heroes() {
            super();
        }

        public Heroes(Event.GameEvent... args) {
            super(args);
        }

        public Heroes(Set<IGameEvent> events) {
            super(events);
        }

        public Heroes(String[] args) {
            super(args);
        }

        public Heroes(String[] args, Event.GameEvent... events) {
            super(args, events);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DescentGameState dgs = (DescentGameState) e.state;
            for (int i = 0; i < dgs.getHeroes().size(); i++) {
                Hero hero = dgs.getHeroes().get(i);
                records.put("Hero " + (i + 1), hero.getName().replace("Hero: ", ""));
                records.put("Hero " + (i + 1) + " Archetype", hero.getProperty("archetype").toString());
                records.put("Hero " + (i + 1) + " Class", hero.getProperty("class").toString());
                records.put("Hero " + (i + 1) + " Used Feat", !hero.isFeatAvailable());
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(ABOUT_TO_START);
                add(Event.GameEvent.ROUND_OVER);
                add(GAME_OVER);
            }};
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            // If we only have two Players (i.e. 1 Hero Player), we need to add another column for the second hero
            int players = nPlayersPerGame;
            if (nPlayersPerGame == 2) players++;
            for (int i = 0; i < players-1; i++) {
                retVal.put("Hero " + (i + 1), String.class);
                retVal.put("Hero " + (i + 1) + " Archetype", String.class);
                retVal.put("Hero " + (i + 1) + " Class", String.class);
                retVal.put("Hero " + (i + 1) + " Used Feat", Boolean.class);
            }
            return retVal;
        }
    }

    public static class Kills extends AbstractMetric
    {
        public Kills() {
            super();
        }

        public Kills(Event.GameEvent... args) {
            super(args);
        }

        public Kills(Set<IGameEvent> events) {
            super(events);
        }

        public Kills(String[] args) {
            super(args);
        }

        public Kills(String[] args, Event.GameEvent... events) {
            super(args, events);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DescentGameState dgs = (DescentGameState) e.state;

            List<Pair<String, String>> kills = dgs.getDefeatedFigures();

            for (Pair<String, String> kill : kills) {
                String[] a = kill.a.split(";");
                String[] b = kill.b.split(";");

                String target = a[0];
                String killer = b[0];

                int index1 = Integer.parseInt(a[1]);
                int index2 = Integer.parseInt(b[1]);

                // Occurs if the Master is defeated, thus Minion 1 is considered 0
                if (index1 == 0) index1++;

                if (target.contains("Hero:"))
                {
                    records.put("Hero " + index1, target.replace("Hero: ", "") + " defeated by " + killer);
                }
                else
                {
                    if (target.contains("Goblin"))
                    {
                        if (target.contains("master"))
                        {
                            records.put("Goblin Master", target + " defeated by " + killer.replace("Hero: ", ""));
                        }
                        else
                        {
                            index1 = Integer.parseInt(target.replace("Goblin Archer minion ", "").substring(0, 1));
                            records.put("Goblin Minion " + index1, target + " defeated by " + killer.replace("Hero: ", ""));
                        }
                    }
                    else
                    {
                        if (target.contains("Barghest"))
                        {
                            if (target.contains("master"))
                            {
                                records.put("Barghest Master", target + " defeated by " + killer.replace("Hero: ", ""));
                            }
                            else
                            {
                                index1 = Integer.parseInt(target.replace("Barghest minion ", "").substring(0, 1));
                                records.put("Barghest Minion " + index1, target + " defeated by " + killer.replace("Hero: ", ""));
                            }
                        }
                    }
                }
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(Event.GameEvent.ROUND_OVER);
                add(GAME_OVER);
            }};
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            retVal.put("Hero 1", String.class);
            retVal.put("Hero 2", String.class);
            retVal.put("Hero 3", String.class);
            retVal.put("Hero 4", String.class);
            retVal.put("Goblin Master", String.class);
            retVal.put("Goblin Minion 1", String.class);
            retVal.put("Goblin Minion 2", String.class);
            retVal.put("Goblin Minion 3", String.class);
            retVal.put("Goblin Minion 4", String.class);
            retVal.put("Barghest Master", String.class);
            retVal.put("Barghest Minion 1", String.class);
            retVal.put("Barghest Minion 2", String.class);
            retVal.put("Barghest Minion 3", String.class);
            return retVal;
        }
    }

    public static class Positions extends AbstractMetric {

        public Positions() {
            super();
        }

        public Positions(Event.GameEvent... args) {
            super(args);
        }

        public Positions(Set<IGameEvent> events) {
            super(events);
        }

        public Positions(String[] args) {
            super(args);
        }

        public Positions(String[] args, Event.GameEvent... events) {
            super(args, events);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            DescentGameState dgs = (DescentGameState) e.state;
            for (int i = 0; i < dgs.getHeroes().size(); i++) {
                Vector2D pos = dgs.getHeroes().get(i).getPosition();
                records.put("Hero " + (i + 1), pos.toString());
            }

            List<Monster> goblins = dgs.getMonsters().get(0);
            List<Monster> barghests = dgs.getMonsters().get(1);

            int index = 0;
            records.put("Goblin Master", "Dead");
            records.put("Goblin Minion 1", "Dead");
            records.put("Goblin Minion 2", "Dead");
            records.put("Goblin Minion 3", "Dead");
            records.put("Goblin Minion 4", "Dead");

            for (Monster goblin: goblins)
            {
                Vector2D pos = goblin.getPosition();
                if (goblin.getName().contains("master"))
                {
                    records.put("Goblin Master", pos.toString());
                }
                else
                {
                    index = Integer.parseInt(goblin.getName().replace("Goblin Archer minion ", ""));
                    records.put("Goblin Minion " + index, pos.toString());
                }
            }

            records.put("Barghest Master", "Dead");
            records.put("Barghest Minion 1", "Dead");
            records.put("Barghest Minion 2", "Dead");
            records.put("Barghest Minion 3", "Dead");

            for (Monster barghest: barghests)
            {
                Vector2D pos = barghest.getPosition();
                if (barghest.getName().contains("master"))
                {
                    records.put("Barghest Master", pos.toString());
                }
                else
                {
                    index = Integer.parseInt(barghest.getName().replace("Barghest minion ", ""));
                    records.put("Barghest Minion " + index, pos.toString());
                }
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(ABOUT_TO_START);
                add(Event.GameEvent.ROUND_OVER);
                add(GAME_OVER);
            }};
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            retVal.put("Hero 1", String.class);
            retVal.put("Hero 2", String.class);
            retVal.put("Hero 3", String.class);
            retVal.put("Hero 4", String.class);
            retVal.put("Goblin Master", String.class);
            retVal.put("Goblin Minion 1", String.class);
            retVal.put("Goblin Minion 2", String.class);
            retVal.put("Goblin Minion 3", String.class);
            retVal.put("Goblin Minion 4", String.class);
            retVal.put("Barghest Master", String.class);
            retVal.put("Barghest Minion 1", String.class);
            retVal.put("Barghest Minion 2", String.class);
            retVal.put("Barghest Minion 3", String.class);
            return retVal;
        }
    }

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
            DescentGameState dgs = (DescentGameState) e.state;
            for (int i = 0; i < dgs.getHeroes().size(); i++) {
                records.put("Hero " + (i + 1) + " HP", Integer.toString(dgs.getHeroes().get(i).getAttributeValue(Figure.Attribute.Health)));
            }
            records.put("Overlord Fatigue", Integer.toString(dgs.getOverlord().getAttributeValue(Figure.Attribute.Fatigue)));

            List<Monster> goblins = dgs.getMonsters().get(0);
            List<Monster> barghests = dgs.getMonsters().get(1);

            int index = 0;
            records.put("Goblin Master HP", "Dead");
            records.put("Goblin Minion 1 HP", "Dead");
            records.put("Goblin Minion 2 HP", "Dead");
            records.put("Goblin Minion 3 HP", "Dead");
            records.put("Goblin Minion 4 HP", "Dead");

            for (Monster goblin: goblins)
            {
                if (goblin.getName().contains("master"))
                {
                    records.put("Goblin Master HP", Integer.toString(goblin.getAttributeValue(Figure.Attribute.Health)));
                }
                else
                {
                    index = Integer.parseInt(goblin.getName().replace("Goblin Archer minion ", ""));
                    records.put("Goblin Minion " + index + " HP", Integer.toString(goblin.getAttributeValue(Figure.Attribute.Health)));
                }
            }

            records.put("Barghest Master HP", "Dead");
            records.put("Barghest Minion 1 HP", "Dead");
            records.put("Barghest Minion 2 HP", "Dead");
            records.put("Barghest Minion 3 HP", "Dead");

            for (Monster barghest: barghests)
            {
                if (barghest.getName().contains("master"))
                {
                    records.put("Barghest Master HP", Integer.toString(barghest.getAttributeValue(Figure.Attribute.Health)));
                }
                else
                {
                    index = Integer.parseInt(barghest.getName().replace("Barghest minion ", ""));
                    records.put("Barghest Minion " + index + " HP", Integer.toString(barghest.getAttributeValue(Figure.Attribute.Health)));
                }
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(Event.GameEvent.ROUND_OVER);
                add(GAME_OVER);
            }};
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
            return Collections.singleton(GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            retVal.put("WinnerType", String.class);
            return retVal;
        }
    }

    public static class NarrativeActions extends AbstractMetric {

        public NarrativeActions() {
            super();
        }

        public NarrativeActions(Event.GameEvent... args) {
            super(args);
        }

        public NarrativeActions(Set<IGameEvent> events) {
            super(events);
        }

        public NarrativeActions(String[] args) {
            super(args);
        }

        public NarrativeActions(String[] args, Event.GameEvent... events) {
            super(args, events);
        }

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == ABOUT_TO_START)
            {
                records.put("Name", "Game Start");
                records.put("ID", 0);
                records.put("Position", "-");
                String setup = getSetup((DescentGameState) e.state).toString();
                records.put("Actions Taken", setup);
                return true;
            }

            if (e.type == GAME_OVER) {
                records.put("Name", "Game Over");
                records.put("ID", 1);
                records.put("Position", "-");
                String setup = getSetup((DescentGameState) e.state).toString();
                records.put("Actions Taken", setup);
                return true;
            }

            if (e.type == Event.GameEvent.GAME_EVENT) {
                String[] text = ((LogEvent)e.action).text.split(":")[1].split(";");
                String figure = text[0].trim();
                int componentID = Integer.parseInt(text[1].trim());
                String position = text[2].trim();

                records.put("Name", figure);
                records.put("ID", componentID);
                records.put("Position", position);

                DescentGameState dgs = (DescentGameState) e.state;
                Figure f = (Figure) dgs.getComponentById(componentID);
                records.put("Actions Taken", f.getActionsTaken().toString());
                //records.put("Actions Taken", getActionsTaken(dgs, f).toString());

                return true;
            }
            return false;
        }

        private List<String> getSetup(DescentGameState dgs)
        {
            List<String> setup = new ArrayList<>();
            for (Hero hero : dgs.getHeroes())
            {
                String h = hero.getComponentName().replace("Hero: ", "") + " - " + hero.getProperty("archetype") + " (" + hero.getProperty("class") + ") - " +
                        hero.getAttributeValue(Health) + " HP - " + hero.getHandEquipment().toString() + " - " + hero.getPosition().toString();
                setup.add(h);
            }
            for (int i = 0; i < dgs.getMonsters().size(); i++)
            {
                for (Monster monster : dgs.getMonsters().get(i))
                {
                    String m = monster.getComponentName() + " - " + monster.getAttributeValue(Health) + " HP - " + monster.getPosition().toString();
                    setup.add(m);
                }
            }
            return setup;
        }

        private List<String> getActionsTaken(DescentGameState dgs, Figure f) {

            // The list needs to stay in approximately the same order as it was originally

            List<String> actionsTaken = f.getActionsTaken();
            List<Integer> moves = new ArrayList<>();
            for (int i = 0; i < actionsTaken.size(); i++)
            {
                if (actionsTaken.get(i).contains("Attack by"))
                {
                    String attack = actionsTaken.get(i);
                    String attackType = attack.split("by")[0].replace("Free", "").replace("Heroic Feat:", "").trim();
                    String target = attack.split("Target:")[1].split(";")[0].trim();
                    String result = attack.split("Result:")[1].split(";")[0].trim();
                    String damage = attack.split("Damage:")[1].split(";")[0].trim();

                    String newAttack = attackType + " on " + target;
                    switch (result){
                        case "Missed":
                            newAttack += " - Missed";
                            break;
                        case "Kill":
                            newAttack += " - Defeated";
                            break;
                        case "Hit":
                            if (damage.equals("0"))
                                newAttack += " - Blocked (No Damage)";
                            else
                                newAttack += " - Hit (" + damage + " Damage)";
                            break;
                    }
                    actionsTaken.set(i, newAttack);
                }

                if (actionsTaken.get(i).contains("Move by"))
                {
                    moves.add(i);
                }
            }
            if (!moves.isEmpty()) {

                String lastMove = actionsTaken.get(moves.get(moves.size() - 1));
                actionsTaken.set(moves.get(moves.size() - 1), "Move to " + lastMove.split("to ")[1]);

                // Get rid of all the Move actions except the last one
                if (moves.size() > 1) {
                    for (int i = moves.size() - 1; i > 0; i--) {
                        actionsTaken.remove(moves.get(i - 1).intValue());
                    }
                }
            }
            // Remove gaining Movement Point references
            actionsTaken.removeIf(s -> s.contains("Movement Point"));
            // Remove Surge references
            actionsTaken.removeIf(s -> s.contains("Surge"));

            // For some reason, the End Turn action is added twice sometimes
            // TODO: Figure out why this is happening
            actionsTaken.removeIf(s -> s.contains("End Turn"));
            actionsTaken.add("End Turn");

            return actionsTaken;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<IGameEvent>() {{
                add(ABOUT_TO_START);
                add(Event.GameEvent.GAME_EVENT);
                add(GAME_OVER);
            }};
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            HashMap<String, Class<?>> retVal = new HashMap<>();
            retVal.put("Name", String.class);
            retVal.put("ID", Integer.class);
            retVal.put("Position", String.class);
            retVal.put("Actions Taken", String.class);
            return retVal;
        }
    }

}

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
            List<Figure> allMonsters = new ArrayList<>(dgs.getAllMonsters());
            for (int i = 0; i < dgs.getHeroes().size(); i++) {
                Figure hero = heroes.get(i);
                records.put("Hero " + (i + 1) + " Name", hero.getName().replace("Hero: ", ""));
                records.put("Hero " + (i + 1) + " Position", hero.getPosition().toString());
                records.put("Hero " + (i + 1) + " Nearest Hero", (DescentHelper.distanceFromNearestAlly(dgs, hero, heroes)));
                records.put("Hero " + (i + 1) + " Farthest Hero", (DescentHelper.distanceFromFurthestAlly(dgs, hero, heroes)));
                records.put("Hero " + (i + 1) + " Average Hero", (DescentHelper.averageDistanceFromAllies(dgs, hero, heroes)));
                records.put("Hero " + (i + 1) + " Nearest Monster", (DescentHelper.distanceFromNearestAlly(dgs, hero, allMonsters)));
                records.put("Hero " + (i + 1) + " Farthest Monster", (DescentHelper.distanceFromFurthestAlly(dgs, hero, allMonsters)));
                records.put("Hero " + (i + 1) + " Average Monster", (DescentHelper.averageDistanceFromAllies(dgs, hero, allMonsters)));
            }

            for (Figure monster : allMonsters)
            {
                records.put(monster.getName() + " Position", monster.getPosition().toString());
                records.put(monster.getName() + " Nearest Hero", (DescentHelper.distanceFromNearestAlly(dgs, monster, heroes)));
                records.put(monster.getName() + " Farthest Hero", (DescentHelper.distanceFromFurthestAlly(dgs, monster, heroes)));
                records.put(monster.getName() + " Average Hero", (DescentHelper.averageDistanceFromAllies(dgs, monster, heroes)));
                records.put(monster.getName() + " Nearest Monster", (DescentHelper.distanceFromNearestAlly(dgs, monster, allMonsters)));
                records.put(monster.getName() + " Farthest Monster", (DescentHelper.distanceFromFurthestAlly(dgs, monster, allMonsters)));
                records.put(monster.getName() + " Average Monster", (DescentHelper.averageDistanceFromAllies(dgs, monster, allMonsters)));

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
                retVal.put("Hero " + (i + 1) + " Nearest Monster", Double.class);
                retVal.put("Hero " + (i + 1) + " Farthest Monster", Double.class);
                retVal.put("Hero " + (i + 1) + " Average Monster", Double.class);
            }

            String[] monsterNames = {"Goblin Archer", "Barghest", "Zombie", "Cave Spider", "Flesh Moulder",
                                        "Ettin", "Elemental", "Merriod", "Shadow Dragon"};
            Integer[] minionCount = {4, 3, 4, 4, 3, 1, 1, 1, 1};
            String[] lieutenantNames = {"Sir Alric Farrow", "Belthir", "Lady Eliza Farrow",
                                        "Lord Merick Farrow", "Splig", "Baron Zachareth"};
            int index = 0;
            for (String name : monsterNames)
            {
                retVal.put(name + " master Position", String.class);
                retVal.put(name + " master Nearest Hero", Double.class);
                retVal.put(name + " master Farthest Hero", Double.class);
                retVal.put(name + " master Average Hero", Double.class);
                retVal.put(name + " master Nearest Monster", Double.class);
                retVal.put(name + " master Farthest Monster", Double.class);
                retVal.put(name + " master Average Monster", Double.class);

                for (int i = 0; i < minionCount[index]; i++)
                {
                    retVal.put(name + " minion " + (i + 1) + " Position", String.class);
                    retVal.put(name + " minion " + (i + 1) + " Nearest Hero", Double.class);
                    retVal.put(name + " minion " + (i + 1) + " Farthest Hero", Double.class);
                    retVal.put(name + " minion " + (i + 1) + " Average Hero", Double.class);
                    retVal.put(name + " minion " + (i + 1) + " Nearest Monster", Double.class);
                    retVal.put(name + " minion " + (i + 1) + " Farthest Monster", Double.class);
                    retVal.put(name + " minion " + (i + 1) + " Average Monster", Double.class);
                }
            }

            for (String name : lieutenantNames) {
                retVal.put(name + " Position", String.class);
                retVal.put(name + " Nearest Hero", Double.class);
                retVal.put(name + " Farthest Hero", Double.class);
                retVal.put(name + " Average Hero", Double.class);
                retVal.put(name + " Nearest Monster", Double.class);
                retVal.put(name + " Farthest Monster", Double.class);
                retVal.put(name + " Average Monster", Double.class);
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

                // Occurs if the master is defeated, thus minion 1 is considered 0
                if (index1 == 0) index1++;

                if (target.contains("Hero:"))
                    records.put("Hero " + index1, target.replace("Hero: ", "") + " defeated by " + killer);
                else
                    records.put(target.split(" \\(")[0], target + " defeated by " + killer.replace("Hero: ", ""));

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
            retVal.put("Goblin Archer master", String.class);
            retVal.put("Goblin Archer minion 1", String.class);
            retVal.put("Goblin Archer minion 2", String.class);
            retVal.put("Goblin Archer minion 3", String.class);
            retVal.put("Goblin Archer minion 4", String.class);
            retVal.put("Barghest master", String.class);
            retVal.put("Barghest minion 1", String.class);
            retVal.put("Barghest minion 2", String.class);
            retVal.put("Barghest minion 3", String.class);
            retVal.put("Zombie master", String.class);
            retVal.put("Zombie minion 1", String.class);
            retVal.put("Zombie minion 2", String.class);
            retVal.put("Zombie minion 3", String.class);
            retVal.put("Zombie minion 4", String.class);
            retVal.put("Cave Spider master", String.class);
            retVal.put("Cave Spider minion 1", String.class);
            retVal.put("Cave Spider minion 2", String.class);
            retVal.put("Cave Spider minion 3", String.class);
            retVal.put("Cave Spider minion 4", String.class);
            retVal.put("Flesh Moulder master", String.class);
            retVal.put("Flesh Moulder minion 1", String.class);
            retVal.put("Flesh Moulder minion 2", String.class);
            retVal.put("Flesh Moulder minion 3", String.class);
            retVal.put("Ettin master", String.class);
            retVal.put("Ettin minion 1", String.class);
            retVal.put("Elemental master", String.class);
            retVal.put("Elemental minion 1", String.class);
            retVal.put("Merriod master", String.class);
            retVal.put("Merriod minion 1", String.class);
            retVal.put("Shadow Dragon master", String.class);
            retVal.put("Shadow Dragon minion 1", String.class);
            retVal.put("Sir Alric Farrow", String.class);
            retVal.put("Belthir", String.class);
            retVal.put("Lady Eliza Farrow", String.class);
            retVal.put("Lord Merick Farrow", String.class);
            retVal.put("Splig", String.class);
            retVal.put("Baron Zachareth", String.class);
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
                if (pos == null)
                    records.put("Hero " + (i + 1), "Spawning");
                else
                    records.put("Hero " + (i + 1), pos.toString());
            }

            List<Monster> allOriginalMonsters = dgs.getAllOriginalMonsters();
            List<Monster> allMonsters = dgs.getAllMonsters();

            for (Monster monster : allOriginalMonsters)
                records.put(monster.getName(), "Dead");

            for (Monster monster : allMonsters)
            {
                Vector2D pos = monster.getPosition();
                if (pos == null)
                    records.put(monster.getName(), "Spawning");
                else
                    records.put(monster.getName(), pos.toString());
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
            retVal.put("Goblin Archer master", String.class);
            retVal.put("Goblin Archer minion 1", String.class);
            retVal.put("Goblin Archer minion 2", String.class);
            retVal.put("Goblin Archer minion 3", String.class);
            retVal.put("Goblin Archer minion 4", String.class);
            retVal.put("Barghest master", String.class);
            retVal.put("Barghest minion 1", String.class);
            retVal.put("Barghest minion 2", String.class);
            retVal.put("Barghest minion 3", String.class);
            retVal.put("Zombie master", String.class);
            retVal.put("Zombie minion 1", String.class);
            retVal.put("Zombie minion 2", String.class);
            retVal.put("Zombie minion 3", String.class);
            retVal.put("Zombie minion 4", String.class);
            retVal.put("Cave Spider master", String.class);
            retVal.put("Cave Spider minion 1", String.class);
            retVal.put("Cave Spider minion 2", String.class);
            retVal.put("Cave Spider minion 3", String.class);
            retVal.put("Cave Spider minion 4", String.class);
            retVal.put("Flesh Moulder master", String.class);
            retVal.put("Flesh Moulder minion 1", String.class);
            retVal.put("Flesh Moulder minion 2", String.class);
            retVal.put("Flesh Moulder minion 3", String.class);
            retVal.put("Ettin master", String.class);
            retVal.put("Ettin minion 1", String.class);
            retVal.put("Elemental master", String.class);
            retVal.put("Elemental minion 1", String.class);
            retVal.put("Merriod master", String.class);
            retVal.put("Merriod minion 1", String.class);
            retVal.put("Shadow Dragon master", String.class);
            retVal.put("Shadow Dragon minion 1", String.class);
            retVal.put("Sir Alric Farrow", String.class);
            retVal.put("Belthir", String.class);
            retVal.put("Lady Eliza Farrow", String.class);
            retVal.put("Lord Merick Farrow", String.class);
            retVal.put("Splig", String.class);
            retVal.put("Baron Zachareth", String.class);
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

            List<Monster> allOriginalMonsters = dgs.getAllOriginalMonsters();
            List<Monster> allMonsters = dgs.getAllMonsters();

            for (Monster monster : allOriginalMonsters)
                records.put(monster.getName() + " HP", "Dead");

            for (Monster monster : allMonsters)
            {
                records.put(monster.getName() + " HP", Integer.toString(monster.getAttributeValue(Figure.Attribute.Health)));
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
            retVal.put("Goblin Archer master HP", String.class);
            retVal.put("Goblin Archer minion 1 HP", String.class);
            retVal.put("Goblin Archer minion 2 HP", String.class);
            retVal.put("Goblin Archer minion 3 HP", String.class);
            retVal.put("Goblin Archer minion 4 HP", String.class);
            retVal.put("Barghest master HP", String.class);
            retVal.put("Barghest minion 1 HP", String.class);
            retVal.put("Barghest minion 2 HP", String.class);
            retVal.put("Barghest minion 3 HP", String.class);
            retVal.put("Zombie master HP", String.class);
            retVal.put("Zombie minion 1 HP", String.class);
            retVal.put("Zombie minion 2 HP", String.class);
            retVal.put("Zombie minion 3 HP", String.class);
            retVal.put("Zombie minion 4 HP", String.class);
            retVal.put("Cave Spider master HP", String.class);
            retVal.put("Cave Spider minion 1 HP", String.class);
            retVal.put("Cave Spider minion 2 HP", String.class);
            retVal.put("Cave Spider minion 3 HP", String.class);
            retVal.put("Cave Spider minion 4 HP", String.class);
            retVal.put("Flesh Moulder master HP", String.class);
            retVal.put("Flesh Moulder minion 1 HP", String.class);
            retVal.put("Flesh Moulder minion 2 HP", String.class);
            retVal.put("Flesh Moulder minion 3 HP", String.class);
            retVal.put("Ettin master HP", String.class);
            retVal.put("Ettin minion 1 HP", String.class);
            retVal.put("Elemental master HP", String.class);
            retVal.put("Elemental minion 1 HP", String.class);
            retVal.put("Merriod master HP", String.class);
            retVal.put("Merriod minion 1 HP", String.class);
            retVal.put("Shadow Dragon master HP", String.class);
            retVal.put("Shadow Dragon minion 1 HP", String.class);
            retVal.put("Sir Alric Farrow HP", String.class);
            retVal.put("Belthir HP", String.class);
            retVal.put("Lady Eliza Farrow HP", String.class);
            retVal.put("Lord Merick Farrow HP", String.class);
            retVal.put("Splig HP", String.class);
            retVal.put("Baron Zachareth HP", String.class);
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
                        hero.getAttributeValue(Health) + " HP - " + hero.getHandEquipment().toString();
                if (hero.getPosition() != null)
                    h += " - " + hero.getPosition().toString();
                setup.add(h);
            }
            for (int i = 0; i < dgs.getMonsters().size(); i++)
            {
                for (Monster monster : dgs.getMonsters().get(i))
                {
                    String m = monster.getComponentName() + " - " + monster.getAttributeValue(Health) + " HP";
                    if (monster.getPosition() != null)
                        m += " - " + monster.getPosition().toString();
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

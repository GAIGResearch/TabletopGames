package games.catan.stats;

import core.actions.LogEvent;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.components.Building;
import games.catan.components.CatanTile;

import java.util.*;

public class CatanMetrics implements IMetricsCollection {
    public enum CatanEvent implements IGameEvent {
        SetupComplete,
        PortSettle,
        SevenOut,
        LongestRoadSteal,
        LargestArmySteal;

        @Override
        public Set<IGameEvent> getValues() {
            return new HashSet<>(Arrays.asList(CatanEvent.values()));
        }
    }

    // win rate per player type per starting position + overall player type
    public static class Win extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            Set<Integer> winners = e.state.getWinners();
            if (winners.size() != 1)
                return false;
            int winnerId = winners.iterator().next();
            records.put("PlayerType", listener.getGame().getPlayers().get(winnerId).toString());
            records.put("PlayerType-StartingPos", listener.getGame().getPlayers().get(winnerId).toString() + "-" + winnerId);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, List<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("PlayerType", String.class);
            columns.put("PlayerType-StartingPos", String.class);
            return columns;
        }
    }

    // On setup complete:
    // Initial sum of production values
    // Variety of starting resources (how many different types of resources)
    // Starting resources (how many of each type)
    public static class StartingResources extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            CatanGameState gs = (CatanGameState) e.state;
            CatanParameters cp = (CatanParameters) gs.getGameParameters();
            Map<Integer, Integer> nDots = nDots(cp);
            List<List<CatanParameters.Resource>> initResources = new ArrayList<>();
            int[] initProductionSum = new int[gs.getNPlayers()];
            for (int i = 0; i < gs.getNPlayers(); i++) {
                initResources.add(new ArrayList<>());
            }

            for (CatanTile[] catanTiles : gs.getBoard()) {
                for (CatanTile tile : catanTiles) {
                    for (Building settl : gs.getBuildings(tile)) {
                        int who = settl.getOwnerId();
                        if (who != -1) {
                            initResources.get(who).add(cp.productMapping.get(tile.getTileType()));
                            initProductionSum[who] += nDots.get(tile.getNumber());
                        }
                    }
                }
            }
            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                for (CatanParameters.Resource r: CatanParameters.Resource.values()) {
                    if (initResources.get(i).contains(r)) {
                        // Count how many occurrences of r are in initResources.get(i)
                        int count = Collections.frequency(initResources.get(i), r);
                        records.put(playerName + "_init_" + r.name(), count);
                    } else {
                        records.put(playerName + "_init_" + r.name(), 0);
                    }
                }
                records.put(playerName + "_sumInitProduction", initProductionSum[i]);
                // Count unique elements in initResources.get(i)
                Set<CatanParameters.Resource> uniqueResources = new HashSet<>(initResources.get(i));
                records.put(playerName + "_varietyResources", uniqueResources.size());
            }

            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(CatanEvent.SetupComplete);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, List<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String player : playerNames) {
                columns.put(player + "_sumInitProduction", Integer.class);
                columns.put(player + "_varietyResources", Integer.class);
                for (CatanParameters.Resource r : CatanParameters.Resource.values()) {
                    columns.put(player + "_init_" + r.name(), Integer.class);
                }
            }
            return columns;
        }

        private Map<Integer, Integer> nDots(CatanParameters params) {
            Map<Integer, Integer> nDotsPerRoll = new HashMap<>();
            int nDots = 0;
            for (int i = params.nDice; i <= params.nDice*params.dieType.nSides; i++) {
                if (i <= params.robber_die_roll) {
                    nDots++;
                } else if (nDots > 0) {
                    nDots--;
                }
                nDotsPerRoll.put(i, nDots);
            }
            return nDotsPerRoll;
        }
    }

    // Settlements played on ports
    public static class PortSettle extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            int playerId = Integer.parseInt(((LogEvent)e.action).text);
            String playerName = listener.getGame().getPlayers().get(playerId).toString();
            records.put(playerName + "_portSettle", 1);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(CatanEvent.PortSettle);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, List<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String player : playerNames) {
                columns.put(player + "_portSettle", Integer.class);
            }
            return columns;
        }
    }

    // On action chosen OR on game over to avoid deep duplicates?
    // # Development cards bought
    // # roads built
    // # settlements built
    // # cities built

    // On game over:
    // # knight cards played
    // longest road length

    // On 7-out event:
    // # times 7-outed

    // On LongestRoadSteal and LargestArmySteal events
    // # times longest road and largest army were taken by other players after initially claimed


    // Settlements played adjacent to existing road

    // Percentage of turns in the lead
}

package games.catan.stats;

import core.actions.AbstractAction;
import core.actions.LogEvent;
import core.components.Edge;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.catan.CatanGameState;
import games.catan.CatanParameters;
import games.catan.actions.build.*;
import games.catan.components.Building;
import games.catan.components.CatanTile;

import java.util.*;
import java.util.stream.IntStream;

import static games.catan.CatanConstants.HEX_SIDES;

@SuppressWarnings("unused")
public class CatanMetrics implements IMetricsCollection {
    public enum CatanEvent implements IGameEvent {
        SetupComplete,
        PortSettle,
        RobberRoll,
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
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
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
                    if (tile.getTileType() != CatanTile.TileType.DESERT && tile.getTileType() != CatanTile.TileType.SEA) {
                        for (Building settl : gs.getBuildings(tile)) {
                            int who = settl.getOwnerId();
                            if (who != -1) {
                                initResources.get(who).add(cp.productMapping.get(tile.getTileType()));
                                initProductionSum[who] += nDots.get(tile.getNumber());
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                for (CatanParameters.Resource r: CatanParameters.Resource.values()) {
                    if (r == CatanParameters.Resource.WILD) continue;
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
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String player : playerNames) {
                columns.put(player + "_sumInitProduction", Integer.class);
                columns.put(player + "_varietyResources", Integer.class);
                for (CatanParameters.Resource r : CatanParameters.Resource.values()) {
                    if (r == CatanParameters.Resource.WILD) continue;
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
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String player : playerNames) {
                columns.put(player + "_portSettle", Integer.class);
            }
            return columns;
        }
    }

    // On game over:
    // # knight cards played
    // longest road length
    public static class Bonuses extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            CatanGameState gs = (CatanGameState) e.state;
            for (int i = 0; i < gs.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_nKnights", gs.getKnights()[i]);
                records.put(playerName + "_longestRoadLength", gs.getRoadLengths()[i]);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p: playerNames) {
                columns.put(p + "_nKnights", Integer.class);
                columns.put(p + "_longestRoadLength", Integer.class);
            }
            return columns;
        }
    }

    // On 7-out event:
    // # times 7-outed
    public static class SevenOuts extends AbstractMetric {
        HashSet<Integer> sevenOuts = new HashSet<>();

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            CatanGameState gs = (CatanGameState) e.state;
            if (e.type == CatanEvent.SevenOut) {
                sevenOuts.add(Integer.parseInt(((LogEvent)e.action).text));
                return false;
            }
            else {
                for (int i = 0; i < gs.getNPlayers(); i++) {
                    String playerName = listener.getGame().getPlayers().get(i).toString();
                    records.put(playerName + "_nSevenOuts", sevenOuts.contains(i) ? 1 : 0);
                }
                sevenOuts.clear();
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(CatanEvent.SevenOut, CatanEvent.RobberRoll));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p: playerNames) {
                columns.put(p + "_nSevenOuts", Integer.class);
            }
            return columns;
        }
    }

    // On LongestRoadSteal and LargestArmySteal events
    // # times longest road and largest army were taken by other players after initially claimed
    public static class BonusSteal extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == CatanEvent.LongestRoadSteal) {
                String playerName = listener.getGame().getPlayers().get(Integer.parseInt(((LogEvent)e.action).text)).toString();
                records.put(playerName + "_nLongestRoadSteals", 1);
            }
            else if (e.type == CatanEvent.LargestArmySteal) {
                String playerName = listener.getGame().getPlayers().get(Integer.parseInt(((LogEvent)e.action).text)).toString();
                records.put(playerName + "_nLargestArmySteals", 1);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(CatanEvent.LongestRoadSteal, CatanEvent.LargestArmySteal));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p: playerNames) {
                columns.put(p + "_nLongestRoadSteals", Integer.class);
                columns.put(p + "_nLargestArmySteals", Integer.class);
            }
            return columns;
        }
    }

    // On action chosen:
    // # Development cards bought
    // # roads built
    // # settlements built
    // # cities built
    public static class Developments extends AbstractMetric {

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            AbstractAction a = e.action;
            String playerName = listener.getGame().getPlayers().get(e.playerID).toString();
            if (a instanceof BuildCity) {
                records.put(playerName + "_devType", BuyAction.BuyType.City.name());
            } else if (a instanceof BuildSettlement && !((BuildSettlement) a).free) {
                records.put(playerName + "_devType", BuyAction.BuyType.Settlement.name());
            } else if (a instanceof BuildRoad && !((BuildRoad) a).free) {
                records.put(playerName + "_devType", BuyAction.BuyType.Road.name());
            } else if (a instanceof BuyDevelopmentCard) {
                records.put(playerName + "_devType", BuyAction.BuyType.DevCard.name());
            } else {
                return false;
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
            for (String p: playerNames) {
                columns.put(p + "_devType", String.class);
            }
            return columns;
        }
    }

    // Percentage of turns in the lead
    public static class LeadPercentage extends AbstractMetric {
        int[] nTurnsInLead;
        int[] nTurnsPlayed;

        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            if (e.type == Event.GameEvent.GAME_OVER) {
                for (int i = 0; i < e.state.getNPlayers(); i++) {
                    String playerName = listener.getGame().getPlayers().get(i).toString();
                    records.put(playerName + "_leadPercentage", (double) nTurnsInLead[i] / nTurnsPlayed[i]);
                }
                return true;
            }
            else if (e.type == Event.GameEvent.TURN_OVER) {
                CatanGameState gs = (CatanGameState) e.state;
                int pId = Integer.parseInt(((LogEvent)e.action).text);
                int[] scores = gs.getScores();
                int[] vps = gs.getVictoryPoints();
                IntStream.range(0, e.state.getNPlayers()).forEach(i -> scores[i] += vps[i]);
                int leader = pId;
                for (int i = 0; i < e.state.getNPlayers(); i++) {
                    if (scores[i] > scores[leader]) {
                        leader = i;
                    }
                }
                if (leader == pId) {
                    nTurnsInLead[pId]++;
                }
                nTurnsPlayed[pId]++;
            }
            return false;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return new HashSet<>(Arrays.asList(Event.GameEvent.TURN_OVER, Event.GameEvent.GAME_OVER));
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            nTurnsInLead = new int[nPlayersPerGame];
            nTurnsPlayed = new int[nPlayersPerGame];

            Map<String, Class<?>> columns = new HashMap<>();
            for (String p: playerNames) {
                columns.put(p + "_leadPercentage", Double.class);
            }
            return columns;
        }
    }

    // On game over:
    // Check each vertex on the board. If there are 3 roads adjacent, check if 2 of those roads are owned by the same player.
    // If so, check if the 3rd road is owned by a different player. If so, increment the number of roads cut off for that player.
    public static class RoadsCutOff extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            CatanGameState gs = (CatanGameState) e.state;
            int[] cutOffs = new int[e.state.getNPlayers()];  // players cut off by others
            int[] cuttingOffs = new int[e.state.getNPlayers()];  // players doing the cut offs
            for (CatanTile[] row: gs.getBoard()) {
                for (CatanTile tile: row) {
                    for (int v = 0; v < HEX_SIDES; v++) {
                        Building settlement = gs.getBuilding(tile, v);
                        List<Integer> owners = new ArrayList<>();
                        for (Edge edge: settlement.getEdges()) {
                            if (edge.getOwnerId() != -1) {
                                owners.add(edge.getOwnerId());
                            }
                        }
                        if (owners.size() == 3) {
                            Set<Integer> ownerSet = new HashSet<>(owners);
                            if (ownerSet.size() == 2) {
                                // A cutting off happened!
                                // Find the one who only appears once in owners list
                                int cutOffPlayer = -1;
                                int cuttingOffPlayer = -1;
                                for (int p: ownerSet) {
                                    if (Collections.frequency(owners, p) == 1) {
                                        cutOffPlayer = p;
                                    } else {
                                        cuttingOffPlayer = p;
                                    }
                                }
                                cutOffs[cutOffPlayer]++;
                                cuttingOffs[cuttingOffPlayer]++;
                            }
                        }
                    }
                }
            }
            for (int i = 0; i < e.state.getNPlayers(); i++) {
                String playerName = listener.getGame().getPlayers().get(i).toString();
                records.put(playerName + "_nRoadsCutOffToOthers", cuttingOffs[i]);
                records.put(playerName + "_nRoadsCutOffByOthers", cutOffs[i]);
            }
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            for (String p: playerNames) {
                columns.put(p + "_nRoadsCutOffToOthers", Integer.class);
                columns.put(p + "_nRoadsCutOffByOthers", Integer.class);
            }
            return columns;
        }
    }

    public static class RandomSeeds extends AbstractMetric {
        @Override
        protected boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            CatanGameState gs = (CatanGameState) e.state;
            CatanParameters cp = (CatanParameters) gs.getGameParameters();
            records.put("HexSeed", cp.hexShuffleSeed);
            records.put("DiceSeed", cp.diceSeed);
            return true;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.GAME_OVER);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("HexSeed", Integer.class);
            columns.put("DiceSeed", Integer.class);
            return columns;
        }
    }



}

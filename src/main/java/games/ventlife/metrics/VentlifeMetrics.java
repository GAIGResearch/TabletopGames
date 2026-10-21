package games.ventlife.metrics;

import core.AbstractGameState;
import core.interfaces.IGameEvent;
import evaluation.listeners.MetricsGameListener;
import evaluation.metrics.AbstractMetric;
import evaluation.metrics.Event;
import evaluation.metrics.IMetricsCollection;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeUtils;
import games.ventlife.actions.Displace;
import games.ventlife.actions.PlaceCreature;
import games.ventlife.actions.PlaceTile;
import games.ventlife.actions.StopPlacing;
import games.ventlife.components.Creature;
import games.ventlife.components.Hex;
import games.ventlife.components.HexCell;
import games.ventlife.components.Species;

import java.util.*;

/**
 * Metrics for analysing how Ventlife agents score: the final score of each player by species, and what each action
 * did to the scores.
 */
@SuppressWarnings("unused")
public class VentlifeMetrics implements IMetricsCollection {

    /**
     * At the end of the game, for each player: name, score, ordinal position, the score from each species (-1 if not
     * in play), creatures on the field and tokens left in supply. Plus the species in play and the shape of the field.
     */
    public static class GameEnd extends AbstractMetric {

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            VentlifeGameState state = (VentlifeGameState) e.state;
            int n = state.getNPlayers();
            StringBuilder species = new StringBuilder();
            for (Species s : state.getSpeciesInPlay())
                species.append(species.isEmpty() ? "" : "|").append(s.name());
            records.put("Species", species.toString());
            int maxLevel = 0, raised = 0;
            for (HexCell c : state.getField().values()) {
                maxLevel = Math.max(maxLevel, c.level());
                if (c.level() >= 2) raised++;
            }
            records.put("FieldSize", state.getField().size());
            records.put("MaxLevel", maxLevel);
            records.put("RaisedHexes", raised);
            records.put("Plateaus", VentlifeUtils.plateaus(state).size());
            int[] onField = new int[n];
            for (Creature c : state.getCreatures().values())
                onField[c.owner()] += c.count();
            for (int p = 0; p < n; p++) {
                records.put("Name-" + p, listener.getGame().getPlayers().get(p).toString());
                records.put("Score-" + p, (int) state.getGameScore(p));
                records.put("Ordinal-" + p, state.getOrdinalPosition(p));
                records.put("OnField-" + p, onField[p]);
                int supply = 0;
                for (Species s : Species.values()) {
                    boolean inPlay = state.getSpeciesInPlay().contains(s);
                    records.put(s.name() + "-" + p, inPlay ? state.speciesScore(p, s) : -1);
                    if (inPlay) supply += state.getSupply(p, s);
                }
                records.put("Supply-" + p, supply);
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
            columns.put("Species", String.class);
            columns.put("FieldSize", Integer.class);
            columns.put("MaxLevel", Integer.class);
            columns.put("RaisedHexes", Integer.class);
            columns.put("Plateaus", Integer.class);
            for (int p = 0; p < nPlayersPerGame; p++) {
                columns.put("Name-" + p, String.class);
                columns.put("Score-" + p, Integer.class);
                columns.put("Ordinal-" + p, Integer.class);
                columns.put("OnField-" + p, Integer.class);
                columns.put("Supply-" + p, Integer.class);
                for (Species s : Species.values())
                    columns.put(s.name() + "-" + p, Integer.class);
            }
            return columns;
        }
    }

    /**
     * Each action chosen: its kind, the species (for creatures), whether a tile makes a plateau, the creatures it
     * covers, and the change it makes to the mover's score and to the best opponent score (measured by applying it to
     * a copy of the state, so before any later decisions in the same turn).
     */
    public static class Actions extends AbstractMetric {

        @Override
        public boolean _run(MetricsGameListener listener, Event e, Map<String, Object> records) {
            VentlifeGameState state = (VentlifeGameState) e.state;
            int player = e.playerID;
            records.put("Player", player);
            records.put("PlayerName", listener.getGame().getPlayers().get(player).toString());
            records.put("TilesPlaced", state.getTilesPlaced());
            records.put("Choices", e.actions == null ? 0 : e.actions.size());
            String kind;
            String species = "";
            int level = 0, ownCovered = 0, oppCovered = 0;
            switch (e.action) {
                case PlaceTile t -> {
                    kind = state.getCell(t.smoker) == null ? "TILE_SEAFLOOR" : "TILE_PLATEAU";
                    level = state.getLevel(t.smoker) + 1;
                    for (Hex h : VentlifeUtils.tileHexes(t.smoker, t.orientation)) {
                        Creature c = state.getCreature(h);
                        if (c == null || (h.equals(t.smoker) && c.species() == Species.TUBE_WORM))
                            continue;
                        if (c.owner() == player) ownCovered += c.count();
                        else oppCovered += c.count();
                    }
                }
                case PlaceCreature p -> {
                    kind = "PLACE";
                    species = p.species.name();
                    level = state.getLevel(p.hex);
                }
                case Displace d -> {
                    kind = "DISPLACE";
                    species = d.creature.species().name();
                    level = state.getLevel(d.to);
                }
                case StopPlacing s -> kind = "STOP";
                default -> kind = e.action.getClass().getSimpleName();
            }
            records.put("Kind", kind);
            records.put("Species", species);
            records.put("Level", level);
            records.put("OwnCovered", ownCovered);
            records.put("OppCovered", oppCovered);

            double ownBefore = state.getGameScore(player), oppBefore = bestOther(state, player);
            AbstractGameState next = state.copy();
            listener.getGame().getForwardModel().next(next, e.action.copy());
            records.put("OwnDelta", next.getGameScore(player) - ownBefore);
            records.put("OppDelta", bestOther(next, player) - oppBefore);
            return true;
        }

        private static double bestOther(AbstractGameState state, int player) {
            double best = 0;
            for (int p = 0; p < state.getNPlayers(); p++)
                if (p != player)
                    best = Math.max(best, state.getGameScore(p));
            return best;
        }

        @Override
        public Set<IGameEvent> getDefaultEventTypes() {
            return Collections.singleton(Event.GameEvent.ACTION_CHOSEN);
        }

        @Override
        public Map<String, Class<?>> getColumns(int nPlayersPerGame, Set<String> playerNames) {
            Map<String, Class<?>> columns = new HashMap<>();
            columns.put("Player", Integer.class);
            columns.put("PlayerName", String.class);
            columns.put("TilesPlaced", Integer.class);
            columns.put("Choices", Integer.class);
            columns.put("Kind", String.class);
            columns.put("Species", String.class);
            columns.put("Level", Integer.class);
            columns.put("OwnCovered", Integer.class);
            columns.put("OppCovered", Integer.class);
            columns.put("OwnDelta", Double.class);
            columns.put("OppDelta", Double.class);
            return columns;
        }
    }
}

package games.ventlife.ai;

import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.VentlifeUtils;
import games.ventlife.components.*;

import java.util.*;

/**
 * Values tile placements in one state without copying it: the VentlifeHeuristic value (not fishSeed), for the player
 * placing the tile, of the state that VentlifeTileSim.afterTile would produce.
 * <p>
 * A tile changes the field only under itself, and moves covered creatures one step away (an Octopus along a line),
 * so every creature whose own score, exposure or covering risk can change lies within two steps of the tile or next
 * to where a covered creature went. Their terms are recomputed
 * on a view of the field with the tile laid over it (TileOverlay); everything else carries over from the state. Shoals
 * and plateau majorities are recomputed in full, when a tile covers a Fish or forms a plateau with Crabs about.
 */
public class VentlifeTileEval {

    private final VentlifeGameState state;
    private final VentlifeParameters params;
    private final Board base;
    private final int n;
    private final double oppMaxWeight, oppMeanWeight, moveFactor, crabValue;
    private final double[] expWeight, riskWeight;
    // the tiles left once the tile is placed
    private final int tilesLeft;
    // the adjusted scores, and each creature's term in them, as they will be valued once a tile is placed
    private final double[] adjusted;
    private final Map<Hex, Double> terms = new HashMap<>();
    private final int[] fishScores, crabScores;
    private final boolean crabsOnField;

    public VentlifeTileEval(VentlifeGameState state, int perspective, VentlifeHeuristic h) {
        this.state = state;
        this.params = state.getParams();
        this.base = Board.of(state);
        n = state.getNPlayers();
        oppMaxWeight = h.oppMaxWeight;
        oppMeanWeight = h.oppMeanWeight;
        moveFactor = h.exposureMoveFactor;
        crabValue = h.exposureCrabValue;
        tilesLeft = CoverageModel.tilesLeft(state) - 1;
        double scale = h.endScaling ? Math.min(1.0, (double) tilesLeft / n) : 1.0;
        double oppExp = h.oppExposure < 0 ? h.exposure : h.oppExposure;
        double oppR = h.oppRisk < 0 ? h.risk : h.oppRisk;
        expWeight = new double[n];
        riskWeight = new double[n];
        for (int p = 0; p < n; p++) {
            expWeight[p] = scale * (p == perspective ? h.exposure : oppExp);
            riskWeight[p] = p == perspective ? h.risk : oppR;
        }
        int[] scores = VentlifeScorer.scores(state);
        adjusted = new double[n];
        boolean crabs = false;
        for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet()) {
            Creature c = e.getValue();
            double t = term(base, e.getKey(), c);
            terms.put(e.getKey(), t);
            // the adjusted score is the game score less the penalties; the term also holds the creature's own score
            adjusted[c.owner()] += t - ownScore(base, e.getKey(), c);
            crabs |= c.species() == Species.YETI_CRAB;
        }
        crabsOnField = crabs;
        for (int p = 0; p < n; p++)
            adjusted[p] += scores[p];
        fishScores = fishScores(base, state.getCreatures().keySet());
        crabScores = crabsOnField ? crabScores(base, state.getField().keySet()) : new int[n];
    }

    /**
     * The heuristic value, for the player placing it, after placer puts the tile with its Black Smoker on smoker in
     * the given orientation.
     */
    public double tileValue(int placer, VentTile tile, Hex smoker, int orientation) {
        return VentlifeHeuristic.value(placer, adjustedAfter(placer, tile, smoker, orientation), oppMaxWeight, oppMeanWeight);
    }

    /**
     * Every player's adjusted score after the tile placement.
     */
    double[] adjustedAfter(int placer, VentTile tile, Hex smoker, int orientation) {
        int level = base.level(smoker) + 1;
        int tileId = state.getTilesPlaced();
        List<Hex> hexes = VentlifeUtils.tileHexes(smoker, orientation);
        TileOverlay over = new TileOverlay(state, hexes, new HexCell[]{new HexCell(Terrain.BLACK_SMOKER, level, tileId),
                new HexCell(tile.left, level, tileId), new HexCell(tile.right, level, tileId)});
        // covered creatures: the placer's first, then clockwise; one player's in tile order
        List<Hex> coveredFrom = new ArrayList<>();
        List<Creature> covered = new ArrayList<>();
        boolean fishMoved = false;
        for (int k = 0; k < n; k++) {
            int owner = (placer + k) % n;
            for (Hex h : hexes) {
                Creature c = state.getCreature(h);
                if (c == null || c.owner() != owner) continue;
                if (h.equals(smoker) && c.species() == Species.TUBE_WORM) continue;
                coveredFrom.add(h);
                covered.add(c);
                over.remove(h);
                fishMoved |= c.species() == Species.EELPOUT_FISH;
            }
        }
        for (int i = 0; i < covered.size(); i++) {
            List<Hex> destinations = destinations(over, covered.get(i).species(), coveredFrom.get(i), level - 1, tileId);
            if (destinations.size() > 1) break;
            if (destinations.size() == 1) over.add(destinations.get(0), covered.get(i));
        }
        // the creatures within two steps of the tile, and next to where covered creatures went (an Octopus may go
        // to the far end of a line)
        Set<Hex> region = new HashSet<>(hexes);
        for (Hex h : hexes)
            for (Hex nb : h.neighbours())
                if (region.add(nb))
                    region.addAll(nb.neighbours());
        for (Hex h : over.added()) {
            region.add(h);
            region.addAll(h.neighbours());
        }
        double[] d = adjusted.clone();
        for (Hex h : region) {
            Double before = terms.get(h);
            Creature c = state.getCreature(h);
            if (before != null) d[c.owner()] -= before;
            Creature after = over.creature(h);
            if (after != null) d[after.owner()] += term(over, h, after);
        }
        if (fishMoved) {
            Set<Hex> fishHexes = new HashSet<>(state.getCreatures().keySet());
            fishHexes.addAll(over.added());
            int[] after = fishScores(over, fishHexes);
            for (int p = 0; p < n; p++) d[p] += after[p] - fishScores[p];
        }
        if (crabsOnField && level >= 2) {
            int[] after = crabScores(over, state.getField().keySet());
            for (int p = 0; p < n; p++) d[p] += after[p] - crabScores[p];
        }
        return d;
    }

    /**
     * The creature's own score if it is scored on its own (not Fish or Crabs).
     */
    private int ownScore(Board b, Hex hex, Creature c) {
        return switch (c.species()) {
            case EELPOUT_FISH, YETI_CRAB -> 0;
            default -> Board.creatureScore(b, params, hex, c);
        };
    }

    /**
     * The creature's contribution to its owner's adjusted score, apart from shoals and plateau majorities.
     */
    private double term(Board b, Hex hex, Creature c) {
        double t = ownScore(b, hex, c);
        int owner = c.owner();
        if (expWeight[owner] == 0 && riskWeight[owner] == 0) return t;
        double loss = Board.lossIfCovered(b, params, hex, c, moveFactor, crabValue);
        if (loss == 0) return t;
        int bits = Board.exposedBits(b, hex);
        if (expWeight[owner] != 0 && bits != 0 && (c.species() != Species.TUBE_WORM || (bits & 1) != 0))
            t -= expWeight[owner] * loss;
        if (riskWeight[owner] != 0)
            t -= riskWeight[owner] * loss * CoverageModel.probability(Board.features(b, hex, c.species(), bits, tilesLeft), c.species());
        return t;
    }

    /**
     * Where a covered creature may go, as Species.destinations, on the overlaid field.
     */
    private static List<Hex> destinations(Board b, Species species, Hex from, int fromLevel, int newTile) {
        List<Hex> retValue = new ArrayList<>();
        if (species == Species.OCTOPUS) {
            for (int d = 0; d < 6; d++) {
                Hex h = from.neighbour(d);
                if (b.cell(h) == null) continue;
                while (b.cell(h.neighbour(d)) != null)
                    h = h.neighbour(d);
                if (b.creature(h) == null && b.cell(h).tileId() != newTile)
                    retValue.add(h);
            }
            retValue.sort(VentlifeUtils.HEX_ORDER);
            return retValue;
        }
        for (Hex nb : from.neighbours()) {
            HexCell cell = b.cell(nb);
            if (cell == null || b.creature(nb) != null || cell.tileId() == newTile) continue;
            boolean ok = switch (species) {
                case VENT_SHRIMP -> cell.terrain() != Terrain.BASALT_RIDGE;
                case EELPOUT_FISH -> cell.terrain() != Terrain.BLACK_SMOKER && cell.level() <= fromLevel;
                case YETI_CRAB -> cell.level() == fromLevel;
                case VOLCANO_SNAIL -> cell.level() < fromLevel;
                default -> false;
            };
            if (ok) retValue.add(nb);
        }
        retValue.sort(VentlifeUtils.HEX_ORDER);
        return retValue;
    }

    private int[] fishScores(Board b, Collection<Hex> positions) {
        int[] retValue = new int[n];
        Set<Hex> seen = new HashSet<>();
        ArrayDeque<Hex> open = new ArrayDeque<>();
        for (Hex start : positions) {
            Creature c = b.creature(start);
            if (c == null || c.species() != Species.EELPOUT_FISH || !seen.add(start)) continue;
            int size = 0;
            open.push(start);
            while (!open.isEmpty()) {
                Hex h = open.pop();
                size++;
                for (Hex nb : h.neighbours()) {
                    Creature f = b.creature(nb);
                    if (f != null && f.species() == Species.EELPOUT_FISH && f.owner() == c.owner() && seen.add(nb))
                        open.push(nb);
                }
            }
            retValue[c.owner()] += params.shoalScore(size);
        }
        return retValue;
    }

    private int[] crabScores(Board b, Collection<Hex> field) {
        int[] retValue = new int[n];
        Set<Hex> seen = new HashSet<>();
        ArrayDeque<Hex> open = new ArrayDeque<>();
        for (Hex start : field) {
            int level = b.level(start);
            if (level < 2 || !seen.add(start)) continue;
            int size = 0;
            int[] crabs = new int[n];
            open.push(start);
            while (!open.isEmpty()) {
                Hex h = open.pop();
                size++;
                Creature c = b.creature(h);
                if (c != null && c.species() == Species.YETI_CRAB) crabs[c.owner()] += c.count();
                for (Hex nb : h.neighbours())
                    if (b.level(nb) == level && seen.add(nb))
                        open.push(nb);
            }
            // VentlifeUtils.plateaus, which the game scores by, includes single hexes
            int most = 0, nMost = 0, second = 0;
            for (int c : crabs) most = Math.max(most, c);
            if (most == 0) continue;
            for (int c : crabs) {
                if (c == most) nMost++;
                else second = Math.max(second, c);
            }
            for (int p = 0; p < n; p++) {
                if (crabs[p] == 0) continue;
                if (crabs[p] == most) retValue[p] += size;
                else if (nMost == 1 && crabs[p] == second) retValue[p] += size / 2;
            }
        }
        return retValue;
    }

    /**
     * The field with a tile laid over it, and covered creatures removed or moved.
     */
    private static class TileOverlay implements Board {
        private final VentlifeGameState state;
        private final List<Hex> hexes;
        private final HexCell[] cells;
        private final Set<Hex> removed = new HashSet<>(4);
        private final Map<Hex, Creature> added = new HashMap<>(4);

        TileOverlay(VentlifeGameState state, List<Hex> hexes, HexCell[] cells) {
            this.state = state;
            this.hexes = hexes;
            this.cells = cells;
        }

        void remove(Hex h) {
            removed.add(h);
        }

        void add(Hex h, Creature c) {
            added.put(h, c);
        }

        Set<Hex> added() {
            return added.keySet();
        }

        @Override
        public HexCell cell(Hex hex) {
            for (int i = 0; i < 3; i++)
                if (hexes.get(i).equals(hex)) return cells[i];
            return state.getCell(hex);
        }

        @Override
        public Creature creature(Hex hex) {
            Creature c = added.get(hex);
            if (c != null) return c;
            if (removed.contains(hex)) return null;
            return state.getCreature(hex);
        }
    }
}

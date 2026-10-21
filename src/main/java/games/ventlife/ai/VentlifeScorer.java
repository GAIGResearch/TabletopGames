package games.ventlife.ai;

import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.VentlifeUtils;
import games.ventlife.components.*;

import java.util.*;

/**
 * Every player's game score in one pass over the field: the same totals as VentlifeGameState.getGameScore, but several
 * times faster when all players' scores are needed (the plateaus, for example, are found once rather than once per
 * player).
 */
public class VentlifeScorer {

    private VentlifeScorer() {
    }

    /**
     * The positions a plateau tile could now cover: bit 1 if covered by one of the tile's terrains, bit 2 if by its
     * Black Smoker (which Tube Worms climb onto rather than being displaced).
     */
    public static Map<Hex, Integer> exposedPositions(VentlifeGameState state) {
        Map<Hex, Integer> retValue = new HashMap<>();
        for (Map.Entry<Hex, HexCell> e : state.getField().entrySet()) {
            HexCell under = e.getValue();
            if (under.terrain() != Terrain.BLACK_SMOKER)
                continue;
            // as VentlifeUtils.isPlateauPlacement, for the six orientations at once
            Hex smoker = e.getKey();
            HexCell[] around = new HexCell[6];
            boolean[] ok = new boolean[6];
            for (int d = 0; d < 6; d++) {
                HexCell c = state.getCell(smoker.neighbour(d));
                ok[d] = c != null && c.level() == under.level();
                around[d] = c;
            }
            boolean any = false;
            for (int o = 0; o < 6; o++) {
                int l = (o + 1) % 6;
                if (!ok[o] || !ok[l]) continue;
                if (around[o].tileId() == under.tileId() && around[l].tileId() == under.tileId()) continue;
                any = true;
                retValue.merge(smoker.neighbour(o), 1, (a, b) -> a | b);
                retValue.merge(smoker.neighbour(l), 1, (a, b) -> a | b);
            }
            if (any)
                retValue.merge(smoker, 2, (a, b) -> a | b);
        }
        return retValue;
    }

    /**
     * The points each player's creatures on exposed positions (see exposedPositions) stand to lose if covered:
     * the whole score of creatures that return to supply when covered (Tube Worms, Vent Sponges), and moveFactor
     * times the score of the others (a Fish counts its share of its shoal, a Crab crabValue).
     */
    public static double[] exposedValue(VentlifeGameState state, double moveFactor, double crabValue) {
        return exposedValue(state, exposedPositions(state), moveFactor, crabValue);
    }

    public static double[] exposedValue(VentlifeGameState state, Map<Hex, Integer> exposed, double moveFactor,
                                        double crabValue) {
        double[] retValue = new double[state.getNPlayers()];
        if (exposed.isEmpty()) return retValue;
        VentlifeParameters params = state.getParams();
        Map<Hex, Creature> creatures = state.getCreatures();
        for (Map.Entry<Hex, Integer> e : exposed.entrySet()) {
            Creature c = creatures.get(e.getKey());
            if (c == null) continue;
            if (c.species() == Species.TUBE_WORM && e.getValue() == 2) continue;
            Hex hex = e.getKey();
            double loss = switch (c.species()) {
                case TUBE_WORM -> c.count() * state.getLevel(hex);
                case VENT_SPONGE -> params.spongePointsPerTerrain * adjacentTerrainCount(state, hex);
                case YETI_CRAB -> moveFactor * crabValue;
                case EELPOUT_FISH -> {
                    int nb = 0;
                    for (Hex h : hex.neighbours()) {
                        Creature f = creatures.get(h);
                        if (f != null && f.species() == Species.EELPOUT_FISH && f.owner() == c.owner()) nb++;
                    }
                    yield moveFactor * params.shoalPoints * Math.min(nb, 1);
                }
                default -> moveFactor * creatureScore(state, hex, c);
            };
            retValue[c.owner()] += loss;
        }
        return retValue;
    }

    /**
     * The points each player's creatures can be expected to lose to being covered: for each creature, the
     * CoverageModel probability that it is covered before the end of the game times what it would lose (as in
     * exposedValue).
     */
    public static double[] riskValue(VentlifeGameState state, Map<Hex, Integer> exposed, double moveFactor,
                                     double crabValue, int model) {
        double[] retValue = new double[state.getNPlayers()];
        int tilesLeft = CoverageModel.tilesLeft(state);
        for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet()) {
            Creature c = e.getValue();
            double loss = lossIfCovered(state, e.getKey(), c, moveFactor, crabValue);
            if (loss != 0)
                retValue[c.owner()] += loss * CoverageModel.probability(state, e.getKey(), c.species(), exposed, tilesLeft, model);
        }
        return retValue;
    }

    /**
     * What a creature would lose if covered: the whole score of Tube Worms and Vent Sponges (which return to supply),
     * moveFactor times the score of the others (a Fish counts its share of its shoal, a Crab crabValue).
     */
    static double lossIfCovered(VentlifeGameState state, Hex hex, Creature c, double moveFactor, double crabValue) {
        return switch (c.species()) {
            case TUBE_WORM, VENT_SPONGE -> creatureScore(state, hex, c);
            case YETI_CRAB -> moveFactor * crabValue;
            case EELPOUT_FISH -> {
                for (Hex h : hex.neighbours()) {
                    Creature f = state.getCreature(h);
                    if (f != null && f.species() == Species.EELPOUT_FISH && f.owner() == c.owner())
                        yield moveFactor * state.getParams().shoalPoints;
                }
                yield 0;
            }
            default -> moveFactor * creatureScore(state, hex, c);
        };
    }

    /**
     * The score of one creature of a species scored on its own (not Fish or Crabs).
     */
    static int creatureScore(VentlifeGameState state, Hex hex, Creature c) {
        VentlifeParameters params = state.getParams();
        switch (c.species()) {
            case TUBE_WORM:
                return c.count() * state.getLevel(hex);
            case VENT_SPONGE:
                return params.spongePointsPerTerrain * adjacentTerrainCount(state, hex);
            case VENT_SHRIMP: {
                boolean vents = false, mat = false;
                for (Hex nb : hex.neighbours()) {
                    HexCell cell = state.getCell(nb);
                    if (cell == null) continue;
                    if (cell.terrain() == Terrain.DIFFUSE_VENTS) vents = true;
                    else if (cell.terrain() == Terrain.MICROBIAL_MAT) mat = true;
                }
                return vents && mat ? params.shrimpPointsBothTerrains : vents || mat ? params.shrimpPointsOneTerrain : 0;
            }
            case VOLCANO_SNAIL: {
                int level = state.getLevel(hex), edges = 0;
                for (Hex nb : hex.neighbours()) {
                    HexCell cell = state.getCell(nb);
                    if (cell != null && cell.level() != level)
                        edges++;
                }
                return params.snailPointsPerHeightEdge * edges;
            }
            case OCTOPUS: {
                int mask = 0;
                for (Hex nb : hex.neighbours()) {
                    Creature other = state.getCreature(nb);
                    if (other != null && other.species() != Species.OCTOPUS)
                        mask |= 1 << other.species().ordinal();
                }
                return params.octopusPointsPerSpecies * Integer.bitCount(mask);
            }
            default:
                return 0;
        }
    }

    public static int[] scores(VentlifeGameState state) {
        int n = state.getNPlayers();
        int[] retValue = new int[n];
        VentlifeParameters params = state.getParams();
        List<Species> inPlay = state.getSpeciesInPlay();
        Map<Hex, Creature> creatures = state.getCreatures();
        boolean fishInPlay = inPlay.contains(Species.EELPOUT_FISH);
        Map<Hex, Creature> fish = fishInPlay ? new HashMap<>() : null;
        for (Map.Entry<Hex, Creature> e : creatures.entrySet()) {
            Creature c = e.getValue();
            Hex hex = e.getKey();
            switch (c.species()) {
                case TUBE_WORM -> retValue[c.owner()] += c.count() * state.getLevel(hex);
                case VENT_SPONGE -> retValue[c.owner()] += params.spongePointsPerTerrain * adjacentTerrainCount(state, hex);
                case VENT_SHRIMP -> {
                    boolean vents = false, mat = false;
                    for (Hex nb : hex.neighbours()) {
                        HexCell cell = state.getCell(nb);
                        if (cell == null) continue;
                        if (cell.terrain() == Terrain.DIFFUSE_VENTS) vents = true;
                        else if (cell.terrain() == Terrain.MICROBIAL_MAT) mat = true;
                    }
                    retValue[c.owner()] += vents && mat ? params.shrimpPointsBothTerrains
                            : vents || mat ? params.shrimpPointsOneTerrain : 0;
                }
                case VOLCANO_SNAIL -> {
                    int level = state.getLevel(hex), edges = 0;
                    for (Hex nb : hex.neighbours()) {
                        HexCell cell = state.getCell(nb);
                        if (cell != null && cell.level() != level)
                            edges++;
                    }
                    retValue[c.owner()] += params.snailPointsPerHeightEdge * edges;
                }
                case OCTOPUS -> {
                    int mask = 0;
                    for (Hex nb : hex.neighbours()) {
                        Creature other = creatures.get(nb);
                        if (other != null && other.species() != Species.OCTOPUS)
                            mask |= 1 << other.species().ordinal();
                    }
                    retValue[c.owner()] += params.octopusPointsPerSpecies * Integer.bitCount(mask);
                }
                case EELPOUT_FISH -> fish.put(hex, c);
                case YETI_CRAB -> {
                    // scored by plateau, below
                }
            }
        }
        if (fishInPlay && !fish.isEmpty()) {
            // shoals: connected Fish of one owner
            Set<Hex> seen = new HashSet<>();
            ArrayDeque<Hex> open = new ArrayDeque<>();
            for (Map.Entry<Hex, Creature> e : fish.entrySet()) {
                if (!seen.add(e.getKey())) continue;
                int owner = e.getValue().owner(), size = 0;
                open.push(e.getKey());
                while (!open.isEmpty()) {
                    Hex h = open.pop();
                    size++;
                    for (Hex nb : h.neighbours()) {
                        Creature f = fish.get(nb);
                        if (f != null && f.owner() == owner && seen.add(nb))
                            open.push(nb);
                    }
                }
                retValue[owner] += params.shoalScore(size);
            }
        }
        if (inPlay.contains(Species.YETI_CRAB))
            addCrabScores(state, retValue);
        return retValue;
    }

    private static void addCrabScores(VentlifeGameState state, int[] retValue) {
        boolean anyCrab = false;
        for (Creature c : state.getCreatures().values())
            if (c.species() == Species.YETI_CRAB) {
                anyCrab = true;
                break;
            }
        if (!anyCrab) return;
        for (Set<Hex> plateau : VentlifeUtils.plateaus(state)) {
            int[] crabs = VentlifeUtils.crabsOn(state, plateau);
            int most = 0;
            for (int c : crabs) most = Math.max(most, c);
            if (most == 0) continue;
            int nMost = 0, second = 0;
            for (int c : crabs) {
                if (c == most) nMost++;
                else second = Math.max(second, c);
            }
            for (int p = 0; p < crabs.length; p++) {
                if (crabs[p] == 0) continue;
                if (crabs[p] == most)
                    retValue[p] += plateau.size();
                else if (nMost == 1 && crabs[p] == second)
                    retValue[p] += plateau.size() / 2;
            }
        }
    }

    static int adjacentTerrainCount(VentlifeGameState state, Hex hex) {
        int mask = 0;
        for (Hex nb : hex.neighbours()) {
            HexCell cell = state.getCell(nb);
            if (cell != null)
                mask |= 1 << cell.terrain().ordinal();
        }
        return Integer.bitCount(mask);
    }
}

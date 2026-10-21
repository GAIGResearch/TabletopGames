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

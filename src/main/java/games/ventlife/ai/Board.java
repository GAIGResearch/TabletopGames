package games.ventlife.ai;

import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.components.*;

/**
 * A read-only view of the field and the creatures on it: a state as it is, or a state with a tile laid over it
 * (TileOverlay). The static methods value single creatures on any view, as VentlifeScorer and CoverageModel do on a
 * state.
 */
interface Board {

    HexCell cell(Hex hex);

    Creature creature(Hex hex);

    static Board of(VentlifeGameState state) {
        return new Board() {
            @Override
            public HexCell cell(Hex hex) {
                return state.getCell(hex);
            }

            @Override
            public Creature creature(Hex hex) {
                return state.getCreature(hex);
            }
        };
    }

    default int level(Hex hex) {
        HexCell c = cell(hex);
        return c == null ? 0 : c.level();
    }

    /**
     * The score of one creature of a species scored on its own (not Fish or Crabs), as VentlifeScorer.creatureScore.
     */
    static int creatureScore(Board b, VentlifeParameters params, Hex hex, Creature c) {
        switch (c.species()) {
            case TUBE_WORM:
                return c.count() * b.level(hex);
            case VENT_SPONGE: {
                int mask = 0;
                for (Hex nb : hex.neighbours()) {
                    HexCell cell = b.cell(nb);
                    if (cell != null) mask |= 1 << cell.terrain().ordinal();
                }
                return params.spongePointsPerTerrain * Integer.bitCount(mask);
            }
            case VENT_SHRIMP: {
                boolean vents = false, mat = false;
                for (Hex nb : hex.neighbours()) {
                    HexCell cell = b.cell(nb);
                    if (cell == null) continue;
                    if (cell.terrain() == Terrain.DIFFUSE_VENTS) vents = true;
                    else if (cell.terrain() == Terrain.MICROBIAL_MAT) mat = true;
                }
                return vents && mat ? params.shrimpPointsBothTerrains : vents || mat ? params.shrimpPointsOneTerrain : 0;
            }
            case VOLCANO_SNAIL: {
                int level = b.level(hex), edges = 0;
                for (Hex nb : hex.neighbours()) {
                    HexCell cell = b.cell(nb);
                    if (cell != null && cell.level() != level) edges++;
                }
                return params.snailPointsPerHeightEdge * edges;
            }
            case OCTOPUS: {
                int mask = 0;
                for (Hex nb : hex.neighbours()) {
                    Creature other = b.creature(nb);
                    if (other != null && other.species() != Species.OCTOPUS)
                        mask |= 1 << other.species().ordinal();
                }
                return params.octopusPointsPerSpecies * Integer.bitCount(mask);
            }
            default:
                return 0;
        }
    }

    /**
     * What a creature would lose if covered, as VentlifeScorer.lossIfCovered.
     */
    static double lossIfCovered(Board b, VentlifeParameters params, Hex hex, Creature c, double moveFactor,
                                double crabValue) {
        return switch (c.species()) {
            case TUBE_WORM, VENT_SPONGE -> creatureScore(b, params, hex, c);
            case YETI_CRAB -> moveFactor * crabValue;
            case EELPOUT_FISH -> {
                for (Hex h : hex.neighbours()) {
                    Creature f = b.creature(h);
                    if (f != null && f.species() == Species.EELPOUT_FISH && f.owner() == c.owner())
                        yield moveFactor * params.shoalPoints;
                }
                yield 0;
            }
            default -> moveFactor * creatureScore(b, params, hex, c);
        };
    }

    /**
     * How a plateau tile could now cover the position, as in VentlifeScorer.exposedPositions: bit 1 by one of its
     * terrains, bit 2 by its Black Smoker; 0 if not at all.
     */
    static int exposedBits(Board b, Hex hex) {
        HexCell own = b.cell(hex);
        if (own == null) return 0;
        int level = own.level(), bits = 0;
        // the tile's Smoker is on the position or next to it, on a Black Smoker of the same level
        if (own.terrain() == Terrain.BLACK_SMOKER)
            for (int o = 0; o < 6 && (bits & 2) == 0; o++)
                if (plateau(b, hex, own, o)) bits |= 2;
        for (int d = 0; d < 6; d++) {
            Hex smoker = hex.neighbour(d);
            HexCell s = b.cell(smoker);
            if (s == null || s.terrain() != Terrain.BLACK_SMOKER || s.level() != level) continue;
            // the position is in direction (d + 3) from the Smoker: the tile's right terrain for orientation d + 3,
            // its left for orientation d + 2
            int back = (d + 3) % 6;
            if (plateau(b, smoker, s, back) || plateau(b, smoker, s, (back + 5) % 6)) {
                bits |= 1;
                break;
            }
        }
        return bits;
    }

    private static boolean plateau(Board b, Hex smoker, HexCell under, int o) {
        HexCell right = b.cell(smoker.neighbour(o)), left = b.cell(smoker.neighbour((o + 1) % 6));
        if (right == null || left == null || right.level() != under.level() || left.level() != under.level())
            return false;
        return right.tileId() != under.tileId() || left.tileId() != under.tileId();
    }

    /**
     * The CoverageData features of a creature of the species on the position (ownerMovesNext left 0).
     */
    static double[] features(Board b, Hex hex, Species species, int exposedBits, int tilesLeft) {
        double[] f = new double[CoverageData.FEATURES.length];
        f[0] = exposedBits != 0 && (species != Species.TUBE_WORM || (exposedBits & 1) != 0) ? 1 : 0;
        HexCell own = b.cell(hex);
        int level = own.level();
        f[1] = level;
        int same = 0, empty = 0, higher = 0, lower = 0, smokers = 0;
        if (own.terrain() == Terrain.BLACK_SMOKER) smokers++;
        for (Hex nb : hex.neighbours()) {
            HexCell c = b.cell(nb);
            if (c == null) {
                empty++;
                continue;
            }
            if (c.level() == level) {
                same++;
                if (c.terrain() == Terrain.BLACK_SMOKER) smokers++;
            } else if (c.level() > level) higher++;
            else lower++;
        }
        f[2] = empty > 0 ? 1 : 0;
        f[3] = same;
        f[4] = empty;
        f[5] = higher;
        f[6] = lower;
        f[7] = smokers;
        f[8] = tilesLeft;
        return f;
    }
}

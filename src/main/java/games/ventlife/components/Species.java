package games.ventlife.components;

import core.components.grid.GridCoord;
import core.components.grid.HexCoord;
import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;

import java.util.*;

public enum Species {
    TUBE_WORM("Giant Tube Worms"), VOLCANO_SNAIL("Volcano Snails"), VENT_SHRIMP("Blind Vent Shrimp"),
    EELPOUT_FISH("Eelpout Fish"), YETI_CRAB("Yeti Crabs"), OCTOPUS("Deep-Sea Octopus"), VENT_SPONGE("Vent Sponges");

    public final String label;

    Species(String label) {
        this.label = label;
    }

    /**
     * Whether the species' placement rule lets the player place one on the hex. The supply is not checked.
     */
    public boolean canPlace(VentlifeGameState state, int player, HexCoord hex) {
        if (!state.isEmptyHex(hex))
            return false;
        // Black Smokers are open to every species but Fish (designer, 2026-10-11)
        Terrain terrain = state.getCell(hex).terrain();
        return switch (this) {
            case TUBE_WORM -> terrain == Terrain.BLACK_SMOKER;
            case VENT_SPONGE -> true;
            case VENT_SHRIMP -> terrain != Terrain.BASALT_RIDGE;
            case EELPOUT_FISH -> terrain != Terrain.BLACK_SMOKER
                    && (state.getLevel(hex) == 1 || nextToOwnFish(state, player, hex));
            case YETI_CRAB -> state.getLevel(hex) >= 2;
            case VOLCANO_SNAIL -> terrain == Terrain.BASALT_RIDGE;
            case OCTOPUS -> state.isEdge(hex);
        };
    }

    private static boolean nextToOwnFish(VentlifeGameState state, int player, HexCoord hex) {
        for (HexCoord n : hex.neighbours()) {
            Creature c = state.getCreature(n);
            if (c != null && c.species() == EELPOUT_FISH && c.owner() == player)
                return true;
        }
        return false;
    }

    /**
     * Where a creature of this species covered at from (whose level was fromLevel before it was covered) by the tile
     * newTile may move, sorted by position; empty if it returns to its owner's supply. No creature moves onto the
     * tile that covered it.
     */
    public List<HexCoord> destinations(VentlifeGameState state, HexCoord from, int fromLevel, int newTile) {
        List<HexCoord> retValue = new ArrayList<>();
        if (this == OCTOPUS) {
            for (int d = 0; d < 6; d++) {
                HexCoord stop = retreat(state, from, d, newTile);
                if (stop != null)
                    retValue.add(stop);
            }
            retValue.sort(null);
            return retValue;
        }
        for (HexCoord n : from.neighbours()) {
            if (!state.isEmptyHex(n) || state.getCell(n).tileId() == newTile)
                continue;
            Terrain terrain = state.getCell(n).terrain();
            int level = state.getLevel(n);
            boolean ok = switch (this) {
                case VENT_SHRIMP -> terrain != Terrain.BASALT_RIDGE;
                case EELPOUT_FISH -> terrain != Terrain.BLACK_SMOKER && level <= fromLevel;
                case YETI_CRAB -> level == fromLevel;
                // placed only on Basalt Ridge, but may move onto any terrain
                case VOLCANO_SNAIL -> level < fromLevel;
                // Tube Worms not on the extended Smoker, and Vent Sponges, always return
                default -> false;
            };
            if (ok)
                retValue.add(n);
        }
        retValue.sort(null);
        return retValue;
    }

    /**
     * Where an Octopus retreating from from in the direction stops, or null if it cannot go that way.
     */
    private HexCoord retreat(VentlifeGameState state, HexCoord from, int direction, int newTile) {
        // along the straight line, passing over everything, to its end: the last hex before one with no tile in the
        // direction of travel (designer, 2026-10-11). Edge hexes along the way, with empty seafloor only to the side,
        // do not count. The direction fails if that end hex is taken or on the new tile, or if there is no tile at
        // all next to the covered position in that direction
        HexCoord h = from.neighbour(direction);
        if (state.getCell(h) == null)
            return null;
        while (state.getCell(h.neighbour(direction)) != null)
            h = h.neighbour(direction);
        return state.isEmptyHex(h) && state.getCell(h).tileId() != newTile ? h : null;
    }

    /**
     * The most creatures of this species one placement may put on the hex.
     */
    public int maxPlaced(VentlifeGameState state, HexCoord hex) {
        // the Tube Worms' Low-Vent Bonus, on a seafloor Black Smoker
        if (this == TUBE_WORM && state.getLevel(hex) == 1)
            return 1 + state.getParams().lowVentBonus;
        return 1;
    }

    /**
     * The player's end-of-game score for this species.
     */
    public int score(VentlifeGameState state, int player) {
        VentlifeParameters params = state.getParams();
        if (this == EELPOUT_FISH)
            return shoalScore(state, player);
        if (this == YETI_CRAB)
            return plateauScore(state, player);
        int score = 0;
        for (Map.Entry<HexCoord, Creature> e : state.getCreatures().entrySet()) {
            Creature c = e.getValue();
            if (c.species() != this || c.owner() != player)
                continue;
            HexCoord hex = e.getKey();
            score += switch (this) {
                case TUBE_WORM -> c.count() * state.getLevel(hex);
                case VENT_SPONGE -> params.spongePointsPerTerrain * adjacentTerrains(state, hex).size();
                case VENT_SHRIMP -> {
                    Set<Terrain> around = adjacentTerrains(state, hex);
                    boolean vents = around.contains(Terrain.DIFFUSE_VENTS), mat = around.contains(Terrain.MICROBIAL_MAT);
                    yield vents && mat ? params.shrimpPointsBothTerrains
                            : vents || mat ? params.shrimpPointsOneTerrain : 0;
                }
                case VOLCANO_SNAIL -> {
                    int edges = 0;
                    for (HexCoord n : hex.neighbours())
                        if (state.getCell(n) != null && state.getLevel(n) != state.getLevel(hex))
                            edges++;
                    yield params.snailPointsPerHeightEdge * edges;
                }
                case OCTOPUS -> {
                    Set<Species> around = EnumSet.noneOf(Species.class);
                    for (HexCoord n : hex.neighbours()) {
                        Creature other = state.getCreature(n);
                        if (other != null && other.species() != OCTOPUS)
                            around.add(other.species());
                    }
                    yield params.octopusPointsPerSpecies * around.size();
                }
                // Fish and Crabs are scored by group, above
                default -> 0;
            };
        }
        return score;
    }

    /**
     * The player's score for their shoals (connected groups of their Fish).
     */
    private static int shoalScore(VentlifeGameState state, int player) {
        Set<HexCoord> fish = new HashSet<>();
        for (Map.Entry<HexCoord, Creature> e : state.getCreatures().entrySet())
            if (e.getValue().species() == EELPOUT_FISH && e.getValue().owner() == player)
                fish.add(e.getKey());
        int score = 0;
        for (Set<HexCoord> shoal : GridCoord.connectedGroups(fish))
            score += state.getParams().shoalScore(shoal.size());
        return score;
    }

    /**
     * The player's score for their Yeti Crabs on the Elevated Plateaus.
     */
    private static int plateauScore(VentlifeGameState state, int player) {
        // an Elevated Plateau is a connected group of covered positions of one level, 2 or more
        Map<Integer, Set<HexCoord>> byLevel = new HashMap<>();
        for (Map.Entry<HexCoord, HexCell> e : state.getField().entrySet())
            if (e.getValue().level() >= 2)
                byLevel.computeIfAbsent(e.getValue().level(), k -> new HashSet<>()).add(e.getKey());
        int score = 0;
        for (Set<HexCoord> sameLevel : byLevel.values())
            for (Set<HexCoord> plateau : GridCoord.connectedGroups(sameLevel)) {
                int[] crabs = new int[state.getNPlayers()];
                for (HexCoord h : plateau) {
                    Creature c = state.getCreature(h);
                    if (c != null && c.species() == YETI_CRAB)
                        crabs[c.owner()] += c.count();
                }
                if (crabs[player] == 0)
                    continue;
                int most = Arrays.stream(crabs).max().orElse(0);
                long nMost = Arrays.stream(crabs).filter(n -> n == most).count();
                // the most Crabs score the size; the second most half of it, rounded down, unless the most is tied
                if (crabs[player] == most)
                    score += plateau.size();
                else if (nMost == 1) {
                    int second = Arrays.stream(crabs).filter(n -> n < most).max().orElse(0);
                    if (crabs[player] == second)
                        score += plateau.size() / 2;
                }
            }
        return score;
    }

    /**
     * The distinct terrains of the top hexes next to the hex.
     */
    private static Set<Terrain> adjacentTerrains(VentlifeGameState state, HexCoord hex) {
        Set<Terrain> retValue = EnumSet.noneOf(Terrain.class);
        for (HexCoord n : hex.neighbours()) {
            HexCell cell = state.getCell(n);
            if (cell != null)
                retValue.add(cell.terrain());
        }
        return retValue;
    }

    @Override
    public String toString() {
        return label;
    }
}

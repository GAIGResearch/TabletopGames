package games.ventlife.components;

import games.ventlife.VentlifeGameState;
import games.ventlife.VentlifeParameters;
import games.ventlife.VentlifeUtils;

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
    public boolean canPlace(VentlifeGameState state, int player, Hex hex) {
        if (!state.isEmptyHex(hex))
            return false;
        Terrain terrain = state.getCell(hex).terrain();
        boolean smokerOk = terrain != Terrain.BLACK_SMOKER || mayUseSmokers(state);
        return switch (this) {
            case TUBE_WORM -> terrain == Terrain.BLACK_SMOKER;
            case VENT_SPONGE -> smokerOk;
            case VENT_SHRIMP -> terrain != Terrain.BASALT_RIDGE;
            case EELPOUT_FISH -> terrain != Terrain.BLACK_SMOKER
                    && (state.getLevel(hex) == 1 || nextToOwnFish(state, player, hex));
            case YETI_CRAB -> smokerOk && state.getLevel(hex) >= 2;
            case VOLCANO_SNAIL -> terrain == Terrain.BASALT_RIDGE;
            case OCTOPUS -> smokerOk && state.isEdge(hex);
        };
    }

    private static boolean nextToOwnFish(VentlifeGameState state, int player, Hex hex) {
        for (Hex n : hex.neighbours()) {
            Creature c = state.getCreature(n);
            if (c != null && c.species() == EELPOUT_FISH && c.owner() == player)
                return true;
        }
        return false;
    }

    /**
     * Where a creature of this species covered at from (whose level was fromLevel before it was covered) may move,
     * sorted by position; empty if it returns to its owner's supply.
     */
    public List<Hex> destinations(VentlifeGameState state, Hex from, int fromLevel) {
        List<Hex> retValue = new ArrayList<>();
        if (this == OCTOPUS) {
            for (int d = 0; d < 6; d++) {
                Hex stop = retreat(state, from, d);
                if (stop != null)
                    retValue.add(stop);
            }
            retValue.sort(VentlifeUtils.HEX_ORDER);
            return retValue;
        }
        for (Hex n : from.neighbours()) {
            if (!state.isEmptyHex(n))
                continue;
            Terrain terrain = state.getCell(n).terrain();
            int level = state.getLevel(n);
            boolean ok = switch (this) {
                case VENT_SHRIMP -> terrain != Terrain.BASALT_RIDGE;
                case EELPOUT_FISH -> terrain != Terrain.BLACK_SMOKER && level <= fromLevel;
                case YETI_CRAB -> (terrain != Terrain.BLACK_SMOKER || mayUseSmokers(state)) && level == fromLevel;
                case VOLCANO_SNAIL -> (terrain != Terrain.BLACK_SMOKER || mayUseSmokers(state)) && level < fromLevel;
                // Tube Worms not on the extended Smoker, and Vent Sponges, always return
                default -> false;
            };
            if (ok)
                retValue.add(n);
        }
        retValue.sort(VentlifeUtils.HEX_ORDER);
        return retValue;
    }

    /**
     * Where an Octopus retreating from from in the direction stops, or null if it cannot go that way.
     */
    private Hex retreat(VentlifeGameState state, Hex from, int direction) {
        // along the straight line, passing over everything but an empty Edge hex it may use; the direction fails if
        // the line leaves the field first
        Hex h = from.neighbour(direction);
        while (state.getCell(h) != null) {
            if (state.isEmptyHex(h) && state.isEdge(h)
                    && (state.getCell(h).terrain() != Terrain.BLACK_SMOKER || mayUseSmokers(state)))
                return h;
            h = h.neighbour(direction);
        }
        return null;
    }

    /**
     * Whether this species may be on a Black Smoker, where its own rule does not say.
     */
    private boolean mayUseSmokers(VentlifeGameState state) {
        return this == TUBE_WORM || this == VENT_SHRIMP || !state.getParams().smokersOnlyForWormsAndShrimp;
    }

    /**
     * The most creatures of this species one placement may put on the hex.
     */
    public int maxPlaced(VentlifeGameState state, Hex hex) {
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
        for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet()) {
            Creature c = e.getValue();
            if (c.species() != this || c.owner() != player)
                continue;
            Hex hex = e.getKey();
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
                    for (Hex n : hex.neighbours())
                        if (state.getCell(n) != null && state.getLevel(n) != state.getLevel(hex))
                            edges++;
                    yield params.snailPointsPerHeightEdge * edges;
                }
                case OCTOPUS -> {
                    Set<Species> around = EnumSet.noneOf(Species.class);
                    for (Hex n : hex.neighbours()) {
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
        Set<Hex> fish = new HashSet<>();
        for (Map.Entry<Hex, Creature> e : state.getCreatures().entrySet())
            if (e.getValue().species() == EELPOUT_FISH && e.getValue().owner() == player)
                fish.add(e.getKey());
        int score = 0;
        for (Set<Hex> shoal : connectedGroups(fish))
            score += state.getParams().shoalScore(shoal.size());
        return score;
    }

    /**
     * The player's score for their Yeti Crabs on the Elevated Plateaus.
     */
    private static int plateauScore(VentlifeGameState state, int player) {
        // an Elevated Plateau is a connected group of covered positions of one level, 2 or more
        Map<Integer, Set<Hex>> byLevel = new HashMap<>();
        for (Map.Entry<Hex, HexCell> e : state.getField().entrySet())
            if (e.getValue().level() >= 2)
                byLevel.computeIfAbsent(e.getValue().level(), k -> new HashSet<>()).add(e.getKey());
        int score = 0;
        for (Set<Hex> sameLevel : byLevel.values())
            for (Set<Hex> plateau : connectedGroups(sameLevel)) {
                int[] crabs = new int[state.getNPlayers()];
                for (Hex h : plateau) {
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
     * The positions split into groups connected by adjacency.
     */
    private static List<Set<Hex>> connectedGroups(Set<Hex> positions) {
        List<Set<Hex>> retValue = new ArrayList<>();
        Set<Hex> seen = new HashSet<>();
        for (Hex start : positions) {
            if (!seen.add(start))
                continue;
            Set<Hex> group = new HashSet<>();
            Deque<Hex> open = new ArrayDeque<>(List.of(start));
            while (!open.isEmpty()) {
                Hex h = open.pop();
                group.add(h);
                for (Hex n : h.neighbours())
                    if (positions.contains(n) && seen.add(n))
                        open.push(n);
            }
            retValue.add(group);
        }
        return retValue;
    }

    /**
     * The distinct terrains of the top hexes next to the hex.
     */
    private static Set<Terrain> adjacentTerrains(VentlifeGameState state, Hex hex) {
        Set<Terrain> retValue = EnumSet.noneOf(Terrain.class);
        for (Hex n : hex.neighbours()) {
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

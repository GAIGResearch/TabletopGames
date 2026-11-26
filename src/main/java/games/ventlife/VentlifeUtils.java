package games.ventlife;

import core.components.grid.GridCoord;
import core.components.grid.HexCoord;
import games.ventlife.components.Creature;
import games.ventlife.components.HexCell;
import games.ventlife.components.Species;
import games.ventlife.components.Terrain;

import java.util.*;

public class VentlifeUtils {

    /** The species of the First Game. */
    public static final List<Species> FIRST_GAME_SPECIES = List.of(Species.TUBE_WORM, Species.VENT_SHRIMP,
            Species.VOLCANO_SNAIL, Species.YETI_CRAB);

    private VentlifeUtils() {
    }

    /**
     * The three positions covered by a tile: its Black Smoker, then its left and right terrains. The right terrain is
     * in direction orientation from the Black Smoker and the left in the next direction clockwise, so orientation 1
     * puts the Black Smoker at the top with the left terrain below-left and the right below-right, as printed.
     */
    public static List<HexCoord> tileHexes(HexCoord smoker, int orientation) {
        return List.of(smoker, smoker.neighbour((orientation + 1) % 6), smoker.neighbour(orientation));
    }

    /**
     * The number of species drafted in the Advanced Variant. They are the species to be used at 2 and 4 players, and
     * those not used at 3.
     */
    public static int draftPicks(int nSpecies, int nPlayers) {
        return nPlayers == 3 ? Species.values().length - nSpecies : nSpecies;
    }

    /**
     * Whether a tile may form a plateau there: its Black Smoker on a Black Smoker, all three hexes on covered positions
     * of one level, over more than one tile.
     */
    public static boolean isPlateauPlacement(VentlifeGameState state, HexCoord smoker, int orientation) {
        HexCell under = state.getCell(smoker);
        if (under == null || under.terrain() != Terrain.BLACK_SMOKER)
            return false;
        boolean differentTiles = false;
        for (HexCoord h : tileHexes(smoker, orientation)) {
            HexCell cell = state.getCell(h);
            if (cell == null || cell.level() != under.level())
                return false;
            if (cell.tileId() != under.tileId())
                differentTiles = true;
        }
        return differentTiles;
    }

    /**
     * Whether a plateau tile could now be placed over a creature of the species on the hex, displacing it. Tube Worms
     * under the new tile's Black Smoker climb onto it, so are not at risk there.
     */
    public static boolean isExposed(VentlifeGameState state, HexCoord hex, Species species) {
        // the tile's Black Smoker is on the hex or next to it
        List<HexCoord> smokers = new ArrayList<>(hex.neighbours());
        smokers.add(hex);
        for (HexCoord smoker : smokers) {
            if (species == Species.TUBE_WORM && smoker.equals(hex))
                continue;
            for (int o = 0; o < 6; o++)
                if (tileHexes(smoker, o).contains(hex) && isPlateauPlacement(state, smoker, o))
                    return true;
        }
        return false;
    }

    /**
     * The Elevated Plateaus: connected groups of covered positions of one level, 2 or more.
     */
    public static List<Set<HexCoord>> plateaus(VentlifeGameState state) {
        Map<Integer, Set<HexCoord>> byLevel = new HashMap<>();
        for (Map.Entry<HexCoord, HexCell> e : state.getField().entrySet())
            if (e.getValue().level() >= 2)
                byLevel.computeIfAbsent(e.getValue().level(), k -> new HashSet<>()).add(e.getKey());
        List<Set<HexCoord>> retValue = new ArrayList<>();
        for (Set<HexCoord> sameLevel : byLevel.values())
            for (Set<HexCoord> group : GridCoord.connectedGroups(sameLevel))
                if (group.size() >= 2)
                    retValue.add(group);
        return retValue;
    }

    /**
     * The number of Yeti Crabs each player has on the positions.
     */
    public static int[] crabsOn(VentlifeGameState state, Set<HexCoord> positions) {
        int[] retValue = new int[state.getNPlayers()];
        for (HexCoord h : positions) {
            Creature c = state.getCreature(h);
            if (c != null && c.species() == Species.YETI_CRAB)
                retValue[c.owner()] += c.count();
        }
        return retValue;
    }
}

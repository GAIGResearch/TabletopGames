package games.ventlife;

import core.components.grid.HexCoord;
import games.ventlife.components.Species;

import java.util.List;

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
}

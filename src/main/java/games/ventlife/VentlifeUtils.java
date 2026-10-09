package games.ventlife;

import games.ventlife.components.Hex;

import java.util.List;

public class VentlifeUtils {

    private VentlifeUtils() {
    }

    /**
     * The three positions covered by a tile: its Black Smoker, then its left and right terrains. The right terrain is
     * in direction orientation from the Black Smoker and the left in the next direction clockwise, so orientation 1
     * puts the Black Smoker at the top with the left terrain below-left and the right below-right, as printed.
     */
    public static List<Hex> tileHexes(Hex smoker, int orientation) {
        return List.of(smoker, smoker.neighbour((orientation + 1) % 6), smoker.neighbour(orientation));
    }
}

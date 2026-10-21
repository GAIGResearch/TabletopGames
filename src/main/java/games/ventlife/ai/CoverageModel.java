package games.ventlife.ai;

import games.ventlife.VentlifeGameState;
import games.ventlife.components.Hex;
import games.ventlife.components.Species;

import java.util.Map;

/**
 * The probability that a creature is covered before the end of the game: a logistic model fitted (by
 * scripts/fitCoverage.py) to games between greedy agents recorded by CoverageData.
 */
public class CoverageModel {

    // bias, exposed, level, edge, sameLevelNbrs, emptyNbrs, higherNbrs, lowerNbrs, smokersNear, t (tiles left / 60),
    // ownerMovesNext, exposed*t, smokersNear*t, sameLevelNbrs*t, edge*t, then the species (Tube Worms are the base)
    private static final double[] W = {-0.2563, 4.4051, -1.0822, -0.4343, -0.5837, -0.0242, -0.8216, -0.1082, 0.0700,
            3.7190, -0.1720, -2.0318, 0.5693, -0.2877, -0.0932, 2.1677, 2.0069, 1.7163, 1.5469, 2.1070, 2.1951};
    private static final Species[] SPECIES_ORDER = {Species.VOLCANO_SNAIL, Species.VENT_SHRIMP, Species.EELPOUT_FISH,
            Species.YETI_CRAB, Species.OCTOPUS, Species.VENT_SPONGE};

    private CoverageModel() {
    }

    /**
     * The probability for a creature of the species on the hex, given the exposed positions and the tiles left.
     */
    public static double probability(VentlifeGameState state, Hex hex, Species species, Map<Hex, Integer> exposed,
                                     int tilesLeft) {
        double[] f = CoverageData.features(state, hex, species, exposed, tilesLeft);
        double t = f[8] / 60.0;
        double z = W[0] + W[1] * f[0] + W[2] * f[1] + W[3] * f[2] + W[4] * f[3] + W[5] * f[4] + W[6] * f[5]
                + W[7] * f[6] + W[8] * f[7] + W[9] * t + W[10] * f[9]
                + W[11] * f[0] * t + W[12] * f[7] * t + W[13] * f[3] * t + W[14] * f[2] * t;
        for (int i = 0; i < SPECIES_ORDER.length; i++)
            if (species == SPECIES_ORDER[i])
                z += W[15 + i];
        return 1.0 / (1.0 + Math.exp(-z));
    }

    public static int tilesLeft(VentlifeGameState state) {
        int retValue = state.getDrawDeck().getSize();
        for (int p = 0; p < state.getNPlayers(); p++)
            retValue += state.getHand(p).getSize();
        return retValue;
    }
}

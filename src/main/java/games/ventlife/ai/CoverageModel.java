package games.ventlife.ai;

import games.ventlife.VentlifeGameState;
import games.ventlife.components.Hex;
import games.ventlife.components.Species;

import java.util.Map;

/**
 * The probability that a creature is covered before the end of the game: logistic models fitted (by
 * scripts/fitCoverage.py) to games recorded by CoverageData.
 */
public class CoverageModel {

    // bias, exposed, level, edge, sameLevelNbrs, emptyNbrs, higherNbrs, lowerNbrs, smokersNear, t (tiles left / 60),
    // ownerMovesNext, exposed*t, smokersNear*t, sameLevelNbrs*t, edge*t, then the species (Tube Worms are the base)
    private static final double[] W1 = {-0.2563, 4.4051, -1.0822, -0.4343, -0.5837, -0.0242, -0.8216, -0.1082, 0.0700,
            3.7190, -0.1720, -2.0318, 0.5693, -0.2877, -0.0932, 2.1677, 2.0069, 1.7163, 1.5469, 2.1070, 2.1951};
    private static final double[] W2 = {-0.3163, 4.1427, -0.7426, -0.1548, -0.6599, -0.2481, -0.7943, -0.1954, 0.1624,
            3.5430, -0.1571, -2.9771, 0.1372, -0.0918, -0.1295, 1.1286, 1.4072, 0.5484, 1.2778, 1.1949, 1.6760};
    // the fitted models, by number from 1: model 1 from 150 games between greedy agents (Exp06), model 2 from 183
    // games between reply play-out agents (Exp11)
    private static final double[][] MODELS = {W1, W2};
    private static final Species[] SPECIES_ORDER = {Species.VOLCANO_SNAIL, Species.VENT_SHRIMP, Species.EELPOUT_FISH,
            Species.YETI_CRAB, Species.OCTOPUS, Species.VENT_SPONGE};

    private CoverageModel() {
    }

    /**
     * The probability for a creature of the species on the hex, given the exposed positions and the tiles left.
     */
    public static double probability(VentlifeGameState state, Hex hex, Species species, Map<Hex, Integer> exposed,
                                     int tilesLeft, int model) {
        return probability(CoverageData.features(state, hex, species, exposed, tilesLeft), species, model);
    }

    /**
     * The probability for a creature of the species with the given CoverageData features.
     */
    public static double probability(double[] f, Species species, int model) {
        double[] W = MODELS[model - 1];
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

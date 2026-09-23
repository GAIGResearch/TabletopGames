package games.golfsix;

import core.components.FrenchCard;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static games.golfsix.GolfSixParameters.COLUMNS;

/**
 * Scoring and card visibility for Six-card Golf.
 */
public class GolfSixUtils {

    /**
     * The points scored by a grid of cards, listed by position. The two cards of a column that are of the same rank
     * score nothing; every other card scores GolfSixParameters.cardValue.
     */
    public static int gridScore(List<FrenchCard> grid, GolfSixParameters params) {
        int score = 0;
        for (int column = 0; column < COLUMNS; column++) {
            FrenchCard top = grid.get(column);
            FrenchCard bottom = grid.get(column + COLUMNS);
            if (top.number != bottom.number)
                score += params.cardValue(top) + params.cardValue(bottom);
        }
        return score;
    }

    /**
     * A lower bound on the points a grid can score.
     */
    public static int minDealScore(GolfSixParameters params) {
        return GolfSixParameters.GRID_SIZE * Math.min(0, rankValues(params).min().orElse(0));
    }

    /**
     * An upper bound on the points a grid can score.
     */
    public static int maxDealScore(GolfSixParameters params) {
        return GolfSixParameters.GRID_SIZE * Math.max(0, rankValues(params).max().orElse(0));
    }

    public static boolean[] visibleToAll(int nPlayers) {
        boolean[] visibility = new boolean[nPlayers];
        Arrays.fill(visibility, true);
        return visibility;
    }

    private static IntStream rankValues(GolfSixParameters params) {
        // the number cards from 3 to 10 score their number
        return IntStream.of(params.aceValue, params.twoValue, params.courtValue, params.kingValue, 10, 3);
    }
}

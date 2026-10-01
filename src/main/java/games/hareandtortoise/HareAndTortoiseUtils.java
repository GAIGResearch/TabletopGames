package games.hareandtortoise;

import java.util.function.IntPredicate;

import static games.hareandtortoise.HareAndTortoiseParameters.HOME_SQUARE;

public final class HareAndTortoiseUtils {

    private HareAndTortoiseUtils() {
    }

    /**
     * The first board square (1-63), starting at the given square and stepping by step (1 forwards, -1 backwards),
     * that passes the test; or 0 if none does.
     */
    public static int firstSquare(int start, int step, IntPredicate test) {
        for (int s = start; s > 0 && s < HOME_SQUARE; s += step)
            if (test.test(s))
                return s;
        return 0;
    }

    /**
     * The nearest tortoise square behind the given square, or 0 if there is none.
     */
    public static int previousTortoise(int square) {
        return firstSquare(square - 1, -1, s -> HareAndTortoiseParameters.BOARD[s] == SquareType.TORTOISE);
    }
}

package games.hareandtortoise;

/**
 * The kinds of square on the Hare and Tortoise board. START and HOME are the spaces before the first square and after
 * the last one.
 */
public enum SquareType {
    START, HARE, CARROT, LETTUCE, TORTOISE,
    FLAG(1, 5, 6), TWO(2), THREE(3), FOUR(4),
    HOME;

    private final int[] racePositions;

    SquareType(int... racePositions) {
        this.racePositions = racePositions;
    }

    /**
     * Whether this is a number square that pays out to a runner in the given position in the race.
     */
    public boolean paysRacePosition(int racePosition) {
        for (int p : racePositions)
            if (p == racePosition)
                return true;
        return false;
    }

    public boolean isNumber() {
        return racePositions.length > 0;
    }
}

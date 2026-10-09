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

    /**
     * Carries out what happens when the player's runner moves onto a square of this type, having paid the given
     * number of carrots for the move.
     */
    public void landOn(HareAndTortoiseGameState state, int player, int carrotsPaid) {
        switch (this) {
            case HOME -> state.setFinishPosition(player, state.getNPlayersHome() + 1);
            // the next turn is spent chewing a lettuce
            case LETTUCE -> state.setLettuceToChew(player, true);
            case HARE -> state.drawHareCard().type.apply(state, player, carrotsPaid);
            default -> {
            }
        }
    }
}

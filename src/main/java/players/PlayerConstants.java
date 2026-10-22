package players;

/**
 * We have five distinct budgets to constrain MCTS:
 * - TIME is measured in milliseconds
 * - ITERATIONS is self-explanatory
 * - FM_CALLS limits the number if times the ForwardModel.next() function can be called
 * - COPY_CALLS limits the number of times a state can be copied (this is frequently quite a bit more time-consuming
 *   then ForwardModel.next(), so for some games FM directly may be an unfair comparator
 * - FMANDCOPY_CALLS limits the sum of the calls to copy() or next()
 * - GAME_SECONDS is the total thinking time, in whole seconds, the player has over the course of a game.
 *   The player tracks this itself (see AbstractPlayer.getGameTimer()); it is not enforced by Game.
 * - ROUND_SECONDS is the same, but for each round of the game: the clock is reset at the player's first decision in
 *   each round, and time not used in a round is lost.
 */
public enum PlayerConstants {
    BUDGET_TIME, BUDGET_ITERATIONS, BUDGET_FM_CALLS, BUDGET_FMANDCOPY_CALLS, BUDGET_COPY_CALLS, BUDGET_GAME_SECONDS,
    BUDGET_ROUND_SECONDS;

    /**
     * @return true if the budget is a player's clock (thinking time over a game or a round)
     */
    public boolean isClock() {
        return this == BUDGET_GAME_SECONDS || this == BUDGET_ROUND_SECONDS;
    }
}

package games.tricktaking;

/**
 * <p>A trick-taking game state whose agents may be set to forget the suits other players are known to be void in
 * (see {@link ForgetVoidsDecorator}).</p>
 *
 * <p>To support this in another game, implement {@link #redeterminise} by moving the reshuffle out of
 * {@code _copy(playerId)} into a method that works on the state itself, and call that from {@code _copy} on the new
 * copy. Note that a copy only receives its core parameters and round counter after {@code _copy}, so inside
 * {@code _copy} anything that depends on them (partialObservable, the dealer) must be taken from the original.</p>
 *
 * @param <S> the type of suit
 */
public interface IForgettableVoids<S extends Enum<S>> {

    KnownVoids<S> getKnownVoids();

    /**
     * Reshuffles, in place, the cards hidden from the player, as {@code copy(playerId)} does for a copy (and only if
     * the game is played with partial observability).
     */
    void redeterminise(int playerId);

    /**
     * Forgets the known voids, so that neither this state nor any copy of it records or uses them, and redeterminises
     * the state from the player's perspective without them. Does nothing if they are already forgotten.
     */
    default void forgetVoids(int playerId) {
        if (getKnownVoids().forget())
            redeterminise(playerId);
    }
}

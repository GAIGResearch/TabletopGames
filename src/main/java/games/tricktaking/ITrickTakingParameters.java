package games.tricktaking;

/**
 * What the shared trick-taking code needs from a game's parameters.
 */
public interface ITrickTakingParameters {

    /**
     * Whether players remember the suits others are void in: if not, {@link KnownVoids} records nothing, and
     * redeterminisation is unconstrained.
     */
    boolean rememberVoids();
}

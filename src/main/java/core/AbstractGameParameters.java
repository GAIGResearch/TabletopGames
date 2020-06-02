package core;

public abstract class AbstractGameParameters {

    // Random seed for the game
    protected long gameSeed;

    public AbstractGameParameters(long seed) {
        this.gameSeed = seed;
    }

    /* Methods to be implemented by subclass */

    /**
     * Return a copy of this game parameters object, with the same parameters as in the original.
     * @return - new game parameters object.
     */
    protected abstract AbstractGameParameters _copy();


    /* Public API */

    /**
     * Retrieve the random seed for this game.
     * @return - random seed.
     */
    public long getGameSeed() {
        return gameSeed;
    }

    /**
     * Copy this game parameter object.
     * @return - new object with the same parameters, but a new random seed.
     */
    public AbstractGameParameters copy() {
        AbstractGameParameters copy = _copy();
        copy.gameSeed = System.currentTimeMillis();
        return copy;
    }
}

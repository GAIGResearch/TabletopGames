package core;

public abstract class AbstractGameParameters {

    // Random seed for the game
    protected long gameSeed;

    public long getGameSeed() {
        return gameSeed;
    }

    public AbstractGameParameters(long seed) {
        this.gameSeed = seed;
    }

    protected abstract AbstractGameParameters _copy();

    public AbstractGameParameters copy() {
        AbstractGameParameters copy = _copy();
        copy.gameSeed = System.currentTimeMillis();
        return copy;
    }
}

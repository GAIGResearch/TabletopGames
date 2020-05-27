package core;

public abstract class AbstractGameParameters {

    // Random seed for the game
    protected long gameSeed = System.currentTimeMillis(); //0;

    public long getGameSeed() {
        return gameSeed;
    }
}

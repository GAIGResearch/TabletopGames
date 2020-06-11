package minigames.descentTileBuild;

import core.AbstractGameParameters;

public class TileBuildParameters extends AbstractGameParameters {
    public int defaultGridSize = 5;
    public int maxGridSize = 50;

    public TileBuildParameters(long seed) {
        super(seed);
    }

    @Override
    protected AbstractGameParameters _copy() {
        return new TileBuildParameters(System.currentTimeMillis());
    }
}

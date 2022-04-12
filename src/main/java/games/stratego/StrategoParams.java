package games.stratego;

import core.AbstractParameters;

public class StrategoParams extends AbstractParameters {
    public int gridSize = 10;
    public int[] xRestrictedTiles = {2,3,6,7};
    public int[] yRestrictedTiles = {4,5};
    public int moveSpeed = 1;
    public int attackRange = 1;
    public int[] pieceSetupCount = {1,8,5,4,4,4,3,2,1,1}; // {1,1,2,3,4,4,4,5,8,1};
    public int pieceSetupNBombs = 6;
    public int pieceSetupNFlags = 1;
    public int maxRounds = 500;

    public boolean isTileValid(final int x, final int y){
        if ((x>=0 && x<gridSize) && (y>=0 && y<gridSize)){
            return (x != xRestrictedTiles[0] || y != yRestrictedTiles[0]) && (x != xRestrictedTiles[0] || y != yRestrictedTiles[1])
                    && (x != xRestrictedTiles[1] || y != yRestrictedTiles[0]) && (x != xRestrictedTiles[1] || y != yRestrictedTiles[1])
                    && (x != xRestrictedTiles[2] || y != yRestrictedTiles[0]) && (x != xRestrictedTiles[2] || y != yRestrictedTiles[1])
                    && (x != xRestrictedTiles[3] || y != yRestrictedTiles[0]) && (x != xRestrictedTiles[3] || y != yRestrictedTiles[1]);
        } else {
            return false;
        }
    }

    public StrategoParams(long seed) {
        super(seed);
    }

    @Override
    protected AbstractParameters _copy() {
        return new StrategoParams(System.currentTimeMillis());
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }
}

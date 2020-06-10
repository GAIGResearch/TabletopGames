package games.catan;

import core.AbstractGameParameters;

public class CatanParameters extends AbstractGameParameters {
    private String dataPath;
    int n_actions_per_turn = 1;

    int settlements_per_player = 5;
    int cities_per_player = 4;
    int roads_per_player = 15;

    public CatanParameters(long seed){
        super(seed);
    }

    public CatanParameters(String dataPath, long seed){
        super(seed);
        this.dataPath = dataPath;
    }

    public String getDataPath(){
        return dataPath;
    }

    @Override
    protected AbstractGameParameters _copy() {
        return null;
    }

    public enum TileType {
        HILLS,
        FOREST,
        MOUNTAINS,
        FIELDS,
        PASTURE,
        DESERT,
        SEA
    }

    public enum Resources {
        BRICK,
        LUMBER,
        ORE,
        GRAIN,
        WOOL
    }

    public enum CardTypes{
        KNIGHT_CARD,
        PROGRESS_CARD,
        VICTORY_POINT_CARD
    }
}

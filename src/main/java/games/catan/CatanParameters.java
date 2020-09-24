package games.catan;

import core.AbstractParameters;

import java.util.HashMap;

public class CatanParameters extends AbstractParameters {
    private String dataPath;
    int n_actions_per_turn = 1;

    int n_players= 4;

    int n_settlements = 5;
    int n_cities = 4;
    int n_roads = 15;
    int n_resource_cards = 19;
    int n_tiles_per_row = 7;

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
    protected AbstractParameters _copy() {
        return null;
    }

    @Override
    protected boolean _equals(Object o) {
        return false;
    }

    HashMap<TileType, Integer> tileCounts = new HashMap<CatanParameters.TileType, Integer>() {{
        put(CatanParameters.TileType.HILLS, 3);
        put(CatanParameters.TileType.FOREST, 4);
        put(CatanParameters.TileType.MOUNTAINS, 3);
        put(CatanParameters.TileType.FIELDS, 4);
        put(CatanParameters.TileType.PASTURE, 4);
//        put(CatanParameters.TileType.DESERT, 1);
    }};

    // put(CatanParameters.TileType.SEA, 18);
    //


    // todo that could be simplified
    // [2, 12]    x 1
    // [3,...,11] x 2
    HashMap<Integer, Integer> numberTokens = new HashMap<Integer, Integer>(){{
        put(2, 1);
        put(3, 2);
        put(4, 2);
        put(5, 2);
        put(6, 2);
        put(8, 2);
        put(9, 2);
        put(10, 2);
        put(11, 2);
        put(12, 1);
    }};

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

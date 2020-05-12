package games.catan;

import core.GameParameters;

public class CatanParameters extends GameParameters {
    private String dataPath;
    int n_actions_per_turn = 1;

    int settlements_per_player = 5;
    int cities_per_player = 4;
    int roads_per_player = 15;

    protected CatanParameters(String dataPath){
        this.dataPath = dataPath;
    }

    public String getDataPath(){
        return dataPath;
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

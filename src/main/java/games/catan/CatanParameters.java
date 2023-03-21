package games.catan;

import core.AbstractParameters;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;

import java.util.HashMap;

import static games.catan.CatanParameters.ActionType.*;
import static games.catan.CatanParameters.Resource.*;

public class CatanParameters extends AbstractParameters {
    private String dataPath;

    public final int n_resource_cards = 19;
    public final int n_tiles_per_row = 7;

    // rules
    public final int max_negotiation_count = 2; // max number of attempts to renegotiate player trade
    public final int default_exchange_rate = 4; // trading with the bank 1:4 ratio by default
    public final int max_cards_without_discard = 7; // max number of resources a player may hold in hand without risking discarding
    public final int max_trade_actions_allowed = 2; // max number of trade actions per turn
    public final int max_build_actions_allowed = 3; // max number of build actions per turn

    // the minimum number of knights required to take the largest army
    public final int min_army_size = 3;
    public final int min_longest_road = 4; // only changes when road_length > min

    // points
    public final int settlement_value = 1;
    public final int city_value = 2;
    public final int longest_road_value = 2;
    public final int largest_army_value = 2;
    public final int victory_point_value = 1;
    public final int points_to_win = 10;

    public CatanParameters(long seed){
        super(seed);
        setMaxRounds(100);
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

    HashMap<CatanTile.TileType, Integer> tileCounts = new HashMap<CatanTile.TileType, Integer>() {{
        put(CatanTile.TileType.HILLS, 3);
        put(CatanTile.TileType.FOREST, 4);
        put(CatanTile.TileType.MOUNTAINS, 3);
        put(CatanTile.TileType.FIELDS, 4);
        put(CatanTile.TileType.PASTURE, 4);
        put(CatanTile.TileType.DESERT, 1);
    }};


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

    public enum Resource {
        BRICK,
        LUMBER,
        ORE,
        GRAIN,
        WOOL
    }

    public enum HarborType {
        NONE,
        BRICK,
        LUMBER,
        ORE,
        GRAIN,
        WOOL,
        GENERIC
    }

    /* Mapping of which field produces what*/
    public HashMap<CatanTile.TileType, Resource> productMapping = new HashMap<CatanTile.TileType, Resource>(){{
        put(CatanTile.TileType.HILLS, BRICK);
        put(CatanTile.TileType.FOREST, Resource.LUMBER);
        put(CatanTile.TileType.MOUNTAINS, Resource.ORE);
        put(CatanTile.TileType.FIELDS, Resource.GRAIN);
        put(CatanTile.TileType.PASTURE, Resource.WOOL);
        put(CatanTile.TileType.DESERT, null);
        put(CatanTile.TileType.SEA, null);
    }};

    public enum ActionType {
        Settlement,
        City,
        Road,
        DevCard
    }

    /* Mapping from name to price of item (cost is in the same order as Resources) */
    public HashMap<ActionType, HashMap<Resource, Integer>> costMapping = new HashMap<ActionType, HashMap<Resource, Integer>>(){{
        // cost order: Brick, lumber, ore, grain, wool
        put(Settlement, new HashMap<Resource, Integer>() {{
            put(BRICK, 1);
            put(LUMBER, 1);
            put(GRAIN, 1);
            put(WOOL, 1);
        }});
        put(City, new HashMap<Resource, Integer>() {{
            put(ORE, 3);
            put(GRAIN, 2);
        }});
        put(Road, new HashMap<Resource, Integer>() {{
            put(BRICK, 1);
            put(LUMBER, 1);
        }});
        put(DevCard, new HashMap<Resource, Integer>() {{
            put(ORE, 1);
            put(GRAIN, 1);
            put(WOOL, 1);
        }});
    }};

    HashMap<ActionType, Integer> tokenCounts = new HashMap<ActionType, Integer>() {{
        put(Settlement, 5);
        put(City, 4);
        put(Road, 15);
    }};

    HashMap<CatanCard.CardType, Integer> developmentCardCount = new HashMap<CatanCard.CardType, Integer>() {{
        put(CatanCard.CardType.KNIGHT_CARD, 14);
        put(CatanCard.CardType.MONOPOLY, 2);
        put(CatanCard.CardType.YEAR_OF_PLENTY, 2);
        put(CatanCard.CardType.ROAD_BUILDING, 2);
        put(CatanCard.CardType.VICTORY_POINT_CARD, 5);
    }};

    public static HashMap<HarborType, Integer> harborCount = new HashMap<HarborType, Integer>() {{
        put(HarborType.BRICK, 1);
        put(HarborType.LUMBER, 1);
        put(HarborType.ORE, 1);
        put(HarborType.GRAIN, 1);
        put(HarborType.WOOL, 1);
        put(HarborType.GENERIC, 4);
    }};

    @Override
    public AbstractParameters copy() {
        // todo set all variables
        CatanParameters copy = new CatanParameters(getRandomSeed());
        return copy;
    }
}

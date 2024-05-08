package games.catan;

import core.AbstractParameters;
import core.components.Dice;
import games.catan.actions.build.BuyAction;
import games.catan.components.Building;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;

import java.util.HashMap;

import static games.catan.actions.build.BuyAction.BuyType.*;
import static games.catan.CatanParameters.Resource.*;

public class CatanParameters extends AbstractParameters {
    private String dataPath;
    public int maxRounds = 1000;

    public int n_resource_cards = 19;
    public int n_tiles_per_row = 7;

    // Dice
    public Dice.Type dieType = Dice.Type.d6;
    public int nDice = 2;
    public int robber_die_roll = 7;

    // rules
    public int max_negotiation_count = 2; // max number of attempts to renegotiate player trade
    public int default_exchange_rate = 4; // trading with the bank 1:4 ratio by default
    public int max_resources_request_trade = 2; // trading with the bank 1:4 ratio by default
    public int max_cards_without_discard = 7; // max number of resources a player may hold in hand without risking discarding
    public int max_trade_actions_allowed = 2; // max number of trade actions per turn
    public double perc_discard_robber = 0.5;

    // the minimum number of knights required to take the largest army
    public int min_army_size = 3;
    public int min_longest_road = 4;

    // points
    public int longest_road_value = 2;
    public int largest_army_value = 2;
    public int points_to_win = 10;

    public int harbour_exchange_rate = 2;
    public int harbour_wild_exchange_rate = 3;
    public int n_settlements_setup = 2;
    public int nResourcesYoP = 2;
    public int nRoadsRB = 2;

    public HashMap<Building.Type, Integer> buildingValue = new HashMap<Building.Type, Integer>() {{
        put(Building.Type.Settlement, 1);
        put(Building.Type.City, 2);
    }};
    public HashMap<Building.Type, Integer> nProduction = new HashMap<Building.Type, Integer>() {{
        put(Building.Type.Settlement, 1);
        put(Building.Type.City, 2);
    }};

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
        WOOL,
        WILD
    }

    public enum ResourceAmount {
        Few("."),
        Some("?"),
        Many("??"),
        Lots("???");
        public final String po;
        ResourceAmount(String po) {
            this.po = po;
        }
        public static ResourceAmount translate(int amount, CatanParameters cp) {
            double perc = amount*1.0 / cp.n_resource_cards;
            int idx = (int)(values().length * perc);
            if (idx == 4) idx--;
            return values()[idx];
        }
    }

    /* Mapping of which field produces what*/
    public HashMap<CatanTile.TileType, Resource> productMapping = new HashMap<CatanTile.TileType, Resource>(){{
        put(CatanTile.TileType.HILLS, BRICK);
        put(CatanTile.TileType.FOREST, LUMBER);
        put(CatanTile.TileType.MOUNTAINS, ORE);
        put(CatanTile.TileType.FIELDS, GRAIN);
        put(CatanTile.TileType.PASTURE, WOOL);
        put(CatanTile.TileType.DESERT, null);
        put(CatanTile.TileType.SEA, null);
    }};

    /* Mapping from name to price of item (cost is in the same order as Resources) */
    public HashMap<BuyAction.BuyType, HashMap<Resource, Integer>> costMapping = new HashMap<BuyAction.BuyType, HashMap<Resource, Integer>>(){{
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

    HashMap<BuyAction.BuyType, Integer> tokenCounts = new HashMap<BuyAction.BuyType, Integer>() {{
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

    public HashMap<Resource, Integer> harborCount = new HashMap<Resource, Integer>() {{
        put(Resource.BRICK, 1);
        put(Resource.LUMBER, 1);
        put(Resource.ORE, 1);
        put(Resource.GRAIN, 1);
        put(Resource.WOOL, 1);
        put(Resource.WILD, 4);
    }};

    public CatanParameters(long seed){
        super(seed);
        setMaxRounds(1000);
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

    @Override
    public AbstractParameters copy() {
        // todo set all variables
        CatanParameters copy = new CatanParameters(getRandomSeed());
        return copy;
    }
}

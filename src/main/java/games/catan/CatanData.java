package games.catan;

import core.AbstractGameData;
import core.components.*;

import java.util.HashMap;
import java.util.List;

public class CatanData extends AbstractGameData {

    private List<GraphBoard> boards;
    private List<Deck<Card>> decks;
    private List<Token> tokens;
    private List<Counter> counters;

    private int victoryPoints;

    @Override
    public void load(String dataPath) {
        // load all components, tiles, decks, counters....
        // todo
        // 126 cards - resources, bonus
        // 37 tiles
        // 90 tokens
        // 18 number tiles
        // 2 dice
        // 1 wooden tile - thief
        HashMap<CatanParameters.TileType, Integer> tileCounts = new HashMap<CatanParameters.TileType, Integer>() {{
            put(CatanParameters.TileType.HILLS, 3);
            put(CatanParameters.TileType.FOREST, 4);
            put(CatanParameters.TileType.MOUNTAINS, 3);
            put(CatanParameters.TileType.FIELDS, 4);
            put(CatanParameters.TileType.PASTURE, 4);
            put(CatanParameters.TileType.DESERT, 1);
            put(CatanParameters.TileType.SEA, 18);
        }};

        // todo that could be simplified
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

        // city tokens

        // resources


        // Build development deck
        HashMap<CatanParameters.CardTypes, Integer> developmentCounts = new HashMap<CatanParameters.CardTypes, Integer>(){{
            put(CatanParameters.CardTypes.KNIGHT_CARD, 10);
        }};


    }

    @Override
    public GraphBoard findBoard(String name) {
        return null;
    }

    @Override
    public Counter findCounter(String name) {
        return null;
    }

    @Override
    public Token findToken(String name) {
        return null;
    }

    @Override
    public Deck findDeck(String name) {
        return null;
    }
}

package games.catan;

import core.AbstractGameData;
import core.components.*;
import core.properties.Property;
import core.properties.PropertyBoolean;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import utilities.Hash;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class CatanData extends AbstractGameData {

    private List<Area> areas;
    private List<Deck<Card>> decks;
    private List<Token> tokens;
    private List<Counter> counters;

    private CatanParameters params;

    public CatanData(CatanParameters params){
        this.params = params;
        this.areas = new ArrayList<>();
        this.decks = new ArrayList<>();
    }

    @Override
    public void load(String dataPath) {
        // load all components, tiles, decks, counters....

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

        // add player tokens (counters)
        for (int i = 0; i < params.n_players; i++){
            Area area = new Area(i, "PlayerArea_" + i);

            Counter cityCounter = new Counter(params.n_cities, 0, params.n_cities, "cityCounter");
            Counter settlementCounter = new Counter(params.n_settlements, 0, params.n_settlements, "settlementCounter");
            Counter roadCounter = new Counter(params.n_roads, 0, params.n_roads, "roadCounter");

            area.putComponent(CatanConstants.cityCounterHash, cityCounter);
            area.putComponent(CatanConstants.settlementCounterHash, settlementCounter);
            area.putComponent(CatanConstants.roadCounterHash, roadCounter);

            areas.add(area);
        }

        // create resource cards
        Deck<Card> resourceDeck = new Deck("resourceDeck");
        for (CatanParameters.Resources res: CatanParameters.Resources.values()) {
            for (int i = 0; i < params.n_resource_cards; i++) {
                Card c = new Card();
                c.setProperty(new PropertyString("cardType", res.name()));
                resourceDeck.add(c);
            }
        }

        // Build development deck
        HashMap<CatanParameters.CardTypes, Integer> developmentCounts = new HashMap<CatanParameters.CardTypes, Integer>(){{
            put(CatanParameters.CardTypes.KNIGHT_CARD, 10);
        }};

        List<Deck<Card>> developmentTypes = Deck.loadDecksOfCards(dataPath + "catan/decks.json");
        Deck<Card> developmentDeck = new Deck("developmentDeck");
        for (Deck<Card> devDeck: developmentTypes){
            // first pass is the devDeck and second is the resource deck
            for (Card c: devDeck.getComponents()){
                PropertyInt count = (PropertyInt)c.getProperty(CatanConstants.countHash);
                if (count != null){
                    for (int i = 0; i < count.value-1; i++){
                        Card cardCopy = c.copy();
                        developmentDeck.add(cardCopy);
                    }
                }
            }
        }


        // merge decks
        decks.add(resourceDeck);
        decks.add(developmentDeck);
    }

    @Override
    public Deck<Card> findDeck(String name) {
        for (Deck<Card> d: decks) {
            if (name.equalsIgnoreCase(d.getComponentName())) {
                return d.copy();
            }
        }
        return null;
    }

}
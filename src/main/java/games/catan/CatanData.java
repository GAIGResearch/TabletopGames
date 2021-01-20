package games.catan;

import core.AbstractGameData;
import core.CoreConstants;
import core.components.*;
import core.properties.Property;
import core.properties.PropertyBoolean;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import utilities.Hash;

import java.util.*;


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
        // Create all components, tiles, decks, counters....

        // add player tokens (counters)
        for (int i = 0; i < params.n_players; i++){
            Area area = new Area(i, "PlayerArea_" + i);

            Counter cityCounter = new Counter(params.n_cities, 0, params.n_cities, "cityCounter");
            Counter settlementCounter = new Counter(params.n_settlements, 0, params.n_settlements, "settlementCounter");
            Counter roadCounter = new Counter(params.n_roads, 0, params.n_roads, "roadCounter");

            area.putComponent(CatanConstants.cityCounterHash, cityCounter);
            area.putComponent(CatanConstants.settlementCounterHash, settlementCounter);
            area.putComponent(CatanConstants.roadCounterHash, roadCounter);
            area.putComponent(CoreConstants.playerHandHash, new Deck("playerHand"));

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

        Deck<Card> developmentDeck = new Deck("developmentDeck");
        for (Map.Entry<String, Integer> entry: params.developmentCardCount.entrySet()){
            for (int i = 0; i < entry.getValue(); i++){
                Card card = new Card();
                card.setProperty(new PropertyString("cardType", entry.getKey()));
                developmentDeck.add(card);
            }
        }
        developmentDeck.shuffle(new Random(params.getRandomSeed()));
//        List<Deck<Card>> developmentTypes = Deck.loadDecksOfCards(dataPath + "catan/decks.json");
//        for (Deck<Card> devDeck: developmentTypes){
//            // first pass is the devDeck and second is the resource deck
//            for (Card c: devDeck.getComponents()){
//                PropertyInt count = (PropertyInt)c.getProperty(CatanConstants.countHash);
//                if (count != null){
//                    for (int i = 0; i < count.value-1; i++){
//                        Card cardCopy = c.copy();
//                        developmentDeck.add(cardCopy);
//                    }
//                }
//            }
//        }


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
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

    private GraphBoard board;
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

        // generate graph board
        board = generateBoard();

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
                c.setProperty(CatanConstants.cardType, new PropertyString("cardType", res.name()));
                resourceDeck.add(c);
            }
        }

        // Build development deck
        HashMap<CatanParameters.CardTypes, Integer> developmentCounts = new HashMap<CatanParameters.CardTypes, Integer>(){{
            put(CatanParameters.CardTypes.KNIGHT_CARD, 10);
        }};

        List<Deck<Card>> developmentDeck = Deck.loadDecksOfCards(dataPath + "catan/decks.json");
        Deck tmpDeck = new Deck("tmpDeck");
        for (Deck<Card> devDeck: developmentDeck){
            // first pass is the devDeck and second is the resource deck
            for (Card c: devDeck.getComponents()){
                PropertyInt count = (PropertyInt)c.getProperty(CatanConstants.countHash);
                if (count != null){
                    for (int i = 0; i < count.value-1; i++){
                        Card cardCopy = c.copy();
                        tmpDeck.add(cardCopy);
                    }
                }
            }
        }
        // todo also contains the resource deck
        developmentDeck.add(tmpDeck);


        // add decks to decks
        decks.add(resourceDeck);
        decks.addAll(developmentDeck);



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

    private GraphBoard generateBoard(){
        // todo need to shuffle and set them according to the rules
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

        GraphBoard board = new GraphBoard();
        ArrayList<BoardNode> boardNodes = new ArrayList<>();
        for (Map.Entry tileCount : tileCounts.entrySet()){
            // todo create component ids
            for (int i = 0; i < (int)tileCount.getValue(); i++){
                BoardNode bn = new BoardNode(6, tileCount.getKey().toString() + "_" + i);
//                bn.setProperty(Hash.GetInstance().hash("number"), new PropertyInt("a",1));
                bn.setProperty(CatanConstants.typeHash, new PropertyString("type", tileCount.getKey().toString()));
                bn.setProperty(CatanConstants.robberHash, new PropertyBoolean("robber", false));
                boardNodes.add(bn);
            }
        }
        board.setBoardNodes(boardNodes);

        // todo steps:
        // 1, put desert in middle
        // 2, distribute all the resource tiles with number tokens
        // 3, distribute sea tiles
        return board;

    }
}
// ------- from GraphBoard copy() --------
//        GraphBoard b = new GraphBoard(componentName, componentID);
//        HashMap<Integer, BoardNode> nodeCopies = new HashMap<>();
//        // Copy board nodes
//        for (BoardNode bn: boardNodes) {
//            BoardNode bnCopy = new BoardNode(bn.getMaxNeighbours(), "", bn.getComponentID());
//            bn.copyComponentTo(bnCopy);
//            nodeCopies.put(bn.getComponentID(), bnCopy);
//        }
//        // Assign neighbours
//        for (BoardNode bn: boardNodes) {
//            BoardNode bnCopy = nodeCopies.get(bn.getComponentID());
//            for (BoardNode neighbour: bn.getNeighbours()) {
//                bnCopy.addNeighbour(nodeCopies.get(neighbour.getComponentID()));
//            }
//            for (Map.Entry<BoardNode, Integer> e: bn.getNeighbourSideMapping().entrySet()) {
//                bnCopy.addNeighbour(nodeCopies.get(e.getKey().componentID), e.getValue());
//            }
//        }
//        // Assign new neighbours
//        b.setBoardNodes(new ArrayList<>(nodeCopies.values()));
//        // Copy properties
//        copyComponentTo(b);
//        return b;
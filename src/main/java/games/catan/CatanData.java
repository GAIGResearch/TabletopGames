package games.catan;

import core.AbstractGameData;
import core.components.*;
import core.properties.PropertyInt;
import core.properties.PropertyString;
import games.catan.CatanConstants.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

        GraphBoard board = generateBoard();




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

        GraphBoard board = new GraphBoard();
        ArrayList<BoardNode> boardNodes = new ArrayList<>();
        for (Map.Entry tileCount : tileCounts.entrySet()){
            // todo create component ids
            for (int i = 0; i < (int)tileCount.getValue(); i++){
                BoardNode bn = new BoardNode(6, tileCount.getKey().toString() + "_" + i);
//                bn.setProperty(Hash.GetInstance().hash("number"), new PropertyInt("a",1));
                bn.setProperty(CatanConstants.typeHash, new PropertyString("type", tileCount.getKey().toString()));
                System.out.println();
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
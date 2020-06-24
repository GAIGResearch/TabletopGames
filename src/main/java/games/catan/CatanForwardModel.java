package games.catan;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.*;
import core.properties.PropertyBoolean;
import core.properties.PropertyString;

import java.util.*;

import static core.CoreConstants.playerHandHash;
import static games.pandemic.PandemicConstants.playerCardHash;

public class CatanForwardModel extends AbstractForwardModel {
    CatanParameters params;
    int nPlayers;

    public CatanForwardModel(){}

    public CatanForwardModel(CatanParameters pp, int nPlayers) {
        this.params = pp;
        this.nPlayers = nPlayers;
    }

    @Override
    protected void _setup(AbstractGameState firstState) {
        // todo set everything to the state
        Random rnd = new Random(firstState.getGameParameters().getGameSeed());

        CatanGameState state = (CatanGameState) firstState;
        CatanParameters params = (CatanParameters)state.getGameParameters();
        CatanData data = state.getData();

        state.areas = new HashMap<>();
        CatanBoard catanBoard = new CatanBoard(params);
        System.out.println("check catanboard");
//        state.board = catanBoard.board;

        // todo distribute everything to player
        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = new Area(i, "Player Area");
            Deck<Card> playerHand = new Deck<>("Player Hand");
            playerHand.setOwnerId(i);
            playerArea.putComponent(playerHandHash, playerHand);
            state.areas.put(i, playerArea);
        }

        // Initialize the game area
        Area gameArea = new Area(-1, "Game Area");
        state.areas.put(-1, gameArea);


    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());

        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return null;
    }

    private GraphBoard generateBoard(){
        // todo steps:
        // 2, distribute all the resource tiles with number tokens
        // 3, distribute sea tiles

        // set up land tiles
        GraphBoard board = new GraphBoard();
        ArrayList<BoardNode> boardNodes = new ArrayList<>();

        // 1, put desert in middle
        BoardNode desert = new BoardNode(6, CatanParameters.TileType.DESERT.name());

        ArrayList<CatanParameters.TileType> tileList = new ArrayList<>();
        for (Map.Entry tileCount : params.tileCounts.entrySet()){
            // todo create component ids
            for (int i = 0; i < (int)tileCount.getValue(); i++) {
                tileList.add((CatanParameters.TileType)tileCount.getKey());
            }
        }
        Collections.shuffle(tileList);

        for (BoardNode bn: boardNodes){
            if (desert.getNeighbours().size() < 6){
                desert.addNeighbour(bn);
            }
        }


//        // first get all the tilecounts and then randomly allocate them
//        for (Map.Entry tileCount : params.tileCounts.entrySet()){
//            // todo create component ids
//            for (int i = 0; i < (int)tileCount.getValue(); i++){
//                BoardNode bn = new BoardNode(6, tileCount.getKey().toString() + "_" + i);
////                bn.setProperty(Hash.GetInstance().hash("number"), new PropertyInt("a",1));
//                bn.setProperty(CatanConstants.typeHash, new PropertyString("type", tileCount.getKey().toString()));
//                bn.setProperty(CatanConstants.robberHash, new PropertyBoolean("robber", false));
//                boardNodes.add(bn);
//            }
//        }



        board.setBoardNodes(boardNodes);




        return board;

    }
}

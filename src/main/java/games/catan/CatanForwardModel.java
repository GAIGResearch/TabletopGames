package games.catan;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.*;
import games.catan.actions.BuildRoad;
import games.catan.actions.BuildSettlement;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.*;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.HEX_SIDES;

public class CatanForwardModel extends AbstractForwardModel {
    private int rollCounter;
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
        Random rnd = new Random(firstState.getGameParameters().getRandomSeed());

        CatanGameState state = (CatanGameState) firstState;
        CatanParameters params = (CatanParameters)state.getGameParameters();
        // data is read in from JSON it has all the cards, tokens and counters
        CatanData data = state.getData();

        state.setBoard(generateBoard(params));
        state.areas = new HashMap<>();

        // Setup areas
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

        state.addComponents();

    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        CatanGameState gs = (CatanGameState) currentState;
        // todo (mb) make sure that reactions are handled correctly (trading)
//        if (((CatanTurnOrder)gs.getTurnOrder()).reactionsFinished()){
//            gs.setMainGamePhase();
//        }
        gs.setRollValue(rollDice(gs.getGameParameters().getRandomSeed()));
        action.execute(gs);

        // end player's turn
        gs.getTurnOrder().endPlayerTurn(gs);

    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState)gameState;
        if (gs.getGamePhase() == CatanGameState.CatanGamePhase.Setup){
            System.out.println("setting settlements with roads");
            // TODO (mb) in initial phase each player places 2 roads and 2 settlements on the board
        }
        if (gs.getGamePhase() == AbstractGameState.DefaultGamePhase.Main){
//            actions.add(new DoNothing());
        }

        // todo (mb) instead of random determine where to build settlement
        Random rnd = new Random();
        int x = rnd.nextInt(7);
        int y = rnd.nextInt(7);
        int vertex = rnd.nextInt(HEX_SIDES);
        actions.add(new BuildSettlement(x, y, vertex, gameState.getCurrentPlayer()));

        // todo (mb) instead of random determine where the player can put roads
        x = rnd.nextInt(7);
        y = rnd.nextInt(7);
        int edge = rnd.nextInt(HEX_SIDES);
//        actions.add(new BuildRoad(x, y, edge, gameState.getCurrentPlayer()));


        // todo (mb) some notes on rules
        // 1, victory cards may only be played when player has 10+ points, can be in the same turn when drawn
        // 2, other dev cards cannot be played on the same turn when they are drawn and only 1 card per turn is playable
        // 3, distance rule - each settlement requires 2 edge distance from other settlements
        // 4, trade is a negotiation in the game - should player send an offer to all other players?
        return actions;
    }

    @Override
    protected AbstractForwardModel _copy() {
        return null;
    }

    private CatanTile[][] generateBoard(CatanParameters params){
        // Shuffle the tile types
        ArrayList<CatanParameters.TileType> tileList = new ArrayList<>();
        for (Map.Entry tileCount : params.tileCounts.entrySet()){
            for (int i = 0; i < (int)tileCount.getValue(); i++) {
                tileList.add((CatanParameters.TileType)tileCount.getKey());
            }
        }
        // Shuffle number tokens
        ArrayList<Integer> numberList = new ArrayList<>();
        for (Map.Entry numberCount : params.numberTokens.entrySet()){
            for (int i = 0; i < (int)numberCount.getValue(); i++) {
                numberList.add((Integer)numberCount.getKey());
            }
        }
        // shuffle collections so we get randomized tiles and tokens on them
        Collections.shuffle(tileList);
        Collections.shuffle(numberList);

        CatanTile[][] board = new CatanTile[7][7];
        int mid_x = board.length/2;
        int mid_y = board[0].length/2;

        CatanTile midTile = new CatanTile(mid_x, mid_y);
//        midTile.setTileType(CatanParameters.TileType.DESERT);

        for (int x = 0; x < board.length; x++){
            for (int y = 0; y < board[x].length; y++){
                CatanTile tile = new CatanTile(x, y);
                // mid_x should be the same as the distance
                if (midTile.distance(tile) >= mid_x){
                    tile.setTileType(CatanParameters.TileType.SEA);
                }
                else if (tileList.size() > 0) {
                    tile.setTileType(tileList.remove(0));
                    // desert has no number and has to place the robber there
                    if (tile.getType().equals(CatanParameters.TileType.DESERT)){
                        tile.placeRobber();
                    }
                    else {
                        tile.setNumber(numberList.remove(0));
                    }

                }
                board[x][y] = tile;
            }
        }

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];

                // --------- Road ------------
                for (int edge = 0; edge < HEX_SIDES; edge++) {
                    // Road has already been set
                    if (tile.getRoads()[edge] != null) {
                        continue;
                    }

                    // set a new road without owner
                    Road road = new Road(-1);
                    tile.setRoad(edge, road);

                    int[] neighbourCoord = CatanTile.get_neighbour_on_edge(tile, edge);
                    // need to check if neighbour is on the board
                    if (Arrays.stream(neighbourCoord).max().getAsInt() < board.length &&
                            Arrays.stream(neighbourCoord).min().getAsInt() >= 0) {
                        // if in range then set road references
                        CatanTile neighbour = board[neighbourCoord[0]][neighbourCoord[1]];
                        neighbour.setRoad((edge + 3) % HEX_SIDES, road);
                    }
                }

                // ------ Settlement ------------
                for (int vertex = 0; vertex < HEX_SIDES; vertex++){
                    // settlement has already been set so skip this loop
                    if (tile.getSettlements()[vertex] != null){
                        continue;
                    }

                    Settlement settlement = new Settlement(-1);
                    tile.setSettlement(vertex, settlement);

                    // Get the other 2 settlements along that vertex and set both of them separately
                    // has to do it in 2 steps as there could cases with only 2 tiles on along a vertex
                    int[][] neighbourCoords = CatanTile.get_neighbours_on_vertex(tile, vertex);
                    if (Arrays.stream(neighbourCoords[0]).max().getAsInt() < board.length &&
                            Arrays.stream(neighbourCoords[0]).min().getAsInt() >= 0) {
                        board[neighbourCoords[0][0]][neighbourCoords[0][1]].setSettlement((vertex + 2) % HEX_SIDES, settlement);
                    }
                    if (Arrays.stream(neighbourCoords[1]).max().getAsInt() < board.length &&
                            Arrays.stream(neighbourCoords[1]).min().getAsInt() >= 0) {
                        board[neighbourCoords[1][0]][neighbourCoords[1][1]].setSettlement((vertex + 4) % HEX_SIDES, settlement);
                    }
                }
            }
        }
        return board;
    }

    public int rollDice(long seed){
        /* Rolls 2 random dices givesn a single random seed */
        Random r1 = new Random(seed + rollCounter);
        rollCounter += 1;
        Random r2 = new Random(seed + rollCounter);
        rollCounter += 1;
        int num1 = r1.nextInt(6);
        int num2 = r2.nextInt(6);

        return num1 + num2 + 2;
    }

}

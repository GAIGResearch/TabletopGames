package games.catan;

import core.AbstractGameState;
import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.*;
import core.interfaces.IGamePhase;
import games.catan.actions.*;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.VERBOSE;
import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.*;

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
        state.setGraph(extractGraphFromBoard(state.getBoard()));
        state.getRoads();
        state.getSettlements();
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
        gameArea.putComponent(resourceDeckHash, data.findDeck("resourceDeck"));
        gameArea.putComponent(developmentDeckHash, data.findDeck("developmentDeck"));

        state.addComponents();
        state.setGamePhase(CatanGameState.CatanGamePhase.Setup);

    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        CatanGameState gs = (CatanGameState) currentState;
        CatanTurnOrder cto = (CatanTurnOrder) gs.getTurnOrder();
        if (action != null){
            action.execute(gs);
        } else {
            if (VERBOSE)
                System.out.println("Player cannot do anything since he has drawn cards or " +
                        " doesn't have any targets available");
        }
        IGamePhase gamePhase = gs.getGamePhase();
        if (gamePhase.equals(CatanGameState.CatanGamePhase.Setup)){
            // As player always places a settlement in the setup phase it is awarded the score for it
            gs.addScore(gs.getCurrentPlayer(), params.settlement_value);
            cto.endPlayerTurn(gs);
            if (cto.getRoundCounter() >= 2){
                // After 2 rounds of setup the main game phase starts
                gs.setMainGamePhase();
            }
        }
        // todo (mb) check to only execute one of each types of actions
        if (gamePhase.equals(AbstractGameState.DefaultGamePhase.Main)){
            if (action instanceof BuildRoad){
                BuildRoad br = (BuildRoad)action;
                // todo remove branches and cycles from road length
                int new_length = gs.getRoadDistance(br.getX(), br.getY(), br.getEdge());
                if (new_length > gs.longestRoadLength){
                    gs.longestRoadLength = new_length;
                    // add points for longest road and set the new road in gamestate
                    if (gs.longestRoad >= 0) {
                        // in this case the longest road was not claimed yet
                        gs.addScore(gs.longestRoad, -params.longest_road_value);
                    }
                    gs.addScore(gs.getCurrentPlayer(), params.longest_road_value);
                    gs.longestRoad = gs.getCurrentPlayer();
                    if (VERBOSE){
                        System.out.println("Player " + gs.getCurrentPlayer() + " has the longest road with length " + gs.longestRoad);
                    }
                }
                if (VERBOSE) {
                    System.out.println("Calculated road length: " + new_length);
                }
            } else if (action instanceof BuildSettlement){
                gs.addScore(gs.getCurrentPlayer(), params.settlement_value);
            } else if (action instanceof BuildCity) {
                gs.addScore(gs.getCurrentPlayer(), params.city_value);
            } else if (action instanceof PlayDevelopmentCard){
                // todo only cards with victory point or knight card if gets the largest army
                gs.addScore(gs.getCurrentPlayer(), params.victory_point_value);
            }

            // win condition
            if (gs.getScore(gs.getCurrentPlayer()) >= params.points_to_win){
                gs.setGameStatus(Utils.GameResult.GAME_END);
                System.out.println("Game over! winner = " + gs.getCurrentPlayer());
            }

            // end player's turn; roll dice and allocate resources
            gs.getTurnOrder().endPlayerTurn(gs);
            rollDiceAndallocateResources(gs);
        }


    }

    private void rollDiceAndallocateResources(CatanGameState gs){
        /* Gives players the resources depending on the current rollValue stored in the game state */
        // roll dice
        gs.setRollValue(rollDice(gs.getGameParameters().getRandomSeed()));
        if (VERBOSE) {
            System.out.println("New role value = " + gs.rollValue);
        }

        int value = gs.getRollValue();
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                if (tile.getNumber() == value){
                    // allocate resource for each settlement/city
                    for (Settlement settl: tile.getSettlements()) {
                        if (settl.getOwner() != -1) {
                            // Move the card from the resource deck and give it to the player
                            List<Card> resourceDeck = ((Deck<Card>)gs.getComponent(resourceDeckHash)).getComponents();
                            int counter = 0;
                            for (int i = 0; i < resourceDeck.size(); i++){
                                Card card = resourceDeck.get(i);
                                if (card.getProperty(cardType).toString().equals(CatanParameters.Resources.values()[CatanParameters.productMapping.get(tile.getType()).ordinal()].toString())){
                                    // remove from deck and give it to player
                                    System.out.println("With Roll value " + gs.rollValue + " Player" + settl.getOwner() + " got " + card.getProperty(cardType));
                                    ((Deck<Card>)gs.getComponent(resourceDeckHash)).remove(card);
                                    ((Deck) gs.getComponent(playerHandHash, settl.getOwner())).add(card);
//                                    ((Deck) gs.getComponentActingPlayer(playerHandHash)).add(card);
                                    counter++;
                                }
                                // getType is 1 for settlement; 2 for city
                                if (counter >= settl.getType()){
                                    break;
                                }
                            }

                        }
                    }
                }
            }
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        // todo (mb) some notes on rules
        // 1, victory cards may only be played when player has 10+ points, can be in the same turn when drawn
        // 2, other dev cards cannot be played on the same turn when they are drawn and only 1 card per turn is playable
        // 3, distance rule - each settlement requires 2 edge distance from other settlements
        // 4, trade is a negotiation in the game - should player send an offer to all other players?

        ArrayList<AbstractAction> actions = new ArrayList<>();
        CatanGameState gs = (CatanGameState)gameState;

        if (gs.getGamePhase() == CatanGameState.CatanGamePhase.Setup){
            return CatanActionFactory.getSetupActions(gs);
        }
        else if (gs.getGamePhase() == AbstractGameState.DefaultGamePhase.Main){
            return CatanActionFactory.getPlayerActions(gs);
        }

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
                    if (tile.getRoads()[edge] == null) {
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

                }

                // ------ Settlement ------------
                for (int vertex = 0; vertex < HEX_SIDES; vertex++){
                    // settlement has already been set so skip this loop
                    if (tile.getSettlements()[vertex] == null){
                        Settlement settlement = new Settlement(-1);
                        tile.setSettlement(vertex, settlement);

                        // Get the other 2 settlements along that vertex and set both of them separately
                        // has to do it in 2 steps as there could cases with only 2 tiles on along a vertex
                        int[][] neighbourCoords = CatanTile.get_neighbours_on_vertex(tile, vertex);
                        // check neighbour #1
                        if (Arrays.stream(neighbourCoords[0]).max().getAsInt() < board.length &&
                                Arrays.stream(neighbourCoords[0]).min().getAsInt() >= 0) {
                            board[neighbourCoords[0][0]][neighbourCoords[0][1]].setSettlement((vertex + 2) % HEX_SIDES, settlement);
                        }
                        // check neighbour #2
                        if (Arrays.stream(neighbourCoords[1]).max().getAsInt() < board.length &&
                                Arrays.stream(neighbourCoords[1]).min().getAsInt() >= 0) {
                            board[neighbourCoords[1][0]][neighbourCoords[1][1]].setSettlement((vertex + 4) % HEX_SIDES, settlement);
                        }
                    }
                }
            }
        }
        return board;
    }

    private Graph extractGraphFromBoard(CatanTile[][] board){
        Graph<Settlement, Road> graph = new Graph<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                // logic to generate the graph from the board representation
                // We are not interested in references to DESERT or SEA tiles
                if (!(tile.getType() == CatanParameters.TileType.DESERT || tile.getType() == CatanParameters.TileType.SEA)){
                    Settlement[] settlements = tile.getSettlements();
                    Road[] roads = tile.getRoads();
                    for (int i = 0; i < settlements.length; i++){
                        //  2 roads are along the same HEX
                        graph.addEdge(tile.settlements[i], tile.settlements[(i+5)%HEX_SIDES], roads[(i+5)%HEX_SIDES]);
                        graph.addEdge(tile.settlements[i], tile.settlements[(i+1)%HEX_SIDES], roads[i]);

                        // last one requires a road and a settlement from a neighbour
                        int[] otherCoords = CatanTile.get_neighbour_on_edge(tile, i);
                        if (Arrays.stream(otherCoords).max().getAsInt() < board.length &&
                                Arrays.stream(otherCoords).min().getAsInt() >= 0) {
                            CatanTile neighbour = board[otherCoords[0]][otherCoords[1]];
                            Road[] neighbour_roads = neighbour.getRoads();
                            // todo the rule below is not general
                            graph.addEdge(tile.settlements[i], neighbour.settlements[(i+5)%HEX_SIDES], neighbour_roads[(i+4)%HEX_SIDES]);
                        }
                    }
                }
            }
        }
        return graph;

    }

    public int rollDice(long seed){
        /* Rolls 2 random dices given a single random seed */
        Random r1 = new Random(seed + rollCounter);
        rollCounter += 1;
        Random r2 = new Random(seed + rollCounter);
        rollCounter += 1;
        int num1 = r1.nextInt(6);
        int num2 = r2.nextInt(6);

        return num1 + num2 + 2;
    }

}

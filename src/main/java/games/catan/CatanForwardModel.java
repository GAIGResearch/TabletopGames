package games.catan;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Area;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import core.properties.PropertyString;
import games.catan.actions.*;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;
import utilities.Utils;

import java.util.*;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.*;
import static games.catan.CatanGameState.CatanGamePhase.*;

public class CatanForwardModel extends AbstractForwardModel {
    CatanParameters params;
    int nPlayers;

    public CatanForwardModel() {
    }

    public CatanForwardModel(CatanParameters pp, int nPlayers) {
        this.params = pp;
        this.nPlayers = nPlayers;
    }

    @Override
    protected void _setup(AbstractGameState firstState) {

        CatanGameState state = (CatanGameState) firstState;
        params = (CatanParameters) state.getGameParameters();

        state.setBoard(generateBoard(params));
        state.setGraph(extractGraphFromBoard(state.getBoard()));
        state.areas = new HashMap<>();

        // Setup areas
        for (int i = 0; i < state.getNPlayers(); i++) {
            Area playerArea = new Area(i, "Player Area");
            Deck<Card> playerHand = new Deck<>("Player Resource Deck", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            playerHand.setOwnerId(i);
            playerArea.putComponent(playerHandHash, playerHand);

            Deck<Card> playerDevDeck = new Deck<>("Player Development Deck", CoreConstants.VisibilityMode.VISIBLE_TO_OWNER);
            playerDevDeck.setOwnerId(i);
            playerArea.putComponent(developmentDeckHash, playerDevDeck);

            // counter value, min, max, name
            playerArea.putComponent(roadCounterHash, new Counter(0, 0, params.n_roads, "Road Counter"));
            playerArea.putComponent(settlementCounterHash, new Counter(0, 0, params.n_settlements, "Settlement Counter"));
            playerArea.putComponent(cityCounterHash, new Counter(0, 0, params.n_cities, "City Counter"));

            state.areas.put(i, playerArea);

        }

        // Initialize the game area
        Area gameArea = new Area(-1, "Game Area");
        state.areas.put(-1, gameArea);

        // create resource deck
        Deck<Card> resourceDeck = new Deck("resourceDeck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL);
        for (CatanParameters.Resources res : CatanParameters.Resources.values()) {
            for (int i = 0; i < params.n_resource_cards; i++) {
                Card c = new Card();
                c.setProperty(new PropertyString("cardType", res.name()));
                resourceDeck.add(c);
            }
        }

        // create and shuffle developmentDeck
        Deck<Card> developmentDeck = new Deck("developmentDeck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (Map.Entry<CatanParameters.CardTypes, Integer> entry : params.developmentCardCount.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                Card card = new Card();
                card.setProperty(new PropertyString("cardType", entry.getKey().toString()));
                developmentDeck.add(card);
            }
        }
        developmentDeck.shuffle(state.rnd);

        gameArea.putComponent(resourceDeckHash, resourceDeck);
        gameArea.putComponent(developmentDeckHash, developmentDeck);
        gameArea.putComponent(developmentDiscardDeck, new Deck("DevelopmentDiscardDeck", CoreConstants.VisibilityMode.VISIBLE_TO_ALL));

        state.addComponents();
        state.setGamePhase(Setup);

    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        CatanGameState gs = (CatanGameState) currentState;
        CatanTurnOrder cto = (CatanTurnOrder) gs.getTurnOrder();
        if (action != null) {
            action.execute(gs);
        } else {
            throw new AssertionError("No action selected by current player");
        }
        // handle scoring
        if (action instanceof BuildRoad) {
            BuildRoad br = (BuildRoad) action;
            int new_length = gs.getRoadDistance(br.getX(), br.getY(), br.getEdge());
            if (new_length > gs.longestRoadLength) {
                gs.longestRoadLength = new_length;
                // add points for longest road and set the new road in gamestate
                if (gs.longestRoad >= 0) {
                    // in this case the longest road was not claimed yet
                    gs.addScore(gs.longestRoad, -params.longest_road_value);
                }
                gs.addScore(gs.getCurrentPlayer(), params.longest_road_value);
                gs.longestRoad = gs.getCurrentPlayer();
                if (gs.getCoreGameParameters().verbose) {
                    System.out.println("Player " + gs.getCurrentPlayer() + " has the longest road with length " + gs.longestRoad);
                }
            }
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("Calculated road length: " + new_length);
            }
        } else if (action instanceof PlaceSettlementWithRoad) {
            // As player always places a settlement in the setup phase it is awarded the score for it
            gs.addScore(gs.getCurrentPlayer(), params.settlement_value);
        } else if (action instanceof BuildSettlement) {
            gs.addScore(gs.getCurrentPlayer(), params.settlement_value);
        } else if (action instanceof BuildCity) {
            gs.addScore(gs.getCurrentPlayer(), -params.settlement_value);
            gs.addScore(gs.getCurrentPlayer(), params.city_value);
        }

        // win condition
        if (gs.getGameScore(gs.getCurrentPlayer()) + gs.getVictoryPoints()[gs.getCurrentPlayer()] >= params.points_to_win) {
            for (int i = 0; i < gs.getNPlayers(); i++) {
                if (i == gs.getCurrentPlayer()) {
                    gs.setPlayerResult(Utils.GameResult.WIN, i);
                } else {
                    gs.setPlayerResult(Utils.GameResult.LOSE, i);
                }
            }
            gs.setGameStatus(Utils.GameResult.GAME_END);
            if (gs.getCoreGameParameters().verbose) {
                System.out.println("Game over! winner = " + gs.getCurrentPlayer());
            }
        } else if (params.max_round_count != -1 && gs.getTurnOrder().getRoundCounter() > params.max_round_count) { // end in tie if round limit exceeded
            for (int i = 0; i < gs.getNPlayers(); i++) {
                gs.setPlayerResult(Utils.GameResult.DRAW, i);
            }
            gs.setGameStatus(Utils.GameResult.GAME_END);
        }

        // prevents multiple DoNothing actions with multi-action turn stages
        if (action instanceof DoNothing && (gs.getGamePhase() == Build || gs.getGamePhase() == Trade)) {
            cto.skipTurnStage(gs);
        }

        if (action instanceof OfferPlayerTrade) {
            cto.endReaction(gs);  // remove previous player to act...this is safe if called (the first time) with no reactive player
            cto.addReactivePlayer(((OfferPlayerTrade) action).otherPlayerID);  // add new one
            // We do not consider the end of a turn until the back-and-forth of negotiation finishes
        } else if (action instanceof PlayKnightCard) {
            // continue...we skip endTurn as we now need to execute the ensuing Robber phase
        } else {
            // end player's turn; roll dice and allocate resources
            cto.endTurnStage(gs);
        }

        if (gs.getGamePhase() == AbstractGameState.DefaultGamePhase.Main) {
            // reset recently bought dev card to null
            gs.boughtDevCard = null;
            rollDiceAndAllocateResources(gs);
        }

    }

    private void rollDiceAndAllocateResources(CatanGameState gs) {
        /* Gives players the resources depending on the current rollValue stored in the game state */
        // roll dice
        gs.rollDice();

        int value = gs.getRollValue();
        CatanTurnOrder cto = (CatanTurnOrder) gs.getTurnOrder();
        cto.logEvent(() -> "Dice roll of " + value, gs);
        CatanTile[][] board = gs.getBoard();
        if (value == 7) {
            // Dice roll was 7 so we change the phase
            cto.setGamePhase(Robber, gs);
        } else {
            cto.setGamePhase(Trade, gs);
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    if (tile.getNumber() == value && !tile.hasRobber()) {
                        // allocate resource for each settlement/city
                        for (Settlement settl : tile.getSettlements()) {
                            if (settl.getOwner() != -1) {
                                // Move the card from the resource deck and give it to the player
                                List<Card> resourceDeck = ((Deck<Card>) gs.getComponent(resourceDeckHash)).getComponents();
                                int counter = 0;
                                for (int i = 0; i < resourceDeck.size(); i++) {
                                    Card card = resourceDeck.get(i);
                                    if (card.getProperty(cardType).toString().equals(CatanParameters.Resources.values()[CatanParameters.productMapping.get(tile.getType()).ordinal()].toString())) {
                                        // remove from deck and give it to player
                                        if (gs.getCoreGameParameters().verbose) {
                                            System.out.println("With Roll value " + gs.rollValue + " Player " + settl.getOwner() + " got " + card.getProperty(cardType));
                                        }
                                        cto.logEvent(() -> " Player " + settl.getOwner() + " got " + card.getProperty(cardType), gs);
                                        ((Deck<Card>) gs.getComponent(resourceDeckHash)).remove(card);
                                        ((Deck) gs.getComponent(playerHandHash, settl.getOwner())).add(card);
                                        counter++;
                                    }
                                    // getType is 1 for settlement; 2 for city
                                    if (counter >= settl.getType()) {
                                        break;
                                    }
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
        CatanGameState cgs = (CatanGameState) gameState;
        if (cgs.getGamePhase() == Setup) {
            return CatanActionFactory.getSetupActions(cgs);
        }
        if (cgs.getGamePhase() == Robber) {
            return CatanActionFactory.getRobberActions(cgs);
        }
        if (cgs.getGamePhase() == Steal) {
            return CatanActionFactory.getStealActions(cgs);
        }
        if (cgs.getGamePhase() == Discard) {
            return CatanActionFactory.getDiscardActions(cgs);
        }
        if (cgs.getGamePhase() == Trade) {
            if (cgs.getCurrentTradeOffer() != null)
                return CatanActionFactory.getTradeReactionActions(cgs);
            return CatanActionFactory.getTradeStageActions(cgs);
        }
        if (cgs.getGamePhase() == Build) {
            return CatanActionFactory.getBuildStageActions(cgs);
        }
        throw new AssertionError("GamePhase is not in the defined set of options");
    }

    @Override
    protected AbstractForwardModel _copy() {
        CatanForwardModel copy = new CatanForwardModel(params, nPlayers);
        return copy;
    }

    private CatanTile[][] generateBoard(CatanParameters params) {
        // Shuffle the tile types
        ArrayList<CatanParameters.TileType> tileList = new ArrayList<>();
        for (Map.Entry tileCount : params.tileCounts.entrySet()) {
            for (int i = 0; i < (int) tileCount.getValue(); i++) {
                tileList.add((CatanParameters.TileType) tileCount.getKey());
            }
        }
        // Shuffle number tokens
        ArrayList<Integer> numberList = new ArrayList<>();
        for (Map.Entry numberCount : params.numberTokens.entrySet()) {
            for (int i = 0; i < (int) numberCount.getValue(); i++) {
                numberList.add((Integer) numberCount.getKey());
            }
        }
        // shuffle collections so we get randomized tiles and tokens on them
        Collections.shuffle(tileList);
        Collections.shuffle(numberList);

        CatanTile[][] board = new CatanTile[7][7];
        int midX = board.length / 2;
        int midY = board[0].length / 2;

        CatanTile midTile = new CatanTile(midX, midY);

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = new CatanTile(x, y);
                // mid_x should be the same as the distance
                if (midTile.getDistanceToTile(tile) >= midX) {
                    tile.setTileType(CatanParameters.TileType.SEA);
                } else if (tileList.size() > 0) {
                    tile.setTileType(tileList.remove(0));
                    // desert has no number and has to place the robber there
                    if (tile.getType().equals(CatanParameters.TileType.DESERT)) {
                        tile.placeRobber();
                    } else {
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

                        int[] neighbourCoord = CatanTile.getNeighbourOnEdge(tile, edge);
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
                for (int vertex = 0; vertex < HEX_SIDES; vertex++) {
                    // settlement has already been set so skip this loop
                    if (tile.getSettlements()[vertex] == null) {
                        Settlement settlement = new Settlement(-1);
                        tile.setSettlement(vertex, settlement);

                        // Get the other 2 settlements along that vertex and set both of them separately
                        // has to do it in 2 steps as there could cases with only 2 tiles on along a vertex
                        int[][] neighbourCoords = CatanTile.getNeighboursOnVertex(tile, vertex);
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
        // Finally set Harbors types
        setHarbors(board);
        return board;
    }

    private Graph extractGraphFromBoard(CatanTile[][] board) {
        Graph<Settlement, Road> graph = new Graph<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                // logic to generate the graph from the board representation
                // We are not interested in references to DESERT or SEA tiles
                if (!(tile.getType() == CatanParameters.TileType.DESERT || tile.getType() == CatanParameters.TileType.SEA)) {
                    Settlement[] settlements = tile.getSettlements();
                    Road[] roads = tile.getRoads();
                    for (int i = 0; i < settlements.length; i++) {
                        //  2 roads are along the same HEX
                        graph.addEdge(tile.settlements[i], tile.settlements[(i + 5) % HEX_SIDES], roads[(i + 5) % HEX_SIDES]);
                        graph.addEdge(tile.settlements[i], tile.settlements[(i + 1) % HEX_SIDES], roads[i]);

                        // last one requires a road and a settlement from a neighbour
                        int[] otherCoords = CatanTile.getNeighbourOnEdge(tile, i);
                        if (Arrays.stream(otherCoords).max().getAsInt() < board.length &&
                                Arrays.stream(otherCoords).min().getAsInt() >= 0) {
                            CatanTile neighbour = board[otherCoords[0]][otherCoords[1]];
                            Road[] neighbourRoads = neighbour.getRoads();
                            graph.addEdge(tile.settlements[i], neighbour.settlements[(i + 5) % HEX_SIDES], neighbourRoads[(i + 4) % HEX_SIDES]);
                        }
                    }
                }
            }
        }
        return graph;
    }

    private void setHarbors(CatanTile[][] board) {
        // set harbors along the tiles where the SEA borders the land
        ArrayList<Integer> harbors = new ArrayList<>();
        for (Map.Entry<CatanParameters.HarborTypes, Integer> entry : CatanParameters.harborCount.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++)
                harbors.add(CatanParameters.HarborTypes.valueOf(entry.getKey().toString()).ordinal());
        }
        Collections.shuffle(harbors);

        int radius = board.length / 2;
        // todo edge 4 can work, but random would be better, the math changes with different directions.
        //Random random = new Random(params.getRandomSeed());
        //int edge = random.nextInt(HEX_SIDES);
        int edge = 4;
        // Get mid tile
        CatanTile tile = board[radius][radius];
        // move along a certain edge to reach SEA tiles
        for (int i = 0; i < radius; i++) {
            int[] tileLocation = CatanTile.getNeighbourOnEdge(tile, edge);
            tile = board[tileLocation[0]][tileLocation[1]];
        }
        // go around in a circle
        int counter = 0;
        for (int i = 0; i < HEX_SIDES; i++) {
            for (int j = 0; j < board.length / 2; j++) {
                int[] tileLocation = CatanTile.getNeighbourOnEdge(tile, i);
                tile = board[tileLocation[0]][tileLocation[1]];
                if (counter % 2 == 0 && harbors.size() > 0) {
                    tile.addHarbor((i + 2) % HEX_SIDES, harbors.remove(0));
                }
                counter++;
            }
        }
    }
}

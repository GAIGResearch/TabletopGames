package games.catan;

import core.AbstractGameState;
import core.CoreConstants;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.components.Counter;
import core.components.Deck;
import core.components.Edge;
import core.components.GraphBoardWithEdges;
import evaluation.metrics.Event;
import games.catan.actions.build.BuyAction;
import games.catan.actions.discard.DiscardResourcesPhase;
import games.catan.actions.trade.OfferPlayerTrade;
import games.catan.components.Building;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;
import games.catan.stats.CatanMetrics;

import java.util.*;

import static core.CoreConstants.DefaultGamePhase.Main;
import static games.catan.CatanConstants.HEX_SIDES;
import static games.catan.CatanGameState.CatanGamePhase.Robber;
import static games.catan.CatanGameState.CatanGamePhase.Setup;
import static games.catan.stats.CatanMetrics.CatanEvent.RobberRoll;
import static games.catan.stats.CatanMetrics.CatanEvent.SevenOut;

@SuppressWarnings("OptionalGetWithoutIsPresent")
public class CatanForwardModel extends StandardForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {

        CatanGameState state = (CatanGameState) firstState;
        CatanParameters params = (CatanParameters) state.getGameParameters();

        state.setBoard(generateBoard(params, state.getRnd()));
        state.setGraph(extractGraphFromBoard(state.getBoard(), params, state.getRnd()));

        state.scores = new int[state.getNPlayers()];
        state.victoryPoints = new int[state.getNPlayers()];
        state.knights = new int[state.getNPlayers()];
        state.roadLengths = new int[state.getNPlayers()];
        state.largestArmyOwner = -1;
        state.longestRoadOwner = -1;
        state.largestArmySize = 0;
        state.longestRoadLength = 0;
        state.rollValue = -1;
        state.developmentCardPlayed = false;

        state.tradeOffer = null;
        state.negotiationStepsCount = 0;
        state.nTradesThisTurn = 0;

        state.exchangeRates = new ArrayList<>();
        state.playerDevCards = new ArrayList<>();
        state.playerTokens = new ArrayList<>();
        state.playerResources = new ArrayList<>();

        // Setup areas
        for (int i = 0; i < state.getNPlayers(); i++) {
            state.playerDevCards.add(new Deck<>("Player Development Deck", i, CoreConstants.VisibilityMode.VISIBLE_TO_OWNER));
            HashMap<BuyAction.BuyType, Counter> tokens = new HashMap<>();
            for (Map.Entry<BuyAction.BuyType, Integer> type: params.tokenCounts.entrySet()) {
                tokens.put(type.getKey(), new Counter(type.getValue(), type.getKey().name() + " Counter " + i));
            }
            state.playerTokens.add(tokens);

            HashMap<CatanParameters.Resource, Counter> resources = new HashMap<>();
            HashMap<CatanParameters.Resource, Counter> exchange = new HashMap<>();
            for (CatanParameters.Resource res: CatanParameters.Resource.values()) {
                resources.put(res, new Counter(res + " " + i));
                exchange.put(res, new Counter(params.default_exchange_rate,1, params.default_exchange_rate,res + " " + i));
            }
            state.playerResources.add(resources);
            state.exchangeRates.add(exchange);

        }

        // create resource pool
        state.resourcePool = new HashMap<>();
        for (CatanParameters.Resource res : CatanParameters.Resource.values()) {
            state.resourcePool.put(res, new Counter(res.name()));
            state.resourcePool.get(res).increment(params.n_resource_cards);
        }

        // create dice rnd (if we have a separate seed for it)
        state.diceRnd = params.diceSeed > -1 ? new Random(params.diceSeed) : state.getRnd();

        // create and shuffle developmentDeck
        state.devCards = new Deck<>("Development Deck", CoreConstants.VisibilityMode.HIDDEN_TO_ALL);
        for (Map.Entry<CatanCard.CardType, Integer> entry : params.developmentCardCount.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++) {
                CatanCard card = new CatanCard(entry.getKey());
                state.devCards.add(card);
            }
        }
        state.devCards.shuffle(state.getRnd());
        state.setGamePhase(Setup);
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        if (currentState.isActionInProgress()) return;

        CatanGameState gs = (CatanGameState) currentState;
        CatanParameters params = (CatanParameters) gs.getGameParameters();
        int player = gs.getCurrentPlayer();

        if (gs.getGamePhase() == Setup) {
            if (gs.getRoundCounter() == 0) {
                if (gs.getTurnCounter() > 0 && gs.getTurnCounter() % (gs.getNPlayers()-1) == 0) {
                    endRound(gs, gs.getCurrentPlayer());
                } else {
                    endCatanPlayerTurn(gs);
                }
            } else {
                if (gs.getTurnCounter() > 0 && gs.getTurnCounter() % (gs.getNPlayers()-1) == 0) {
                    // Finished setup
                    gs.logEvent(CatanMetrics.CatanEvent.SetupComplete);

                    endRound(gs, 0);
                    gs.setGamePhase(Main);
                    rollDiceAndAllocateResources(gs, params);
                } else {
                    int nextPlayer = gs.getCurrentPlayer()-1;
                    if (nextPlayer < 0) nextPlayer = 0;
                    endPlayerTurn(gs, nextPlayer);
                }
            }
        }

        else if (gs.getGamePhase() == Main) {

            // Win condition
            if (gs.getGameScore(player) + gs.getVictoryPoints()[player] >= params.points_to_win) {
                endGame(currentState);
                if (gs.getCoreGameParameters().verbose) {
                    System.out.println("Game over! winner = " + player);
                }
                return;
            }

            if (action instanceof DoNothing) {
                // end player's turn; roll dice and allocate resources
                if (gs.getTurnCounter() > 0 && gs.getTurnCounter() % (gs.getNPlayers()-1) == 0) {
                    endRound(gs, 0);
                } else {
                    endCatanPlayerTurn(gs);
                }
                rollDiceAndAllocateResources(gs, params);
            } else if (gs.tradeOffer != null) {
                OfferPlayerTrade opt = (OfferPlayerTrade) gs.tradeOffer;
                gs.negotiationStepsCount++;

                // Check if this should be rejected automatically
                if (gs.negotiationStepsCount >= params.max_negotiation_count) {
                    if (gs.getCoreGameParameters().verbose) {
                        System.out.println("Trade rejected by default (too many attempts)");
                    }
                    gs.negotiationStepsCount = 0;
                    gs.tradeOffer = null;
                    gs.setTurnOwner(opt.offeringPlayerID);
                    gs.nTradesThisTurn++;
                } else {

                    // Trade offer reply needed, swap player between offering and other in action
                    switch (opt.stage) {
                        case Offer:
                            gs.setTurnOwner(opt.otherPlayerID);
                        case CounterOffer:
                            gs.setTurnOwner(opt.offeringPlayerID);
                    }
                }
            }
        }
    }

    public void endCatanPlayerTurn(CatanGameState gs) {
        super.endPlayerTurn(gs);
        gs.nTradesThisTurn = 0;
    }

    void rollDiceAndAllocateResources(CatanGameState gs, CatanParameters cp) {
        /* Gives players the resources depending on the current rollValue stored in the game state */

        /* Rolls 2 random dice given a single random seed */
        int n = cp.dieType.nSides;
        int nDice = cp.nDice;
        int rollValue = 0;
        for (int i = 0; i < nDice; i++) {
            rollValue += gs.diceRnd.nextInt(n) + 1;
        }
        gs.setRollValue(rollValue);

        int roll = rollValue;
        gs.logEvent(Event.GameEvent.GAME_EVENT, () -> "Dice roll of " + roll);
        if (gs.getCoreGameParameters().verbose) {
            System.out.println("Dice roll: " + gs.rollValue);
        }
        CatanTile[][] board = gs.getBoard();
        if (rollValue == cp.robber_die_roll) {
            // Dice roll was 7, so we change the phase
            // Check if anyone needs to discard cards
            for (int p = 0; p < gs.getNPlayers(); p++) {
                int nResInHand = gs.getNResourcesInHand(p);
                if (nResInHand > cp.max_cards_without_discard) {
                    gs.logEvent(SevenOut, String.valueOf(p));
                    int r = (int)(nResInHand * cp.perc_discard_robber); // remove half of the resources
                    new DiscardResourcesPhase(p, r).execute(gs);
                }
            }
            gs.logEvent(RobberRoll);
            gs.setGamePhase(Robber);
        } else {
            for (CatanTile[] catanTiles : board) {
                for (CatanTile tile : catanTiles) {
                    if (tile.getNumber() == rollValue && !tile.hasRobber()) {
                        // Allocate resource for each settlement/city on this tile to their owner
                        for (Building settl : gs.getBuildings(tile)) {
                            int who = settl.getOwnerId();
                            if (who != -1) {
                                // Move the card from the resource deck and give it to the player
                                CatanParameters.Resource res = cp.productMapping.get(tile.getTileType());
                                int nGenerated = cp.nProduction.get(settl.getBuildingType());
                                gs.resourcePool.get(res).decrement(nGenerated);
                                gs.playerResources.get(who).get(res).increment(nGenerated);
                                if (gs.getCoreGameParameters().verbose) {
                                    System.out.println("p" + who + " gets " + res);
                                }
                                gs.logEvent(Event.GameEvent.GAME_EVENT, () -> "Player " + who + " gets " + res);
                            }
                        }
                    }
                }
            }
        }
    }

    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        return _computeAvailableActions(gameState, ActionSpace.Default);
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState, ActionSpace actionSpace) {
        CatanGameState cgs = (CatanGameState) gameState;
        CatanParameters cp = (CatanParameters) gameState.getGameParameters();
        int player = cgs.getCurrentPlayer();

        if (cgs.getGamePhase() == Setup) {
            return CatanActionFactory.getSetupActions(cgs, actionSpace, player);
        }
        if (cgs.getGamePhase() == Robber) {
            return CatanActionFactory.getRobberActions(cgs, actionSpace, player, false);
        }
        // Main phase: trade, build (road, city, dev card), or play dev card
        List<AbstractAction> mainActions = new ArrayList<>();

        if (cgs.tradeOffer != null) {
            // Only replies allowed
            mainActions.addAll(CatanActionFactory.getPlayerTradeActions(cgs, actionSpace, player));

        } else {

            // Trade With the bank / ports
            mainActions.addAll(CatanActionFactory.getDefaultTradeActions(cgs, actionSpace, player));

            // Trade With other players, unless already too many trades this turn
            if (cp.tradingAllowed && cgs.nTradesThisTurn < cp.max_trade_actions_allowed) {
                mainActions.addAll(CatanActionFactory.getPlayerTradeActions(cgs, actionSpace, player));
            }

            // Build
            mainActions.addAll(CatanActionFactory.getBuyActions(cgs, actionSpace, player));

            // Dev cards
            if (cgs.noDevelopmentCardPlayed()) {
                mainActions.addAll(CatanActionFactory.getDevCardActions(cgs, actionSpace, player));
            }

            mainActions.add(new DoNothing());  // End turn
        }

        return mainActions;
    }

    private CatanTile[][] generateBoard(CatanParameters params, Random rnd) {
        // Shuffle the tile types
        ArrayList<CatanTile.TileType> tileList = new ArrayList<>();
        for (Map.Entry<CatanTile.TileType, Integer> tileCount : params.tileCounts.entrySet()) {
            for (int i = 0; i < tileCount.getValue(); i++) {
                tileList.add(tileCount.getKey());
            }
        }
        // Shuffle number tokens
        ArrayList<Integer> numberList = new ArrayList<>();
        for (Map.Entry<Integer, Integer> numberCount : params.numberTokens.entrySet()) {
            for (int i = 0; i < numberCount.getValue(); i++) {
                numberList.add(numberCount.getKey());
            }
        }
        // shuffle collections, so we get randomized tiles and tokens on them
        Random toUse = params.hexShuffleSeed > -1 ? new Random(params.hexShuffleSeed) : rnd;
        Collections.shuffle(tileList, toUse);
        Collections.shuffle(numberList, toUse);

        CatanTile[][] board = new CatanTile[params.n_tiles_per_row][params.n_tiles_per_row];
        int midX = board.length / 2;
        int midY = board[0].length / 2;

        CatanTile midTile = new CatanTile(midX, midY);

        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = new CatanTile(x, y);
                // mid_x should be the same as the distance
                if (midTile.getDistanceToTile(tile) >= midX) {
                    tile.setTileType(CatanTile.TileType.SEA);
                } else if (tileList.size() > 0) {
                    tile.setTileType(tileList.remove(0));
                    // desert has no number and has to place the robber there
                    if (tile.getTileType().equals(CatanTile.TileType.DESERT)) {
                        tile.placeRobber();
                    } else {
                        tile.setNumber(numberList.remove(0));
                    }

                }
                board[x][y] = tile;
            }
        }
        return board;
    }

    private GraphBoardWithEdges extractGraphFromBoard(CatanTile[][] board, CatanParameters cp, Random rnd) {
        GraphBoardWithEdges graph = new GraphBoardWithEdges();

        // Create vertices and add references in tiles
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                for (int vertex = 0; vertex < HEX_SIDES; vertex++) {
                    // settlement has already been set so skip this loop
                    if (tile.getVerticesBoardNodeIDs()[vertex] == -1) {
                        Building building = new Building();
                        graph.addBoardNode(building);
                        tile.setVertexBoardNodeID(vertex, building.getComponentID());

                        // Get the other 2 settlements along that vertex and set both of them separately
                        // has to do it in 2 steps as there could be cases with only 2 tiles on along a vertex
                        int[][] neighbourCoords = tile.getNeighboursOnVertex(vertex);
                        // check neighbour #1
                        if (Arrays.stream(neighbourCoords[0]).max().getAsInt() < board.length &&
                                Arrays.stream(neighbourCoords[0]).min().getAsInt() >= 0) {
                            board[neighbourCoords[0][0]][neighbourCoords[0][1]].setVertexBoardNodeID((vertex + 2) % HEX_SIDES, building.getComponentID());
                        }
                        // check neighbour #2
                        if (Arrays.stream(neighbourCoords[1]).max().getAsInt() < board.length &&
                                Arrays.stream(neighbourCoords[1]).min().getAsInt() >= 0) {
                            board[neighbourCoords[1][0]][neighbourCoords[1][1]].setVertexBoardNodeID((vertex + 4) % HEX_SIDES, building.getComponentID());
                        }
                    }
                }
            }
        }

        // Create connections between vertices (edges/roads) and add references in tiles
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                // logic to generate the graph from the board representation
                // We are not interested in references to DESERT or SEA tiles
                if (!(tile.getTileType() == CatanTile.TileType.DESERT || tile.getTileType() == CatanTile.TileType.SEA)) {
                    for (int i = 0; i < HEX_SIDES; i++) {
                        Edge edge = graph.addConnection(tile.getVerticesBoardNodeIDs()[i], tile.getVerticesBoardNodeIDs()[(i + 1) % HEX_SIDES]);
                        tile.setEdgeID(i, edge.getComponentID());

                        // last one requires a road and a settlement from a neighbour
                        int[] otherCoords = tile.getNeighbourOnEdge(i);
                        if (Arrays.stream(otherCoords).max().getAsInt() < board.length &&
                                Arrays.stream(otherCoords).min().getAsInt() >= 0) {
                            CatanTile neighbour = board[otherCoords[0]][otherCoords[1]];
                            neighbour.setEdgeID((i+3) % HEX_SIDES, edge.getComponentID());
                        }
                    }
                }
            }
        }

        // Finally set Harbors types
        setHarbors(board, graph, cp, rnd);

        return graph;
    }

    private void setHarbors(CatanTile[][] board, GraphBoardWithEdges graphBoard, CatanParameters cp, Random rnd) {
        // set harbors along the tiles where the SEA borders the land
        ArrayList<CatanParameters.Resource> harbors = new ArrayList<>();
        for (Map.Entry<CatanParameters.Resource, Integer> entry : cp.harborCount.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++)
                harbors.add(entry.getKey());
        }
        Random toUse = cp.hexShuffleSeed > -1 ? new Random(cp.hexShuffleSeed * 2L) : rnd;
        Collections.shuffle(harbors, toUse);

        int radius = board.length / 2;
        // todo edge 4 can work, but random would be better, the math changes with different directions.
        //int edge = random.nextInt(HEX_SIDES);
        int edge = 4;
        // Get mid tile
        CatanTile tile = board[radius][radius];
        // move along a certain edge to reach SEA tiles
        for (int i = 0; i < radius; i++) {
            int[] tileLocation = tile.getNeighbourOnEdge(edge);
            tile = board[tileLocation[0]][tileLocation[1]];
        }
        // go around in a circle
        int counter = 0;
        for (int i = 0; i < HEX_SIDES; i++) {
            for (int j = 0; j < board.length / 2; j++) {
                int[] tileLocation = tile.getNeighbourOnEdge(i);
                tile = board[tileLocation[0]][tileLocation[1]];
                if (counter % 2 == 0 && !harbors.isEmpty()) {
                    CatanParameters.Resource harbour = harbors.remove(0);
                    ((Building)graphBoard.getNodeByID(tile.getVerticesBoardNodeIDs()[(i + 2) % HEX_SIDES])).setHarbour(harbour);
                    ((Building)graphBoard.getNodeByID(tile.getVerticesBoardNodeIDs()[(i + 3) % HEX_SIDES])).setHarbour(harbour);
                }
                counter++;
            }
        }
    }
}

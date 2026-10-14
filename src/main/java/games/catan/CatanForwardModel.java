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
import core.components.grid.HexCoord;
import core.components.grid.HexSide;
import core.components.grid.HexVertex;
import core.components.grid.SparseGrid;
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
        // the robber starts on the desert
        for (CatanTile tile : state.getTiles())
            if (tile.getTileType() == CatanTile.TileType.DESERT)
                state.setRobber(tile.hex);
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
                            break;
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
            for (CatanTile tile : gs.getTiles()) {
                if (tile.getNumber() == rollValue && !gs.hasRobber(tile)) {
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

    private SparseGrid<HexCoord, CatanTile> generateBoard(CatanParameters params, Random rnd) {
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

        // land within the radius of the centre, ringed by sea
        int radius = params.n_tiles_per_row / 2;
        SparseGrid<HexCoord, CatanTile> board = new SparseGrid<>("Board");
        for (HexCoord hex : HexCoord.hexagon(radius)) {
            CatanTile.TileType type = null;
            int number = 0;
            if (hex.distance(HexCoord.ORIGIN) == radius) {
                type = CatanTile.TileType.SEA;
            } else if (!tileList.isEmpty()) {
                type = tileList.remove(0);
                // desert has no number
                if (type != CatanTile.TileType.DESERT) {
                    number = numberList.remove(0);
                }
            }
            board.set(hex, new CatanTile(hex, type, number));
        }
        return board;
    }

    private GraphBoardWithEdges extractGraphFromBoard(SparseGrid<HexCoord, CatanTile> board, CatanParameters cp, Random rnd) {
        GraphBoardWithEdges graph = new GraphBoardWithEdges();
        List<CatanTile> tiles = new ArrayList<>();
        for (HexCoord hex : board.sortedPositions())
            tiles.add(board.get(hex));

        // A building at each vertex, shared by the tiles meeting there
        Map<HexVertex, Integer> buildings = new HashMap<>();
        Map<HexCoord, int[]> vertexIDs = new HashMap<>();
        for (CatanTile tile : tiles) {
            int[] ids = new int[HEX_SIDES];
            for (int vertex = 0; vertex < HEX_SIDES; vertex++) {
                ids[vertex] = buildings.computeIfAbsent(new HexVertex(tile.hex, vertex), v -> {
                    Building building = new Building();
                    graph.addBoardNode(building);
                    return building.getComponentID();
                });
            }
            vertexIDs.put(tile.hex, ids);
        }

        // A road along each side of a land tile other than the desert, from one of its vertices to the next. The
        // tile across the side, whatever it is, shares it
        Map<HexSide, Integer> roads = new HashMap<>();
        for (CatanTile tile : tiles) {
            if (tile.getTileType() == CatanTile.TileType.DESERT || tile.getTileType() == CatanTile.TileType.SEA)
                continue;
            for (int i = 0; i < HEX_SIDES; i++) {
                HexSide side = new HexSide(tile.hex, i);
                if (!roads.containsKey(side)) {
                    Edge edge = graph.addConnection(vertexIDs.get(tile.hex)[i], vertexIDs.get(tile.hex)[(i + 1) % HEX_SIDES]);
                    roads.put(side, edge.getComponentID());
                }
            }
        }
        for (CatanTile tile : tiles) {
            int[] edgeIDs = new int[HEX_SIDES];
            for (int i = 0; i < HEX_SIDES; i++) {
                Integer id = roads.get(new HexSide(tile.hex, i));
                edgeIDs[i] = id == null ? -1 : id;
            }
            board.set(tile.hex, tile.withIDs(vertexIDs.get(tile.hex), edgeIDs));
        }

        // Finally set Harbors types
        setHarbors(board, graph, cp, rnd);

        return graph;
    }

    private void setHarbors(SparseGrid<HexCoord, CatanTile> board, GraphBoardWithEdges graphBoard, CatanParameters cp, Random rnd) {
        // set harbors along the tiles where the SEA borders the land
        ArrayList<CatanParameters.Resource> harbors = new ArrayList<>();
        for (Map.Entry<CatanParameters.Resource, Integer> entry : cp.harborCount.entrySet()) {
            for (int i = 0; i < entry.getValue(); i++)
                harbors.add(entry.getKey());
        }
        Random toUse = cp.hexShuffleSeed > -1 ? new Random(cp.hexShuffleSeed * 2L) : rnd;
        Collections.shuffle(harbors, toUse);

        // on every other tile of the ring of sea, at its two vertices facing the land
        int radius = cp.n_tiles_per_row / 2;
        List<HexCoord> ring = HexCoord.ORIGIN.ring(radius);
        for (int k = 0; k < ring.size() && !harbors.isEmpty(); k += 2) {
            CatanTile tile = board.get(ring.get(k));
            // the ring reaches this tile by a step in direction i, so the land is at its vertices i + 2 and i + 3
            int i = k / radius;
            CatanParameters.Resource harbour = harbors.remove(0);
            ((Building) graphBoard.getNodeByID(tile.getVertexID((i + 2) % HEX_SIDES))).setHarbour(harbour);
            ((Building) graphBoard.getNodeByID(tile.getVertexID((i + 3) % HEX_SIDES))).setHarbour(harbour);
        }
    }
}

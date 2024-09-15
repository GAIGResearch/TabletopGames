package games.catan;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.components.Counter;
import core.components.Deck;
import core.components.Edge;
import games.catan.actions.build.*;
import games.catan.actions.dev.*;
import games.catan.actions.discard.DiscardResources;
import games.catan.actions.robber.MoveRobber;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.actions.setup.DeepPlaceSettlementThenRoad;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.actions.trade.*;
import games.catan.components.Building;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;
import games.puertorico.roles.Settler;
import org.antlr.v4.runtime.misc.IntSet;
import utilities.Utils;

import java.util.*;

import static games.catan.CatanConstants.HEX_SIDES;
import static games.catan.components.Building.Type.Settlement;
import static java.util.stream.Collectors.toList;

public class CatanActionFactory {
    /**
     * Calculates setup actions
     *
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getSetupActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        // find possible settlement locations and propose them as actions
        CatanTile[][] board = gs.getBoard();
        HashSet<Integer> settlementsAdded = new HashSet<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                // where it is legal to place tile then it can be placed from there
                if (!(tile.getTileType().equals(CatanTile.TileType.SEA) ||
                        tile.getTileType().equals(CatanTile.TileType.DESERT))) {
                    for (int i = 0; i < HEX_SIDES; i++) {
                        Building settlement = gs.getBuilding(tile, i);
                        if (!settlementsAdded.contains(settlement.getComponentID()) && settlement.getOwnerId() == -1) {
                            if (gs.checkSettlementPlacement(settlement, gs.getCurrentPlayer())) {
                                settlementsAdded.add(settlement.getComponentID());
                                if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Flat is default
                                    int[][] coords = tile.getNeighboursOnVertex(i);
                                    int edge = (HEX_SIDES + i - 1) % HEX_SIDES;
                                    Edge edgeObj = gs.getRoad(settlement, tile, edge);
                                    if (edgeObj.getOwnerId() == -1) {
                                        actions.add(new PlaceSettlementWithRoad(x, y, i, edge, player));
                                        for (int k = 0; k < coords.length; k++) {
                                            int[] neighbour = coords[k];
                                            int vertex = (i + 2 * (k + 1)) % HEX_SIDES;
                                            edge = (HEX_SIDES + vertex - 1) % HEX_SIDES;
                                            CatanTile nTile = board[neighbour[0]][neighbour[1]];
                                            edgeObj = gs.getRoad(nTile, vertex, edge);
                                            if (edgeObj != null && edgeObj.getOwnerId() == -1) {
                                                actions.add(new PlaceSettlementWithRoad(neighbour[0], neighbour[1], vertex, edge, player));
                                            }
                                        }
                                    }
                                } else {
                                    actions.add(new DeepPlaceSettlementThenRoad(x, y, i, player));
                                }
                            }
                        }
                    }
                }
            }
        }

        return actions;
    }

    static List<AbstractAction> getPlayerTradeActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        List<AbstractAction> actions = new ArrayList<>();
        OfferPlayerTrade opt = (OfferPlayerTrade) gs.tradeOffer;

        if (opt != null) {
            // Reject the trade offer
            actions.add(new EndNegotiation(player, opt.offeringPlayerID));

            // Accept the offer if legal exchange
            if (gs.checkCost(opt.resourceOffered, opt.nOffered, opt.offeringPlayerID) && gs.checkCost(opt.resourceRequested, opt.nRequested, opt.otherPlayerID)) {
                actions.add(new AcceptTrade(player, opt.resourceOffered, opt.nOffered, opt.resourceRequested, opt.nRequested, opt.offeringPlayerID, opt.otherPlayerID));
            }

            // Or counter-offer, if we've not already done too many steps
            if (gs.nTradesThisTurn < ((CatanParameters) gs.getGameParameters()).max_negotiation_count) {
                actions.addAll(CatanActionFactory.getPlayerTradeOfferActions(gs, actionSpace, player, opt));
            }
        } else if (gs.getNResourcesInHand(player) > 0) {
            // Create a new offer
            actions.addAll(getPlayerTradeOfferActions(gs, actionSpace, player, null));
        }
        return actions;
    }

    /**
     * Generates PlayerTradeOffers relating to single type trades
     * i.e Lumber for Grain, Brick for Stone
     *
     * @param gs          - current state
     * @param actionSpace - action space type
     * @param playerID    - active player actions are computed for (and has visibility of their own resources)
     * @param tradeOffer  - current trade offer to reply to. If null, a new one will be created from scratch.
     * @return - ArrayList of OfferPlayerTrade actions
     */
    public static List<AbstractAction> getPlayerTradeOfferActions(CatanGameState gs, ActionSpace actionSpace, int playerID, OfferPlayerTrade tradeOffer) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Map<CatanParameters.Resource, Counter> resources = gs.getPlayerResources(playerID);
        int n_players = gs.getNPlayers();
        if (tradeOffer == null) {
            // Construct new offer
            List<AbstractAction> offers = new ArrayList<>();
            for (int playerIndex = 0; playerIndex < n_players; playerIndex++) { // loop through players
                if (playerIndex != playerID && gs.getNResourcesInHand(playerIndex) > 0) { // exclude current player and players with no resources in hand
                    for (CatanParameters.Resource resToOffer : CatanParameters.Resource.values()) {
                        if (resToOffer == CatanParameters.Resource.WILD) continue;
                        int maxToOffer = resources.get(resToOffer).getValue();
                        if (maxToOffer > 0) {
                            for (CatanParameters.Resource resToRequest : CatanParameters.Resource.values()) {
                                if (resToRequest == CatanParameters.Resource.WILD) continue;
                                if (resToRequest != resToOffer) {
                                    int maxToRequest = ((CatanParameters) gs.getGameParameters()).max_resources_request_trade;
                                    if (maxToRequest > 0) { // exclude the currently offered resource
                                        offers.addAll(createTradeOfferActions(playerID, playerIndex, resToOffer, resToRequest, maxToOffer, maxToRequest, -1, -1, OfferPlayerTrade.Stage.Offer));
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Default is flat
                actions.addAll(offers);
            } else if (offers.size() > 0) {
                // Deep new offer construct
                actions.add(new DeepConstructNewOffer(playerID));
            }
        } else {
            // Adjust existing offer
            List<AbstractAction> allCounterOffers = new ArrayList<>();
            int maxToOffer = tradeOffer.offeringPlayerID == playerID ? resources.get(tradeOffer.resourceOffered).getValue() : ((CatanParameters) gs.getGameParameters()).max_resources_request_trade;
            int maxToRequest = tradeOffer.otherPlayerID == playerID ? resources.get(tradeOffer.resourceRequested).getValue() : ((CatanParameters) gs.getGameParameters()).max_resources_request_trade;
            if (maxToOffer > 0 && maxToRequest > 0) {
                allCounterOffers = createTradeOfferActions(tradeOffer.offeringPlayerID, tradeOffer.otherPlayerID,
                        tradeOffer.resourceOffered, tradeOffer.resourceRequested, maxToOffer, maxToRequest,
                        tradeOffer.nOffered, tradeOffer.nRequested,
                        tradeOffer.stage == OfferPlayerTrade.Stage.Offer ? OfferPlayerTrade.Stage.CounterOffer : OfferPlayerTrade.Stage.Offer);
            }
            if (!allCounterOffers.isEmpty()) {
                if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Default is flat
                    actions.addAll(allCounterOffers);
                } else {
                    // Deep counter-offer construct. Only add if there exists a counter-offer, we still need to calculate all options
                    actions.add(new DeepCounterOffer(tradeOffer.stage, playerID));
                }
            }
        }
        return actions;
    }

    /**
     * Helper function that lists all combinations of trade offers, from 1 to maxToOffer of resource offered, and from 1 to maxToRequest for resource requested
     *
     * @param offeringPlayer    - player offering trade originally (may not be active player)
     * @param otherPlayer       - player involved in trade (that resources are requested from)
     * @param resourceToOffer   - resource to offer
     * @param resourceToRequest - resource to request
     * @param maxToOffer        - maximum number of resources that should be offered
     * @param maxToRequest      - maximum number of resources that should be requested
     * @param nOffered          - number of resources offered previously, -1 if not yet decided
     * @param nRequested        - number of resources requested previously, -1 if not yet decided
     * @return actions - list of actions
     */
    private static List<AbstractAction> createTradeOfferActions(int offeringPlayer, int otherPlayer,
                                                                CatanParameters.Resource resourceToOffer,
                                                                CatanParameters.Resource resourceToRequest,
                                                                int maxToOffer, int maxToRequest,
                                                                int nOffered, int nRequested, OfferPlayerTrade.Stage stage) {
        List<AbstractAction> actions = new ArrayList<>();
        for (int offerQuantity = 1; offerQuantity <= maxToOffer; offerQuantity++) {
            for (int requestQuantity = 1; requestQuantity <= maxToRequest; requestQuantity++) {
                if (nOffered != offerQuantity || nRequested != requestQuantity) {
                    actions.add(new OfferPlayerTrade(stage, resourceToOffer, offerQuantity, resourceToRequest, requestQuantity, offeringPlayer, otherPlayer));
                }
            }
        }
        return actions;
    }

    /**
     * Combinations of resources in hand to discard.
     *
     * @param gs          - game state
     * @param actionSpace - action space type
     * @param player      - player to discard resources
     * @param nToDiscard  - how many to discard
     * @return - list of actions discarding cards.
     */
    public static List<AbstractAction> getDiscardActions(CatanGameState gs, ActionSpace actionSpace, int player, int nToDiscard) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Flat is default
            int nResources = gs.getNResourcesInHand(player);
            int[] resIdx = new int[nResources];
            for (int i = 0; i < nResources; i++) resIdx[i] = i;
            List<int[]> combinations = Utils.generateCombinations(resIdx, nResources);
            for (int[] combination : combinations) {
                CatanParameters.Resource[] cardsToDiscard = new CatanParameters.Resource[nToDiscard];
                for (int i = 0; i < nToDiscard; i++) {
                    cardsToDiscard[i] = gs.pickResourceFromHand(player, combination[i]);
                }
                actions.add(new DiscardResources(cardsToDiscard, player));
            }
        } else {
            // Deep: Choose 1 resource at a time
            for (CatanParameters.Resource resource : CatanParameters.Resource.values()) {
                if (resource == CatanParameters.Resource.WILD) continue;
                if (gs.getPlayerResources(player).get(resource).getValue() > 0)
                    actions.add(new DiscardResources(new CatanParameters.Resource[]{resource}, player));
            }
        }
        if (actions.size() == 0) actions.add(new DoNothing());
        return actions;
    }

    /**
     * Calculates player's actions when robber is active
     *
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getRobberActions(CatanGameState gs, ActionSpace actionSpace, int player, boolean knight) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                if (!(tile.getTileType().equals(CatanTile.TileType.SEA))) {
                    if (actionSpace.structure != ActionSpace.Structure.Deep) { // Flat is default
                        Set<Integer> targets = new LinkedHashSet<>();
                        Building[] settlements = gs.getBuildings(tile);
                        for (Building settlement : settlements) {
                            if (settlement.getOwnerId() != -1 && settlement.getOwnerId() != gs.getCurrentPlayer()) {
                                targets.add(settlement.getOwnerId());
                            }
                        }
                        for (int target : targets) {
                            if (knight) actions.add(new PlayKnightCard(x, y, player, target));
                            else actions.add(new MoveRobberAndSteal(x, y, player, target));
                        }
                        if (targets.isEmpty()) {
                            if (knight) actions.add(new PlayKnightCard(x, y, player, -1));
                            else actions.add(new MoveRobberAndSteal(x, y, player, -1));
                        }
                    } else {
                        // Deep: first move, then steal
                        if (knight) actions.add(new PlayKnightCardDeep(x, y, player));
                        else actions.add(new MoveRobber(x, y, player));
                    }
                }
            }
        }
        return actions;
    }

    /**
     * @param gs          - game state
     * @param actionSpace - action space type
     * @param player      - player buying
     * @return lists all buy actions to the player; building road, settlement, city or buying a development card
     */
    public static List<AbstractAction> getBuyActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        ArrayList<AbstractAction> actions = new ArrayList<>();

        List<AbstractAction> buyRoadActions = getBuyRoadActions(gs, player, false);
        List<AbstractAction> buySettlementActions = getBuySettlementActions(gs, player);
        // Road, Settlement or City
        if (actionSpace.structure != ActionSpace.Structure.Deep) {
            actions.addAll(buyRoadActions);
            actions.addAll(buySettlementActions);
        } else {
            // Deep: choose between buying road / city / settlement, then where to place them
            for (BuyAction.BuyType type : BuyAction.BuyType.values()) {
                if (type == BuyAction.BuyType.DevCard) continue;
                if (gs.checkCost(catanParameters.costMapping.get(type), player)
                        && !gs.playerTokens.get(player).get(type).isMaximum()) {
                    if (type == BuyAction.BuyType.Road && buyRoadActions.size() > 0 ||
                            type == BuyAction.BuyType.Settlement && buySettlementActions.size() > 0)
                        actions.add(new BuyAction(player, type));
                }
            }
        }

        // Dev card separate, always just 1 step, no choice
        if (gs.checkCost(catanParameters.costMapping.get(BuyAction.BuyType.DevCard), player) && gs.devCards.getSize() > 0) {
            actions.add(new BuyDevelopmentCard(player));
        }

        return actions;
    }

    public static List<AbstractAction> getBuyRoadActions(CatanGameState gs, int player, boolean free) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (free || gs.checkCost(catanParameters.costMapping.get(BuyAction.BuyType.Road), player)
                && !gs.playerTokens.get(player).get(BuyAction.BuyType.Road).isMaximum()) {
            CatanTile[][] board = gs.getBoard();
            HashSet<Edge> edgesChecked = new HashSet<>(256);
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    // Skip sea and desert tiles; we will look at their edges via their neighbours
                    if (tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
                        continue;
                    for (int i = 0; i < HEX_SIDES; i++) {
                        // we build from a vertex (settlement) to an adjacent vertex, but do not need to
                        // actually retrieve the settlement object
                        Edge edge = gs.getRoad(tile, i, i);
                        if (edge == null) continue;
                        if (edgesChecked.contains(edge)) continue;
                        edgesChecked.add(edge);

                        if (gs.checkRoadPlacement(tile, i, (i + 1) % HEX_SIDES, edge, gs.getCurrentPlayer())) {
                            actions.add(new BuildRoad(x, y, i, player, free, edge.getComponentID()));
                        }
                    }
                }
            }
        }
        return actions;
    }

    public static List<AbstractAction> getBuySettlementActions(CatanGameState gs, int player) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        boolean canBuySettlement = gs.checkCost(catanParameters.costMapping.get(BuyAction.BuyType.Settlement), player)
                && !gs.playerTokens.get(player).get(BuyAction.BuyType.Settlement).isMaximum();
        boolean canBuyCity = gs.checkCost(catanParameters.costMapping.get(BuyAction.BuyType.City), player)
                && !gs.playerTokens.get(player).get(BuyAction.BuyType.City).isMaximum();
        if (canBuySettlement || canBuyCity) {
            Set<Building> settlementsAdded = new HashSet<>();
            CatanTile[][] board = gs.getBoard();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < HEX_SIDES; i++) {
                        Building settlement = gs.getBuilding(tile, i);
                        if (settlementsAdded.contains(settlement)) continue;
                        settlementsAdded.add(settlement);

                        if (canBuyCity && settlement.getOwnerId() == player && settlement.getBuildingType() == Settlement) {
                            actions.add(new BuildCity(x, y, i, player));
                        }

                        if (canBuySettlement && settlement.getOwnerId() == -1) {  // cannot build on top of existing settlement
                            // legal to place?
                            if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
                                    && gs.checkSettlementPlacement(settlement, gs.getCurrentPlayer())) {
                                actions.add(new BuildSettlement(x, y, i, player, false));
                            }
                        }
                    }
                }
            }
        }
        return actions;
    }

    /**
     * @param gs          - game state
     * @param actionSpace - action space type
     * @param player      - player playing dev card
     * @return list of actions to play a dev card in hand
     */
    public static List<AbstractAction> getDevCardActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        Set<AbstractAction> actions = new LinkedHashSet<>();
        Deck<CatanCard> playerDevDeck = gs.playerDevCards.get(player);

        for (CatanCard c : playerDevDeck.getComponents()) {
            // avoid playing a card that has been bought in the same turn
            if (c.roundCardWasBought == gs.getTurnCounter() || c.cardType == CatanCard.CardType.VICTORY_POINT_CARD) {  // We don't play VP cards
                continue;
            }
            if (actionSpace.structure != ActionSpace.Structure.Deep) { // Flat is default
                actions.addAll(getDevCardActions(gs, actionSpace, player, c.cardType));
            } else {
                // Deep: play dev card of type X. Then compute for the card type the variations possible, potentially in a deep way if available
                actions.add(new PlayDevCard(player, c.cardType, c.cardType.nDeepSteps((CatanParameters) gs.getGameParameters())));
            }
        }

        return new ArrayList<>(actions);
    }

    public static List<AbstractAction> getDevCardActions(CatanGameState gs, ActionSpace actionSpace, int player, CatanCard.CardType cardType) {
        ArrayList<AbstractAction> actions = new ArrayList<>();

        if (cardType == CatanCard.CardType.KNIGHT_CARD) {
            actions.addAll(getRobberActions(gs, actionSpace, player, true));
        } else if (cardType == CatanCard.CardType.MONOPOLY) {
            for (CatanParameters.Resource resource : CatanParameters.Resource.values()) {
                if (resource == CatanParameters.Resource.WILD) continue;
                actions.add(new PlayMonopoly(resource, player));
            }
        } else if (cardType == CatanCard.CardType.YEAR_OF_PLENTY) {
            List<CatanParameters.Resource> resourcesAvailable = new ArrayList<>();
            for (CatanParameters.Resource res : CatanParameters.Resource.values()) {
                if (res == CatanParameters.Resource.WILD) continue;
                if (gs.resourcePool.get(res).getValue() > 0)
                    for (int i = 0; i < ((CatanParameters) gs.getGameParameters()).nResourcesYoP; i++) {  // TODO this loop not needed if Utils.generateCombinations allows repetitions
                        resourcesAvailable.add(res);
                    }
            }

            if (resourcesAvailable.size() >= ((CatanParameters) gs.getGameParameters()).nResourcesYoP) {
                if (actionSpace.structure != ActionSpace.Structure.Deep) {

                    int[] resIdx = new int[resourcesAvailable.size()];
                    for (int i = 0; i < resourcesAvailable.size(); i++) {
                        resIdx[i] = i;
                    }
                    List<int[]> combinations = Utils.generateCombinations(resIdx, ((CatanParameters) gs.getGameParameters()).nResourcesYoP);
                    for (int[] combo : combinations) {
                        CatanParameters.Resource[] resources = new CatanParameters.Resource[combo.length];
                        for (int i = 0; i < combo.length; i++) {
                            resources[i] = resourcesAvailable.get(combo[i]);
                        }
                        actions.add(new PlayYearOfPlenty(resources, player, true));
                    }
                } else {
                    // Deep: one resource at a time
                    for (CatanParameters.Resource res : CatanParameters.Resource.values()) {
                        if (res != CatanParameters.Resource.WILD && gs.resourcePool.get(res).getValue() > 0)
                            actions.add(new DeepYearOfPlenty(player, res, cardType.nDeepSteps((CatanParameters) gs.getGameParameters())));
                    }
                }
            }
        } else if (cardType == CatanCard.CardType.ROAD_BUILDING) {
            if (!gs.playerTokens.get(player).get(BuyAction.BuyType.Road).isMaximum()) {

                List<AbstractAction> roads = getBuyRoadActions(gs, player, true);
                int nRoads = Math.min(gs.playerTokens.get(player).get(BuyAction.BuyType.Road).getValue(), ((CatanParameters) gs.getGameParameters()).nRoadsRB);
                if (actionSpace.structure != ActionSpace.Structure.Deep && roads.size() >= nRoads) {  // Flat is default

                    // Identify all combinations of possible roads to build
                    int[] roadsIdx = new int[roads.size()];
                    for (int i = 0; i < roads.size(); i++) {
                        roadsIdx[i] = i;
                    }
                    List<int[]> combinations = Utils.generateCombinations(roadsIdx, nRoads);
                    for (int[] combo : combinations) {
                        AbstractAction[] roadsToBuild = new BuildRoad[combo.length];
                        for (int i = 0; i < combo.length; i++) {
                            roadsToBuild[i] = roads.get(combo[i]);
                        }
                        actions.add(new PlayRoadBuilding(player, roadsToBuild));
                    }
                } else {
                    // Deep: one road at a time
                    for (AbstractAction road : roads) {
                        actions.add(new DeepRoadBuilding(player, road, nRoads));
                    }
                }
            }
        }
        return actions;
    }

    /**
     * @param gs          - game state
     * @param actionSpace - action space type
     * @param player      - player trading
     * @return list all possible trades with the bank / harbours, using minimum exchange rate available for each resource
     * type owned by the player
     */
    public static List<AbstractAction> getDefaultTradeActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Map<CatanParameters.Resource, Counter> playerExchangeRate = gs.getExchangeRates(player);
        for (Map.Entry<CatanParameters.Resource, Counter> res : gs.playerResources.get(player).entrySet()) {
            if (res.getKey() == CatanParameters.Resource.WILD) continue;

            // give N resources (minimum exchange rate for this resource)
            CatanParameters.Resource resToGive = res.getKey();
            int nGive = playerExchangeRate.get(res.getKey()).getValue();
            int nOwned = res.getValue().getValue();
            if (nOwned >= nGive) {
                // for 1 other resource
                List<AbstractAction> trades = new ArrayList<>();
                for (CatanParameters.Resource resToGet : CatanParameters.Resource.values()) {
                    if (resToGet != CatanParameters.Resource.WILD && resToGive != resToGet && gs.getResourcePool().get(resToGet).getValue() > 0) {
                        trades.add(new DefaultTrade(resToGive, resToGet, nGive, player));
                    }
                }
                if (trades.size() > 0) {
                    if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Flat is default
                        actions.addAll(trades);
                    } else {
                        actions.add(new DeepDefaultTrade(resToGive, nGive, player));
                    }
                }
            }
        }
        return actions;
    }
}

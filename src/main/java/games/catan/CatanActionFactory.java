package games.catan;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.components.Counter;
import core.components.Deck;
import core.components.Edge;
import games.catan.actions.*;
import games.catan.components.Building;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;
import utilities.Utils;

import java.util.*;

import static games.catan.CatanConstants.HEX_SIDES;
import static games.catan.components.Building.Type.Settlement;

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
//                        actions.add(new BuildSettlement_v2(settlement, activePlayer));
//                        actions.add(new BuildSettlement(x, y, i, activePlayer));
                    for (int i = 0; i < HEX_SIDES; i++) {
                        Building settlement = gs.getBuilding(tile, i);
                        if (!settlementsAdded.contains(settlement.getComponentID()) && settlement.getOwnerId() == -1) {
                            if (gs.checkSettlementPlacement(settlement, gs.getCurrentPlayer())) {
                                settlementsAdded.add(settlement.getComponentID());
                                if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Flat is default
                                    int[][] coords = tile.getNeighboursOnVertex(i);
                                    int edge = (HEX_SIDES+i-1)%HEX_SIDES;
                                    Edge edgeObj = gs.getRoad(settlement, tile, edge);
                                    if (edgeObj.getOwnerId() == -1) {
                                        actions.add(new PlaceSettlementWithRoad(x, y, i, edge, player));
                                        for (int k = 0; k < coords.length; k++) {
                                            int[] neighbour = coords[k];
                                            int vertex = (i + 2*(k+1)) % HEX_SIDES;
                                            edge = (HEX_SIDES+vertex-1)%HEX_SIDES;
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

    /**
     * Generates PlayerTradeOffers relating to single type trades
     * i.e Lumber for Grain, Brick for Stone
     *
     * @param gs - current state
     * @param actionSpace - action space type
     * @param player - active player actions are computed for (and has visibility of their own resources
     * @return - ArrayList, PlayerTradeOffer type (unique).
     */
    static List<AbstractAction> getPlayerTradeOfferActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        return getPlayerTradeOfferActions(gs, actionSpace, player, -1, true, null, null, -1, -1, true, OfferPlayerTrade.Stage.Offer);
    }

    /**
     * Generates PlayerTradeOffers relating to single type trades
     * i.e Lumber for Grain, Brick for Stone
     *
     * @param gs - current state
     * @param actionSpace - action space type
     * @param player - active player actions are computed for (and has visibility of their own resources)
     * @param otherPlayer - other player involved in the trade, -1 if not yet decided
     * @param offer - if true, player is offering player in original trade. if false, player is the other player (that resources are requested from)
     * @param resourceToOffer - resource offered previously, null if not yet decided
     * @param resourceToRequest - resource requested previously, null if not yet decided
     * @param nOffered - number of resources offered previously, -1 if not yet decided
     * @param nRequested - number of resources requested previously, -1 if not yet decided
     * @param execute - if the action should execute. false if response to current trade offers, if one of these is already in progress.
     * @return - ArrayList of OfferPlayerTrade actions
     */
    public static List<AbstractAction> getPlayerTradeOfferActions(CatanGameState gs, ActionSpace actionSpace, int player, int otherPlayer, boolean offer,
                                                           CatanParameters.Resource resourceToOffer, CatanParameters.Resource resourceToRequest,
                                                           int nOffered, int nRequested, boolean execute, OfferPlayerTrade.Stage stage) {
        // TODO: deep
        ArrayList<AbstractAction> actions = new ArrayList<>();
        HashMap<CatanParameters.Resource, Counter> resources = gs.getPlayerResources(player);
        int n_players = gs.getNPlayers();
        if (otherPlayer == -1 && resourceToOffer == null && resourceToRequest == null) {
            for (int playerIndex = 0; playerIndex < n_players; playerIndex++) { // loop through players
                if (playerIndex != player) { // exclude current player
                    for (CatanParameters.Resource resToOffer : CatanParameters.Resource.values()) {
                        int maxToOffer = (offer ? resources.get(resToOffer).getValue() : ((CatanParameters) gs.getGameParameters()).max_resources_request_trade);
                        if (maxToOffer > 0) {
                            for (CatanParameters.Resource resToRequest : CatanParameters.Resource.values()) {
                                if (resToRequest != resToOffer) {
                                    int maxToRequest = (!offer ? resources.get(resToRequest).getValue() : ((CatanParameters) gs.getGameParameters()).max_resources_request_trade);
                                    if (maxToRequest > 0) { // exclude the currently offered resource
                                        if (offer) {
                                            createTradeOfferActions(player, playerIndex, resToOffer, resToRequest, actions, maxToOffer, maxToRequest, -1, -1, execute, stage);
                                        } else {
                                            createTradeOfferActions(playerIndex, player, resToOffer, resToRequest, actions, maxToOffer, maxToRequest, -1, -1, execute, stage);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            int maxToOffer = (offer ? resources.get(resourceToOffer).getValue() : ((CatanParameters) gs.getGameParameters()).max_resources_request_trade);
            int maxToRequest = (!offer ? resources.get(resourceToOffer).getValue() : ((CatanParameters) gs.getGameParameters()).max_resources_request_trade);
            if (maxToOffer > 0 && maxToRequest > 0) { // exclude the currently offered resource
                if (offer) {
                    createTradeOfferActions(player, otherPlayer, resourceToOffer, resourceToRequest, actions, maxToOffer, maxToRequest, nOffered, nRequested, execute, stage);
                } else {
                    createTradeOfferActions(otherPlayer, player, resourceToOffer, resourceToRequest, actions, maxToOffer, maxToRequest, nOffered, nRequested, execute, stage);
                }
            }
        }
        return actions;
    }

    /**
     * Helper function that lists all combinations of trade offers, from 1 to maxToOffer of resource offered, and from 1 to maxToRequest for resource requested
     * @param player - player offering trade originally (may not be active player)
     * @param otherPlayer - player involved in trade (that resources are requested from)
     * @param resourceToOffer - resource to offer
     * @param resourceToRequest - resource to request
     * @param actions - list of actions we should add the combinations to
     * @param maxToOffer - maximum number of resources that should be offered
     * @param maxToRequest - maximum number of resources that should be requested
     * @param nOffered - number of resources offered previously, -1 if not yet decided
     * @param nRequested - number of resources requested previously, -1 if not yet decided
     * @param execute - if the action should execute. false if response to current trade offers, if one of these is already in progress.
     */
    private static void createTradeOfferActions(int player, int otherPlayer,
                                                CatanParameters.Resource resourceToOffer,
                                                CatanParameters.Resource resourceToRequest,
                                                ArrayList<AbstractAction> actions,
                                                int maxToOffer, int maxToRequest,
                                                int nOffered, int nRequested, boolean execute, OfferPlayerTrade.Stage stage) {
        for (int offerQuantity = 1; offerQuantity <= maxToOffer; offerQuantity++) {
            HashMap<CatanParameters.Resource, Integer> resourcesOffered = new HashMap<>();
            resourcesOffered.put(resourceToOffer, offerQuantity);
            for (int requestQuantity = 1; requestQuantity <= maxToRequest; requestQuantity++) {
                if (nOffered != offerQuantity && nRequested != requestQuantity) {
                    HashMap<CatanParameters.Resource, Integer> resourcesRequested = new HashMap<>();
                    resourcesRequested.put(resourceToRequest, requestQuantity);
                    actions.add(new OfferPlayerTrade(stage, resourcesOffered, resourcesRequested, player, otherPlayer, execute)); // create the action
                }
            }
        }
    }

    /**
     * Combinations of resources in hand to discard.
     * @param gs - game state
     * @param actionSpace - action space type
     * @param player - player to discard resources
     * @param nToDiscard - how many to discard
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
            for (CatanParameters.Resource resource: CatanParameters.Resource.values()) {
                if (gs.getPlayerResources(player).get(resource).getValue() > 0) actions.add(new DiscardResources(new CatanParameters.Resource[]{resource}, player));
            }
        }
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
                        HashSet<Integer> targets = new HashSet<>();
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
     * @param gs - game state
     * @param actionSpace - action space type
     * @param player - player buying
     * @return lists all buy actions to the player; building road, settlement, city or buying a development card
     */
    public static List<AbstractAction> getBuyActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        ArrayList<AbstractAction> actions = new ArrayList<>();

        // Road, Settlement or City
        if (actionSpace.structure != ActionSpace.Structure.Deep) {
            actions.addAll(getBuyRoadActions(gs, player, false));
            actions.addAll(getBuySettlementActions(gs, player));
            actions.addAll(getBuyCityActions(gs, player));
        } else {
            // Deep: choose between buying road / city / settlement, then where to place them
            for (BuyAction.BuyType type: BuyAction.BuyType.values()) {
                if (type == BuyAction.BuyType.DevCard) continue;
                if (gs.checkCost(catanParameters.costMapping.get(type), player)
                        && !gs.playerTokens.get(player).get(type).isMaximum()) {
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
            HashSet<Integer> roadsAdded = new HashSet<>();
            CatanTile[][] board = gs.getBoard();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < HEX_SIDES; i++) {
                        Building settlement = gs.getBuilding(tile, i);
                        // Roads
                        Edge edge = gs.getRoad(settlement, tile, i);
                        if (edge == null || roadsAdded.contains(edge.getComponentID())) continue;
                        roadsAdded.add(edge.getComponentID());

                        if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
                                && gs.checkRoadPlacement(i, tile, gs.getCurrentPlayer())) {
                            actions.add(new BuildRoad(x, y, i, player, false));
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
        if (gs.checkCost(catanParameters.costMapping.get(BuyAction.BuyType.Settlement), player)
                && !gs.playerTokens.get(player).get(BuyAction.BuyType.Settlement).isMaximum()) {
            HashSet<Integer> settlementsAdded = new HashSet<>();
            CatanTile[][] board = gs.getBoard();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < HEX_SIDES; i++) {
                        Building settlement = gs.getBuilding(tile, i);
                        if (settlementsAdded.contains(settlement.getComponentID())) continue;
                        settlementsAdded.add(settlement.getComponentID());

                        // legal to place?
                        if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
                                && gs.checkSettlementPlacement(settlement, gs.getCurrentPlayer())) {
                            actions.add(new BuildSettlement(x, y, i, player, false));
                        }
                    }
                }
            }
        }
        return actions;
    }

    public static List<AbstractAction> getBuyCityActions(CatanGameState gs, int player) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        ArrayList<AbstractAction> actions = new ArrayList<>();
        if (gs.checkCost(catanParameters.costMapping.get(BuyAction.BuyType.City), player)
                && !gs.playerTokens.get(player).get(BuyAction.BuyType.City).isMaximum()) {
            HashSet<Integer> settlementsAdded = new HashSet<>();
            CatanTile[][] board = gs.getBoard();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < HEX_SIDES; i++) {
                        Building settlement = gs.getBuilding(tile, i);
                        if (settlementsAdded.contains(settlement.getComponentID())) continue;
                        settlementsAdded.add(settlement.getComponentID());
                        if (settlement.getOwnerId() == player && settlement.getBuildingType() == Settlement) {
                            actions.add(new BuildCity(x, y, i, player));
                        }
                    }
                }
            }
        }
        return actions;
    }

    /**
     * @param gs - game state
     * @param actionSpace - action space type
     * @param player - player playing dev card
     * @return list of actions to play a dev card in hand
     */
    public static List<AbstractAction> getDevCardActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<CatanCard> playerDevDeck = gs.playerDevCards.get(player);

        for (CatanCard c : playerDevDeck.getComponents()) {
            // avoid playing a card that has been bought in the same turn
            if (c.turnCardWasBought == gs.getTurnCounter()) {
                continue;
            }
            if (actionSpace.structure != ActionSpace.Structure.Deep) { // Flat is default
                actions.addAll(getDevCardActions(gs, actionSpace, player, c.cardType));
            } else {
                // Deep: play dev card of type X. Then compute for the card type the variations possible, potentially in a deep way if available
                actions.add(new PlayDevCard(player, c.cardType, c.cardType.nDeepSteps((CatanParameters) gs.getGameParameters())));
            }
        }

        return actions;
    }

    public static List<AbstractAction> getDevCardActions(CatanGameState gs, ActionSpace actionSpace, int player, CatanCard.CardType cardType) {
        ArrayList<AbstractAction> actions = new ArrayList<>();

        if (cardType == CatanCard.CardType.KNIGHT_CARD) {
            actions.addAll(getRobberActions(gs, actionSpace, player, true));
        }

        else if (cardType == CatanCard.CardType.MONOPOLY) {
            for (CatanParameters.Resource resource : CatanParameters.Resource.values()) {
                actions.add(new PlayMonopoly(resource, player));
            }
        }

        else if (cardType == CatanCard.CardType.YEAR_OF_PLENTY) {
            if (actionSpace.structure != ActionSpace.Structure.Deep) {
                List<CatanParameters.Resource> resourcesAvailable = new ArrayList<>();

                for (CatanParameters.Resource res : CatanParameters.Resource.values()) {
                    if (gs.resourcePool.get(res).getValue() > 0)
                        for (int i = 0; i < ((CatanParameters) gs.getGameParameters()).nResourcesYoP; i++) {  // TODO this loop not needed if Utils.generateCombinations allows repetitions
                            resourcesAvailable.add(res);
                        }
                }

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
                    actions.add(new PlayYearOfPlenty(resources, player));
                }
            } else {
                // Deep: one resource at a time
                for (CatanParameters.Resource res : CatanParameters.Resource.values()) {
                    if (gs.resourcePool.get(res).getValue() > 0)
                        actions.add(new DeepYearOfPlenty(player, res, cardType.nDeepSteps((CatanParameters) gs.getGameParameters())));
                }
            }
        }

        else if (cardType == CatanCard.CardType.ROAD_BUILDING) {
            List<AbstractAction> roads = getBuyRoadActions(gs, player, true);

            if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Flat is default

                // Identify all combinations of possible roads to build
                int[] roadsIdx = new int[roads.size()];
                for (int i = 0; i < roads.size(); i++) {
                    roadsIdx[i] = i;
                }
                List<int[]> combinations = Utils.generateCombinations(roadsIdx, ((CatanParameters) gs.getGameParameters()).nRoadsRB);
                for (int[] combo : combinations) {
                    AbstractAction[] roadsToBuild = new BuildRoad[combo.length];
                    for (int i = 0; i < combo.length; i++) {
                        roadsToBuild[i] = roads.get(combo[i]);
                    }
                    actions.add(new PlayRoadBuilding(player, roadsToBuild));
                }

//                CatanTile[][] board = gs.getBoard();
//                for (int x = 0; x < board.length; x++) {
//                    for (int y = 0; y < board[x].length; y++) {
//                        CatanTile tile = board[x][y];
//                        for (int i = 0; i < HEX_SIDES; i++) {
//                            if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
//                                    && gs.checkRoadPlacement(i, tile, player)
//                                    && !gs.playerTokens.get(player).get(BuyAction.BuyType.Road).isMaximum()) {
//                                actions.add(new BuildRoad(x, y, i, player, true));
//                            }
//                        }
//                    }
//                }
            } else {
                // Deep: one road at a time
                for (AbstractAction road: roads) {
                    actions.add(new DeepRoadBuilding(player, road, cardType.nDeepSteps((CatanParameters) gs.getGameParameters())));
                }
            }
        }
        return actions;
    }

    /**
     * @param gs - game state
     * @param actionSpace - action space type
     * @param player - player trading
     * @return list all possible trades with the bank / harbours, using minimum exchange rate available for each resource
     * type owned by the player
     */
    public static List<AbstractAction> getDefaultTradeActions(CatanGameState gs, ActionSpace actionSpace, int player) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        HashMap<CatanParameters.Resource, Counter> playerExchangeRate = gs.getExchangeRates(player);
        for (Map.Entry<CatanParameters.Resource, Counter> res: gs.playerResources.get(player).entrySet()) {
            // give N resources (minimum exchange rate for this resource)
            CatanParameters.Resource resToGive = res.getKey();
            int nGive = playerExchangeRate.get(res.getKey()).getValue();
            int nOwned = res.getValue().getValue();
            if (nOwned >= nGive) {
                if (actionSpace.structure != ActionSpace.Structure.Deep) {  // Flat is default
                    // for 1 other resource
                    for (CatanParameters.Resource resToGet : CatanParameters.Resource.values()) {
                        if (resToGive != resToGet && gs.getResourcePool().get(resToGet).getValue() > 0) {
                            actions.add(new DefaultTrade(resToGive, resToGet, nGive, player));
                        }
                    }
                } else {
                    actions.add(new DeepDefaultTrade(resToGive, nGive, player));
                }
            }
        }
        return actions;
    }
}

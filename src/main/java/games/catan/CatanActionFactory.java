package games.catan;

import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.components.Card;
import core.components.Counter;
import core.components.Deck;
import games.catan.actions.*;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;
import games.catan.components.Settlement;

import java.util.*;

public class CatanActionFactory {
    /**
     * Calculates setup actions
     *
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getSetupActions(CatanGameState gs, ActionSpace actionSpace) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int turnStep = ((CatanTurnOrder) gs.getTurnOrder()).turnStep;
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList<>();
        // find possible settlement locations and propose them as actions
        CatanTile[][] board = gs.getBoard();
        if (turnStep == 0) {
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    // where it is legal to place tile then it can be placed from there
                    if (!(tile.getTileType().equals(CatanTile.TileType.SEA) ||
                            tile.getTileType().equals(CatanTile.TileType.DESERT))) {
//                            actions.add(new BuildSettlement_v2(settlement, activePlayer));
//                            actions.add(new BuildSettlement(x, y, i, activePlayer));
                        if (actionSpace.structure == ActionSpace.Structure.Flat) {
                            for (int i = 0; i < 6; i++) {
                                Settlement settlement = tile.getSettlements()[i];
                                if (settlement.getOwner() == -1) {
                                    if (gs.checkSettlementPlacement(settlement, gs.getCurrentPlayer())) {
                                        actions.add(new PlaceSettlementWithRoad(x, y, i, activePlayer));
                                    }
                                }
                            }
                        } else {

                        }
                    }
                }
            }
        }

        return actions;
    }

    /**
     * Calculates regular Player actions in the Trade stage
     *
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getTradeStageActions(CatanGameState gs) {
        CatanTurnOrder cto = (CatanTurnOrder) gs.getTurnOrder();

        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());
        actions.addAll(getTradeActions(gs));
        actions.addAll(getPlayerTradeOfferActions(gs));
        if (!cto.isDevelopmentCardPlayed()) {
            actions.addAll(getDevCardActions(gs));
        }

        return actions;
    }

    /**
     * Generates PlayerTradeOffers relating to single type trades
     * i.e Lumber for Grain, Brick for Stone
     *
     * @param gs
     * @return - ArrayList, PlayerTradeOffer type (unique).
     */
    static List<AbstractAction> getPlayerTradeOfferActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        HashMap<CatanParameters.Resource, Counter> resources = gs.getPlayerResources(gs.getCurrentPlayer());
        int exchangeRate = ((CatanParameters) gs.getGameParameters()).default_exchange_rate;
        int n_players = gs.getNPlayers();
        int currentPlayer = gs.getCurrentPlayer();

        for (int playerIndex = 0; playerIndex < n_players; playerIndex++) { // loop through players
            if (playerIndex != currentPlayer) { // exclude current player
                HashMap<CatanParameters.Resource, Counter> otherPlayerInventory = gs.getPlayerResources(playerIndex);
                for (Map.Entry<CatanParameters.Resource, Counter> e: resources.entrySet()) { // loop through current players resources to offer
                    for (CatanParameters.Resource resourceToRequest: CatanParameters.Resource.values()) { // loop through current players resources to request
                        if (resourceToRequest != e.getKey()) { // exclude the currently offered resource
                            int maxToRequest = otherPlayerInventory.get(resourceToRequest).getValue();
                            if (maxToRequest == 0)
                                continue;

                            HashMap<CatanParameters.Resource, Integer> resourcesOffered = new HashMap<>();
                            HashMap<CatanParameters.Resource, Integer> resourcesRequested = new HashMap<>();

                            // we simplify this to be aggressive in our initial bid, on the basis that we are open to a counter-offer.
                            // Effectively this creates one bid per player and possible pair of resources (which slightly reduces the combinatorial explosion of actions here)
                            int offerQuantity = Math.max(1, maxToRequest / exchangeRate); // offer at least one
                            offerQuantity = Math.min(offerQuantity, e.getValue().getValue()); // do not offer more than we have
                            resourcesOffered.put(e.getKey(), offerQuantity);
                            resourcesRequested.put(resourceToRequest, Math.min(maxToRequest, offerQuantity * exchangeRate));
                            actions.add(new OfferPlayerTrade(resourcesOffered, resourcesRequested, currentPlayer, playerIndex, 1)); // create the action
                        }
                    }
                }
            }
        }
        return actions;
    }

    static List<AbstractAction> getTradeReactionActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        OfferPlayerTrade offeredPlayerTrade = gs.getCurrentTradeOffer();

        actions.add(new EndNegotiation()); // rejects the trade offer
        if (offeredPlayerTrade.getNegotiationCount() < ((CatanParameters) gs.getGameParameters()).max_negotiation_count + 1) { // check that the maximum number of negotiations has not been exceeded to prevent AI looping
            actions.addAll(getResponsePlayerTradeOfferActions(gs));
        }
        actions.addAll(getAcceptTradeActions(gs));

        return actions;
    }

    static List<AbstractAction> getAcceptTradeActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        OfferPlayerTrade offeredPlayerTrade = gs.getCurrentTradeOffer();

        if (gs.checkCost(offeredPlayerTrade.getResourcesRequested(), offeredPlayerTrade.otherPlayerID)) {
            actions.add(new AcceptTrade(offeredPlayerTrade.offeringPlayerID, offeredPlayerTrade.otherPlayerID,
                    offeredPlayerTrade.resourcesRequested, offeredPlayerTrade.resourcesOffered));
        }

        return actions;
    }

    /**
     * Generates PlayerTradeOffer actions that act as renegotiation on an existing trade offer
     * This function only generates actions that involve changing the quantities of the offered/requested resources for
     * single resource type trades
     *
     * @param gs
     * @return
     */
    static List<AbstractAction> getResponsePlayerTradeOfferActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        OfferPlayerTrade offeredPlayerTrade = gs.getCurrentTradeOffer();
        if (offeredPlayerTrade.otherPlayerID != gs.getCurrentPlayer())
            throw new AssertionError("We should always be alternating Offer and Counter-Offer");
        int exchangeRate = ((CatanParameters) gs.getGameParameters()).default_exchange_rate;
        int[] playerResources = gs.getPlayerResources(gs.getCurrentPlayer());
        int[] resourcesOffered = offeredPlayerTrade.getResourcesOffered();
        int[] resourcesRequested = offeredPlayerTrade.getResourcesRequested();
        int[] resourcesToOffer = new int[5];
        int[] resourcesToRequest = new int[5];
        int resourceRequestedIndex = 0;
        int resourceOfferedIndex = 0;

        for (int i = 0; i < resourcesOffered.length; i++) { // Sets the index of which resources are involved in the trade
            if (resourcesOffered[i] > 0) {
                resourceOfferedIndex = i;
            }
            if (resourcesRequested[i] > 0) {
                resourceRequestedIndex = i;
            }
        }

        int maxRequest = gs.getPlayerResources(offeredPlayerTrade.offeringPlayerID)[resourceOfferedIndex];
        // TODO: Once we have partial observability of player hands, we need to modify this to take account of uncertainty (add new type of UNKNOWN in result)
        for (int quantityAvailableToOffer = 1; quantityAvailableToOffer < playerResources[resourceRequestedIndex] + 1; quantityAvailableToOffer++) { // loop through the quantity of resources to offer
            for (int quantityAvailableToRequest = 1; quantityAvailableToRequest <= maxRequest; quantityAvailableToRequest++) { // loop to generate all possible combinations of offer for the current resource pair
                if (!(quantityAvailableToOffer == resourcesRequested[resourceRequestedIndex] && quantityAvailableToRequest == resourcesOffered[resourceOfferedIndex])) {
                    resourcesToOffer[resourceRequestedIndex] = quantityAvailableToOffer;
                    resourcesToRequest[resourceOfferedIndex] = quantityAvailableToRequest;
                    if (!(Arrays.equals(resourcesToOffer, resourcesRequested) && Arrays.equals(resourcesToRequest, resourcesOffered))) { // ensures the trade offer is not the same as the existing trade offer
                        actions.add(new OfferPlayerTrade(resourcesToOffer.clone(), resourcesToRequest.clone(), offeredPlayerTrade.getOtherPlayerID(), offeredPlayerTrade.getOfferingPlayerID(), offeredPlayerTrade.getNegotiationCount() + 1)); // create the action
                    }
                    Arrays.fill(resourcesToOffer, 0);
                    Arrays.fill(resourcesToRequest, 0);
                }
            }
        }


        return actions;
    }

    static List<AbstractAction> getStealActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int[] stealingFrom = {0, 0, 0, 0};
        CatanTile[][] board = gs.getBoard();
        for (CatanTile[] catanTiles : board) {
            for (CatanTile tile : catanTiles) {
                if (tile.hasRobber()) {
                    Settlement[] settlements = tile.getSettlements();
                    for (Settlement settlement : settlements) {
                        if (settlement.getOwner() != -1 && settlement.getOwner() != gs.getCurrentPlayer() && stealingFrom[settlement.getOwner()] == 0) {
                            stealingFrom[settlement.getOwner()] = 1;
                            actions.add(new StealResource(settlement.getOwner()));
                        }
                    }
                }
            }
        }
        if (actions.size() == 0) {
            actions.add(new DoNothing());
        }
        return actions;
    }

    /**
     * Calculates regular Player actions in Buy stage
     *
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getBuildStageActions(CatanGameState gs) {
        CatanTurnOrder cto = (CatanTurnOrder) gs.getTurnOrder();

        ArrayList<AbstractAction> actions = new ArrayList<>();
        actions.add(new DoNothing());
        actions.addAll(getBuyActions(gs));

        if (!cto.isDevelopmentCardPlayed()) {
            actions.addAll(getDevCardActions(gs));
        }

        return actions;
    }

    static List<AbstractAction> getDiscardActions(CatanGameState gs) {
        final int DISCARD_COMBINATION_LIMIT = 20;
        ArrayList<AbstractAction> actions = new ArrayList<>();

        int deckSize = 0;
        for (Map.Entry<CatanParameters.Resource, Counter> e: gs.playerResources[gs.getCurrentPlayer()].entrySet()) {
            deckSize += e.getValue().getValue();
        }
        if (deckSize <= ((CatanParameters) gs.getGameParameters()).max_cards_without_discard) {
            actions.add(new DoNothing());
            return actions;
        } else {
            int r = deckSize / 2; // remove half of the resources
            if (deckSize < DISCARD_COMBINATION_LIMIT) {
                int[] resources = new int[5];  // todo put how many in order
                List<int[]> combinations = new ArrayList<>();
                //TODO identify which combinations method is faster
//                for( int brickIndex = 0; brickIndex <= resources[0]; brickIndex++){
//                    if(brickIndex == r){
//                        combinations.add(new int[]{brickIndex, 0, 0, 0, 0});
//                    } else if(brickIndex > r){
//                        break;
//                    }
//                    for( int lumberIndex = 0; lumberIndex <= resources[1]; lumberIndex++){
//                        if(brickIndex + lumberIndex == r){
//                            combinations.add(new int[]{brickIndex, lumberIndex, 0, 0, 0});
//                        } else if(brickIndex + lumberIndex > r){
//                            break;
//                        }
//                        for( int oreIndex = 0; oreIndex <= resources[2]; oreIndex++){
//                            if(brickIndex + lumberIndex + oreIndex == r){
//                                combinations.add(new int[]{brickIndex, lumberIndex, oreIndex, 0, 0});
//                            } else if(brickIndex + lumberIndex + oreIndex > r){
//                                break;
//                            }
//                            for( int grainIndex = 0; grainIndex <= resources[3]; grainIndex++){
//                                if(brickIndex + lumberIndex + oreIndex + grainIndex == r){
//                                    combinations.add(new int[]{brickIndex, lumberIndex, oreIndex, grainIndex, 0});
//                                } else if(brickIndex + lumberIndex + oreIndex + grainIndex > r){
//                                    break;
//                                }
//                                for( int woolIndex = 0; woolIndex <= resources[4]; woolIndex++){
//                                    if(brickIndex + lumberIndex + oreIndex + grainIndex + woolIndex == r){
//                                        combinations.add(new int[]{brickIndex, lumberIndex, oreIndex, grainIndex, woolIndex});
//                                    } else if(brickIndex + lumberIndex + oreIndex + grainIndex + woolIndex > r){
//                                        break;
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }

                for (int brickIndex = resources[0]; brickIndex >= 0; brickIndex--) {
                    if (brickIndex == r) {
                        combinations.add(new int[]{brickIndex, 0, 0, 0, 0});
                    }
                    if (brickIndex >= r) {
                        continue;
                    }
                    for (int lumberIndex = resources[1]; lumberIndex >= 0; lumberIndex--) {
                        if (brickIndex + lumberIndex == r) {
                            combinations.add(new int[]{brickIndex, lumberIndex, 0, 0, 0});
                        }
                        if (brickIndex + lumberIndex >= r) {
                            continue;
                        }
                        for (int oreIndex = resources[2]; oreIndex >= 0; oreIndex--) {
                            if (brickIndex + lumberIndex + oreIndex == r) {
                                combinations.add(new int[]{brickIndex, lumberIndex, oreIndex, 0, 0});
                            }
                            if (brickIndex + lumberIndex + oreIndex >= r) {
                                continue;
                            }
                            for (int grainIndex = resources[3]; grainIndex >= 0; grainIndex--) {
                                if (brickIndex + lumberIndex + oreIndex + grainIndex == r) {
                                    combinations.add(new int[]{brickIndex, lumberIndex, oreIndex, grainIndex, 0});
                                }
                                if (brickIndex + lumberIndex + oreIndex + grainIndex >= r) {
                                    continue;
                                }
                                for (int woolIndex = resources[4]; woolIndex >= 0; woolIndex--) {
                                    if (brickIndex + lumberIndex + oreIndex + grainIndex + woolIndex == r) {
                                        combinations.add(new int[]{brickIndex, lumberIndex, oreIndex, grainIndex, woolIndex});
                                    } else if (brickIndex + lumberIndex + oreIndex + grainIndex + woolIndex < r) {
                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
                CatanParameters.Resource[] values = CatanParameters.Resource.values();
                for (int[] combination : combinations) {
                    CatanParameters.Resource[] cardsToDiscard = new CatanParameters.Resource[r];
                    int counter = 0;
                    for (int i = 0; i < combination.length; i++) {
                        for (int k = 0; k < combination[i]; k++) {
                            cardsToDiscard[counter] = values[i];
                            counter++;
                        }
                    }
                    actions.add(new DiscardCards(cardsToDiscard, gs.getCurrentPlayer()));
                }
            } else {
                // Current solution to memory issue, random picks cards to discard if player has over DISCARD_COMBINATION_LIMIT
                Random rnd = new Random();
                CatanParameters.Resource[] cardsToDiscard = new CatanParameters.Resource[r];
                int[] combination = new int[r];
                for (int i = 0; i < combination.length; i++) {
                    boolean comb_not_set = true;
                    while (comb_not_set) {
                        int temp = rnd.nextInt(deckSize);
                        if (Arrays.stream(combination).sequential().noneMatch(value -> value == temp)) {
                            combination[i] = temp;
                            comb_not_set = false;
                        }
                    }
                }
                for (int i = 0; i < r; i++) {
                    cardsToDiscard[i] = CatanParameters.Resource.valueOf(playerResourceDeck.get(combination[i]).getProperty(CatanConstants.cardType).toString());
                }
                actions.add(new DiscardCards(cardsToDiscard, gs.getCurrentPlayer()));
            }
        }
        return actions;
    }

    /**
     * Returns all the possible index combination of an array.
     *
     * @param data        - a list of arrays that gets overwritten and contains the indices
     * @param buffer      - current buffer of indices
     * @param bufferIndex - current index in buffer
     * @param dataIndex   - current index in data
     * @param dataLength  - length of the original data array
     * @param r           - number of entries to return per array
     */
    static void getCombination(List<int[]> data, int buffer[], int bufferIndex, int dataIndex, int dataLength, int r) {
        // buffer is ready to be added to data
        if (bufferIndex == r) {
            data.add(buffer.clone());
            return;
        }
        // no more elements to add to the buffer
        if (dataIndex >= dataLength) {
            return;
        }
        buffer[bufferIndex] = dataIndex;
        // iterate with including current bufferIndex
        getCombination(data, buffer, bufferIndex + 1, dataIndex + 1, dataLength, r);
        // iterate with excluding current bufferIndex
        getCombination(data, buffer, bufferIndex, dataIndex + 1, dataLength, r);
    }

    /**
     * Calculates player's actions when robber is active
     *
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getRobberActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                if (!(tile.getTileType().equals(CatanTile.TileType.SEA)))
                    actions.add(new MoveRobber(x, y));
            }
        }

        return actions;
    }

    // --------------- Helper functions to keep main functions simpler -----------------

    /* Function that lists all buy actions to the player; building road, settlement or city or buying a development card */
    public static List<AbstractAction> getBuyActions(CatanGameState gs) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int turnStep = ((CatanTurnOrder) gs.getTurnOrder()).turnStep;
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList<>();

        // find possible roads, settlements and city upgrades and propose them as actions
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
                    Settlement settlement = tile.getSettlements()[i];

                    // where it is legal to place tile then it can be placed from there
                    if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
                            && gs.checkSettlementPlacement(settlement, gs.getCurrentPlayer())) {
                        if (gs.checkCost(catanParameters.costMapping.get(CatanParameters.ActionType.Settlement), activePlayer)
                                && !gs.playerTokens[activePlayer].get(CatanParameters.ActionType.Settlement).isMaximum()) {
                            actions.add(new BuildSettlement(x, y, i, activePlayer, false));
                        }
                    }

                    if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
                            && gs.checkRoadPlacement(i, tile, gs.getCurrentPlayer())) {
                        if (gs.checkCost(catanParameters.costMapping.get(CatanParameters.ActionType.Road), activePlayer)
                                && !gs.playerTokens[activePlayer].get(CatanParameters.ActionType.Road).isMaximum()) {
                            actions.add(new BuildRoad(x, y, i, activePlayer, false));
                        }
                    }

                    if (settlement.getOwner() == activePlayer && settlement.getType() == 1) {
                        if (gs.checkCost(catanParameters.costMapping.get(CatanParameters.ActionType.City), activePlayer)
                                && !gs.playerTokens[activePlayer].get(CatanParameters.ActionType.City).isMaximum()) {
                            actions.add(new BuildCity(x, y, i, activePlayer));
                        }
                    }
                }
            }
        }
        if (gs.checkCost(catanParameters.costMapping.get(CatanParameters.ActionType.DevCard), activePlayer)
                && gs.devCards.getSize() > 0) {
            actions.add(new BuyDevelopmentCard());
        }
        return actions;
    }

    public static List<AbstractAction> getDevCardActions(CatanGameState gs) {
        // Player can buy dev card and play one
        ArrayList<AbstractAction> actions = new ArrayList<>();
        boolean knightCard = false;
        boolean monopolyCard = false;
        boolean yearOfPlentyCard = false;
        boolean roadBuildingCard = false;

        // get playerHand; for each card add a new action
        Deck<CatanCard> playerDevDeck = gs.playerDevCards[gs.getCurrentPlayer()];

        for (CatanCard c : playerDevDeck.getComponents()) {
            // avoid playing a card that has been bought in the same turn
            if (c.turnCardWasBought == gs.getTurnOrder().getTurnCounter()) {
                continue;
            }
            // victory points are automatically revealed once a player has 10+ points
            if (c.cardType == CatanCard.CardType.KNIGHT_CARD) {
                if (knightCard) {
                    continue;
                } else {
                    knightCard = true;
                    actions.add(new PlayKnightCard());
                }
            }
            if (c.cardType == CatanCard.CardType.MONOPOLY) {
                if (monopolyCard) {
                    continue;
                } else {
                    monopolyCard = true;
                    for (CatanParameters.Resource resource : CatanParameters.Resource.values()) {
                        actions.add(new Monopoly(resource));
                    }
                }
            }
            if (c.cardType == CatanCard.CardType.YEAR_OF_PLENTY) {
                if (yearOfPlentyCard) {
                    continue;
                } else {
                    yearOfPlentyCard = true;
                    Deck<Card> resourceDeck = (Deck<Card>) gs.getComponent(CatanConstants.resourceDeckHash);
                    CatanParameters.Resource[] resources = CatanParameters.Resource.values();
                    ArrayList<CatanParameters.Resource> resourcesAvailable = new ArrayList<>();

                    for (int i = 0; i < resources.length; i++) {
                        int index = i;
                        Optional<Card> resource = resourceDeck.stream().filter(card -> card.getProperty(CatanConstants.cardType).toString().equals(resources[index].toString())).findFirst();
                        if (resource.isPresent()) resourcesAvailable.add(resources[i]);
                    }

                    for (CatanParameters.Resource resource1 : resourcesAvailable) {
                        for (CatanParameters.Resource resource2 : resourcesAvailable) {
                            actions.add(new YearOfPlenty(resource1, resource2));
                        }
                    }
                }
            }
            if (c.cardType == CatanCard.CardType.ROAD_BUILDING) {
                if (roadBuildingCard) {
                    continue;
                } else {
                    roadBuildingCard = true;
                    // Identify all possible roads to build
                    CatanTile[][] board = gs.getBoard();
                    for (int x = 0; x < board.length; x++) {
                        for (int y = 0; y < board[x].length; y++) {
                            CatanTile tile = board[x][y];
                            for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
                                if (!(tile.getTileType().equals(CatanTile.TileType.SEA) || tile.getTileType().equals(CatanTile.TileType.DESERT))
                                        && gs.checkRoadPlacement(i, tile, gs.getCurrentPlayer())
                                        && !gs.playerTokens[gs.getCurrentPlayer()].get(CatanParameters.ActionType.Road).isMaximum()) {
                                    actions.add(new BuildRoad(x, y, i, gs.getCurrentPlayer(), true));
                                }
                            }
                        }
                    }
                }
            }
        }

        return actions;
    }

    public static List<AbstractAction> getTradeActions(CatanGameState gs) {
        // Player can buy dev card and play one
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = gs.getCurrentPlayer();

        // default trade
        HashMap<CatanParameters.Resource, Counter> playerExchangeRate = gs.getExchangeRates(player);
        for (Map.Entry<CatanParameters.Resource, Counter> res: gs.playerResources[player].entrySet()) {
            if (res.getValue().getValue() >= playerExchangeRate.get(res.getKey()).getValue()) {
                for (CatanParameters.Resource res2: CatanParameters.Resource.values()) {
                    if (res.getKey() != res2) {
                        // list all possible trades with the bank / harbours
                        actions.add(new DefaultTrade(res.getKey(), res2, playerExchangeRate.get(res.getKey()).getValue()));
                    }
                }
            }
        }


        return actions;
    }
}

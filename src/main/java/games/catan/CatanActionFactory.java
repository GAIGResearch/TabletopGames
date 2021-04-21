package games.catan;

import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Card;
import core.components.Deck;
import games.catan.actions.*;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;
import games.catan.CatanParameters.Resources;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static core.CoreConstants.playerHandHash;
import static games.catan.CatanConstants.resourceDeckHash;

public class CatanActionFactory {
    /**
     * Calculates setup actions
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getSetupActions(CatanGameState gs) {
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int turnStep = ((CatanTurnOrder) gs.getTurnOrder()).turnStep;
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList();

        // find possible settlement locations and propose them as actions
        CatanTile[][] board = gs.getBoard();
        if (turnStep == 0) {
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    for (int i = 0; i < 6; i++) {
                        Settlement settlement = tile.getSettlements()[i];

                        // where it is legal to place tile then it can be placed from there
                        if (settlement.getOwner() == -1 &&
                                !(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))) {
//                            actions.add(new BuildSettlement_v2(settlement, activePlayer));
//                            actions.add(new BuildSettlement(x, y, i, activePlayer));
                            if (checkSettlementPlacement(settlement, gs)){
                                actions.add(new PlaceSettlementWithRoad(new BuildSettlement(x, y, i, activePlayer), new BuildRoad(x, y, i, activePlayer)));

                            }
                        }
                    }
                }
            }
        }

        return actions;
    }

    /**
     * Calculates regular Player actions in the Trade stage
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getTradeStageActions(CatanGameState gs) {
        CatanTurnOrder cto = (CatanTurnOrder)gs.getTurnOrder();

        ArrayList<AbstractAction> actions = new ArrayList();
        actions.add(new DoNothing());
        actions.addAll(getTradeActions(gs));
        actions.addAll(getPlayerTradeOfferActions(gs));
        if (!cto.isDevelopmentCardPlayed()){
            actions.addAll(getDevCardActions(gs));
        }

        return actions;
    }

    /**
     * Generates PlayerTradeOffers relating to single type trades
     * i.e Lumber for Grain, Brick for Stone
     * @param gs
     * @return - ArrayList, PlayerTradeOffer type (unique).
     */
    static List<AbstractAction> getPlayerTradeOfferActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int[] resources = gs.getPlayerResources(gs.getCurrentPlayer());
        int exchangeRate = ((CatanParameters)gs.getGameParameters()).default_exchange_rate;
        int n_players = ((CatanParameters)gs.getGameParameters()).n_players;
        int currentPlayer = gs.getCurrentPlayer();
        int[] resourcesOffered = new int[5];
        int[] resourcesRequested = new int[5];

        for (int playerIndex = 0; playerIndex < n_players; playerIndex++){ // loop through players
            if(playerIndex != currentPlayer){ // exclude current player
                for (int resourceToOfferIndex = 0; resourceToOfferIndex < resources.length; resourceToOfferIndex++ ){ // loop through current players resources to offer
                    if(resources[resourceToOfferIndex] > 0){ // don't continue if the player has none of the current resource
                        for (int resourceToRequestIndex = 0;  resourceToRequestIndex < resources.length; resourceToRequestIndex++){ // loop through current players resources to request
                            if (resourceToRequestIndex != resourceToOfferIndex){ // exclude the currently offered resource
                                for (int quantityAvailableToOfferIndex = 1; quantityAvailableToOfferIndex < resources[resourceToOfferIndex] + 1; quantityAvailableToOfferIndex++ ){ // loop through the quantity of resources to offer
                                    for (int quantityAvailableToRequestIndex = 1; quantityAvailableToRequestIndex < (exchangeRate * quantityAvailableToOfferIndex) - (quantityAvailableToOfferIndex - 1); quantityAvailableToRequestIndex++){ // loop to generate all possible combinations of offer for the current resource pair
                                        for (int quantityToOffer = 1; quantityToOffer < (quantityAvailableToOfferIndex + 1); quantityToOffer++) { // add the amount of resources to offer to the list
                                            resourcesOffered[resourceToOfferIndex]++;
                                        }
                                        for (int quantityToRequest = 1; quantityToRequest < (quantityAvailableToRequestIndex + 1); quantityToRequest++){ // add the amount of resources to request to the list
                                            resourcesRequested[resourceToRequestIndex]++;
                                        }
                                        actions.add(new OfferPlayerTrade(resourcesOffered.clone(), resourcesRequested.clone(), currentPlayer, playerIndex)); // create the action
                                        Arrays.fill(resourcesOffered,0);
                                        Arrays.fill(resourcesRequested,0);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return  actions;
    }

    static List<AbstractAction> getTradeReactionActions (CatanGameState gs){
        ArrayList<AbstractAction> actions = new ArrayList();
        OfferPlayerTrade offeredPlayerTrade = gs.getCurrentTradeOffer();

        actions.add(new DoNothing()); // rejects the trade offer
        if (offeredPlayerTrade.getNegotiationCount() < ((CatanParameters)gs.getGameParameters()).max_negotiation_count + 1){ // check that the maximum number of negotiations has not been exceeded to prevent AI looping
            actions.addAll(getResponsePlayerTradeOfferActions(gs));
        }
        actions.addAll(getAcceptTradeActions(gs));

        return actions;
    }

    static List<AbstractAction> getAcceptTradeActions(CatanGameState gs){
        ArrayList<AbstractAction> actions = new ArrayList();
        OfferPlayerTrade offeredPlayerTrade = gs.getCurrentTradeOffer();
        int[] resources = gs.getPlayerResources(gs.getCurrentPlayer());

        if(CatanGameState.checkCost(resources, offeredPlayerTrade.getResourcesRequested())){
            actions.add(new AcceptTrade(offeredPlayerTrade));
        }

        return actions;
    }

    /**
     * Generates PlayerTradeOffer actions that act as renegotiation on an existing trade offer
     * This function only generates actions that involve changing the quantities of the offered/requested resources for
     * single resource type trades
     * @param gs
     * @return
     */
    static List<AbstractAction> getResponsePlayerTradeOfferActions (CatanGameState gs){
        ArrayList<AbstractAction> actions = new ArrayList();
        OfferPlayerTrade offeredPlayerTrade = gs.getCurrentTradeOffer();
        int exchangeRate = ((CatanParameters)gs.getGameParameters()).default_exchange_rate;
        int[] playerResources = gs.getPlayerResources(gs.getCurrentPlayer());
        int[] resourcesOffered = offeredPlayerTrade.getResourcesOffered();
        int[] resourcesRequested = offeredPlayerTrade.getResourcesRequested();
        int[] resourcesToOffer = new int[5];
        int[] resourcesToRequest = new int[5];
        int resourceRequestedIndex = 0;
        int resourceOfferedIndex = 0;

        for (int i = 0; i < resourcesOffered.length; i++){ // Sets the index of which resources are involved in the trade
            if (resourcesOffered[i]>0){
                resourceOfferedIndex=i;
            }
            if (resourcesRequested[i]>0){
                resourceRequestedIndex=i;
            }
        }

        for (int quantityAvailableToOfferIndex = 1; quantityAvailableToOfferIndex < playerResources[resourceRequestedIndex] + 1; quantityAvailableToOfferIndex++ ){ // loop through the quantity of resources to offer
            for (int quantityAvailableToRequestIndex = 1; quantityAvailableToRequestIndex < (exchangeRate * quantityAvailableToOfferIndex) - (quantityAvailableToOfferIndex - 1); quantityAvailableToRequestIndex++){ // loop to generate all possible combinations of offer for the current resource pair
                    if(!(quantityAvailableToOfferIndex == resourcesRequested[resourceRequestedIndex] && quantityAvailableToRequestIndex == resourcesOffered[resourceOfferedIndex])) {
                        resourcesToOffer[resourceRequestedIndex]+=quantityAvailableToOfferIndex; // add the amount of resources to offer to the list
                        resourcesToRequest[resourceOfferedIndex]+=quantityAvailableToRequestIndex; // add the amount of resources to request to the list
                        if (!(resourcesToOffer.equals(resourcesRequested) && resourcesToRequest.equals(resourcesOffered))) { // ensures the trade offer is not the same as the existing trade offer
                            actions.add(new OfferPlayerTrade(resourcesToOffer.clone(), resourcesToRequest.clone(), offeredPlayerTrade.getOtherPlayerID(), offeredPlayerTrade.getOfferingPlayerID(), offeredPlayerTrade.getNegotiationCount())); // create the action
                        }
                        Arrays.fill(resourcesToOffer, 0);
                        Arrays.fill(resourcesToRequest, 0);
                    }
                }
            }


        return actions;
    }

    static List<AbstractAction> getStealActions(CatanGameState gs){
        ArrayList<AbstractAction> actions = new ArrayList();
        int[] stealingFrom = {0,0,0,0};
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                if (tile.hasRobber()){
                    Settlement[] settlements = tile.getSettlements();
                    for (int i = 0; i < settlements.length; i++){
                        if (settlements[i].getOwner() != -1 && settlements[i].getOwner() != gs.getCurrentPlayer() && stealingFrom[settlements[i].getOwner()]==0){
                            stealingFrom[settlements[i].getOwner()]=1;
                            actions.add(new StealResource(settlements[i].getOwner()));
                        }
                    }
                }
            }
        }
        if (actions.size() == 0){
            actions.add(new DoNothing());
        }
        return actions;
    }

    /**
     * Calculates regular Player actions in Buy stage
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getBuildStageActions(CatanGameState gs) {
        CatanTurnOrder cto = (CatanTurnOrder)gs.getTurnOrder();

        ArrayList<AbstractAction> actions = new ArrayList();
        actions.add(new DoNothing());
        actions.addAll(getBuyActions(gs));

        if (!cto.isDevelopmentCardPlayed()){
            actions.addAll(getDevCardActions(gs));
        }

        return actions;
    }

    static List<AbstractAction> getDiscardActions(CatanGameState gs){
        final int DISCARD_COMBINATION_LIMIT = 13;
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<Card> playerResourceDeck = (Deck<Card>)gs.getComponentActingPlayer(playerHandHash);
        Deck<Card> commonResourceDeck = (Deck<Card>)gs.getComponent(resourceDeckHash);

        int deckSize = playerResourceDeck.getSize();
        if (deckSize <= ((CatanParameters)gs.getGameParameters()).max_cards_without_discard){
            actions.add(new DoNothing());
            return actions;
        } else {
            if(deckSize < DISCARD_COMBINATION_LIMIT){
                // list all the combinations
                int n = playerResourceDeck.getSize();
                int r = n / 2; // remove half of the resources

                List<int[]> results = new ArrayList<>();
                // todo limit number of actions when too many cards are in player's hand
                System.out.println("Discarding " + r + " card from " + n + "cards");
                getCombination(results, new int[r], 0, 0, n, r);
                for (int[] result: results){
                    ArrayList<Card> cardsToDiscard = new ArrayList<>();
                    for (int i = 0; i < result.length; i++){
                        cardsToDiscard.add(playerResourceDeck.get(result[i]));
                    }
                    actions.add(new DiscardCards(cardsToDiscard, gs.getCurrentPlayer()));
                }
            } else {
                // Current solution to memory issue, random picks cards to discard if player has over DISCARD_COMBINATION_LIMIT
                ArrayList<Card> cardsToDiscard = new ArrayList<>();
                Random rnd = new Random();
                for (int i = 0; i < deckSize/2; i++){
                    cardsToDiscard.add(playerResourceDeck.get(rnd.nextInt(deckSize)));
                }
                actions.add(new DiscardCards(cardsToDiscard, gs.getCurrentPlayer()));
            }
        }
        return actions;
    }

    /**
     * Returns all the possible index combination of an array.
     * @param data - a list of arrays that gets overwritten and contains the indices
     * @param buffer - current buffer of indices
     * @param bufferIndex - current index in buffer
     * @param dataIndex - current index in data
     * @param dataLength - length of the original data array
     * @param r - number of entries to return per array
     */
    static void getCombination(List<int[]> data, int buffer[], int bufferIndex, int dataIndex, int dataLength, int r){
        // buffer is ready to be added to data
        if (bufferIndex == r){
            data.add(buffer.clone());
            return;
        }
        // no more elements to add to the buffer
        if (dataIndex >= dataLength){
            return;
        }
        buffer[bufferIndex] = dataIndex;
        // iterate with including current bufferIndex
        getCombination(data, buffer, bufferIndex+1, dataIndex+1, dataLength, r);
        // iterate with excluding current bufferIndex
        getCombination(data, buffer, bufferIndex, dataIndex+1, dataLength, r);
    }

    /**
     * Calculates player's actions when robber is active
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getRobberActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList();
        if (gs.getRollValue() == 7){
            CatanTile[][] board = gs.getBoard();
            for (int x = 0; x < board.length; x++) {
                for (int y = 0; y < board[x].length; y++) {
                    CatanTile tile = board[x][y];
                    if (!(tile.getType().equals(CatanParameters.TileType.SEA)))
                        actions.add(new MoveRobber(x, y));
                }
            }
        }

        return actions;
    }

    public static boolean checkSettlementPlacement(Settlement settlement, CatanGameState gs){
        // checks if any of the neighbouring settlements are already taken (distance rule)
        // if yes returns false otherwise true

        // if settlement is taken then cannot replace it
        if (settlement.getOwner() != -1){
            return false;
        }

        // check if there is a settlement one distance away
        Graph<Settlement, Road> graph = gs.getGraph();
        List<Settlement> settlements = graph.getNeighbourNodes(settlement);
        for (Settlement settl: settlements){
            if (settl.getOwner() != -1){
                return false;
            }
        }

        List<Road> roads = graph.getConnections(settlement);
        // check first if we have a road next to the settlement owned by the player
        // Doesn't apply in the setup phase
        if (!gs.getGamePhase().equals(CatanGameState.CatanGamePhase.Setup)){
            for (Road road : roads) {
                if (road.getOwner() == gs.getCurrentPlayer()) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    static boolean checkRoadPlacement(int roadId, CatanTile tile, CatanGameState gs){
        /*
        * @args:
        * roadId - Id of the road on tile
        * tile - tile on which we would like to build a road
        * gs - Game state */

        Graph<Settlement, Road> graph = gs.getGraph();
        Road road = tile.getRoads()[roadId];

        // check if road is already taken
        if (road.getOwner() != -1){
            return false;
        }
        // check if there is our settlement along edge
        int playerId = gs.getCurrentPlayer();
        Settlement settl1 = tile.getSettlements()[roadId];
        Settlement settl2 = tile.getSettlements()[(roadId+1)%6];
        if (settl1.getOwner() == playerId || settl2.getOwner() == playerId){
            return true;
        }

        // check if there is a road on a neighbouring edge
        List<Road> roads = graph.getConnections(settl1);
        roads.addAll(graph.getConnections(settl2));
        for (Road rd :roads){
            if (rd.getOwner() == playerId){
                return true;
            }
        }
        return false;
    }

    // --------------- Helper functions to keep main functions simpler -----------------

    /* Function that lists all buy actions to the player; building road, settlement or city or buying a development card */
    public static List<AbstractAction> getBuyActions(CatanGameState gs){
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int turnStep = ((CatanTurnOrder) gs.getTurnOrder()).turnStep;
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        int[] resources = gs.getPlayerResources(activePlayer);
        System.out.println("Player " + gs.getCurrentPlayer() + " has " + Arrays.toString(resources));
        ArrayList<AbstractAction> actions = new ArrayList();


        // find possible roads, settlements and city upgrades and propose them as actions
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
                    Settlement settlement = tile.getSettlements()[i];

                    // where it is legal to place tile then it can be placed from there
                    if (!(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))
                            && checkSettlementPlacement(settlement, gs)) {
                        if (CatanGameState.checkCost(resources, CatanParameters.costMapping.get("settlement"))) {
                            actions.add(new BuildSettlement(x, y, i, activePlayer));
                        }
                    }

                    if (!(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))
                            && checkRoadPlacement(i, tile, gs)){
                        if (CatanGameState.checkCost(resources, CatanParameters.costMapping.get("road"))) {
                            actions.add(new BuildRoad(x, y, i, activePlayer));
                        }
                    }

                    if (settlement.getOwner() == activePlayer && settlement.getType() == 1){
                        if (CatanGameState.checkCost(resources, CatanParameters.costMapping.get("city"))) {
                            actions.add(new BuildCity(x, y, i, activePlayer));
                        }
                    }
                }
            }
        }
        if (CatanGameState.checkCost(resources, CatanParameters.costMapping.get("developmentCard")))
            actions.add(new BuyDevelopmentCard());
    return actions;
    }

    public static List<AbstractAction> getDevCardActions(CatanGameState gs){
        // Player can buy dev card and play one
        ArrayList<AbstractAction> actions = new ArrayList();

        // get playerHand; for each card add a new action
        Deck<Card> playerDevDeck = (Deck<Card>)gs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        for (Card c: playerDevDeck.getComponents()){
            // avoid playing a card that has been bought in the same turn
            if (c == gs.getBoughtDevCard()){
                continue;
            }
            // victory points are automatically revealed once a player has 10+ points
            String cardType = c.getProperty(CatanConstants.cardType).toString();
            if (cardType.equals(CatanParameters.CardTypes.KNIGHT_CARD.toString())){
                actions.add(new PlayKnightCard(c));
            }
            if (cardType.equals(CatanParameters.CardTypes.MONOPOLY.toString())){
                for (CatanParameters.Resources resource: CatanParameters.Resources.values()){
                    actions.add(new Monopoly(resource, c));
                }
            }
            if (cardType.equals(CatanParameters.CardTypes.YEAR_OF_PLENTY.toString())){
                for (CatanParameters.Resources resource1: CatanParameters.Resources.values()){
                    for (CatanParameters.Resources resource2: CatanParameters.Resources.values()) {
                        actions.add(new YearOfPlenty(resource1, resource2));
                    }
                }
            }
            if (cardType.equals(CatanParameters.CardTypes.ROAD_BUILDING.toString())){
                for (Road road: getRoadsToBuild(gs))
                    actions.add(new PlaceRoad(road, c));
            }
        }

        return actions;
    }

    public static List<Road> getRoadsToBuild(CatanGameState gs){
        CatanTile[][] board = gs.getBoard();
        ArrayList<Road> roads = new ArrayList<>();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                for (int i = 0; i < CatanConstants.HEX_SIDES; i++) {
                    if (!(tile.getType().equals(CatanParameters.TileType.SEA) || tile.getType().equals(CatanParameters.TileType.DESERT))
                            && checkRoadPlacement(i, tile, gs)){
                        roads.add(tile.getRoads()[i]);
                    }
                }
            }
        }
        return roads;
    }

    public static List<AbstractAction> getTradeActions(CatanGameState gs){
        // Player can buy dev card and play one
        ArrayList<AbstractAction> actions = new ArrayList();

        // get playerHand; for each card add a new action
        int[] resources = gs.getPlayerResources(gs.getCurrentPlayer());

        // default trade
        int playerExchangeRate[] = gs.getExchangeRates();
        for (int i = 0; i < resources.length; i++){
            if (resources[i] >= playerExchangeRate[i]){
                for (int j = 0; j < resources.length; j++){
                    if (j!=i){
                        // list all possible trades
                        actions.add(new DefaultTrade(CatanParameters.Resources.values()[i], CatanParameters.Resources.values()[j], playerExchangeRate[i]));
                    }
                }
            }
            // todo make trade offers
        }


        return actions;
    }
}

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
            actions.addAll(getCardActions(gs));
        }

        return actions;
    }

    /**
     * Calculates all possible PlayerTradeOffers
     * @param gs
     * @return - ArrayList, PlayerTradeOffer type (unique).
     */
    static List<AbstractAction> getPlayerTradeOfferActions(CatanGameState gs) {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int[] resources = gs.getPlayerResources();
        int exchange_rate = ((CatanParameters)gs.getGameParameters()).default_exchange_rate;
        int n_players = ((CatanParameters)gs.getGameParameters()).n_players;
        ArrayList<Resources> resources_offered = new ArrayList<>();
        ArrayList<Resources> resources_requested = new ArrayList<>();

        for (int player_index = 0; player_index < n_players; player_index++){ // loop through players
            if(player_index != gs.getCurrentPlayer()){ // exclude current player
                for (int resource_to_offer_index = 0; resource_to_offer_index < resources.length; resource_to_offer_index++ ){ // loop through current players resources to offer
                    if(resources[resource_to_offer_index] > 0){ // don't continue if the player has none of the current resource
                        for (int resource_to_request_index = 0;  resource_to_request_index < resources.length; resource_to_request_index++){ // loop through current players resources to request
                            if (resource_to_request_index != resource_to_offer_index){ // exclude the currently offered resource
                                for (int quantity_available_to_offer_index = 1; quantity_available_to_offer_index < resources[resource_to_offer_index] + 1; quantity_available_to_offer_index++ ){ // loop through the quantity of resources to offer
                                    for (int quantity_available_to_request_index = 1; quantity_available_to_request_index < (exchange_rate * quantity_available_to_offer_index); quantity_available_to_request_index++){ // loop to generate all possible combinations of offer for the current resource pair
                                        for (int quantity_to_offer = 1; quantity_to_offer < (quantity_available_to_offer_index + 1); quantity_to_offer++) { // add the amount of resources to offer to the list
                                            resources_offered.add(CatanParameters.Resources.values()[resource_to_offer_index]);
                                        }
                                        for (int quantity_to_request = 1; quantity_to_request < (quantity_available_to_request_index + 1); quantity_to_request++){ // add the amount of resources to request to the list
                                            resources_requested.add(CatanParameters.Resources.values()[resource_to_request_index]);
                                        }
                                        actions.add(new OfferPlayerTrade((ArrayList<Resources>)resources_offered.clone(), (ArrayList<Resources>)resources_requested.clone(), player_index)); // create the action
                                        resources_offered.clear(); // clear the offered list
                                        resources_requested.clear(); // clear the requested list
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

    static List<AbstractAction> getStealActions(CatanGameState gs){
        ArrayList<AbstractAction> actions = new ArrayList();
        CatanTile[][] board = gs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                if (tile.hasRobber()){
                    Settlement[] settlements = tile.getSettlements();
                    for (int i = 0; i < settlements.length; i++){
                        if (settlements[i].getOwner() != -1 && settlements[i].getOwner() != gs.getCurrentPlayer()){
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
            actions.addAll(getCardActions(gs));
        }

        actions.add(new DoNothing());
        actions.addAll(getBuyActions(gs));
        actions.addAll(getCardActions(gs));
        actions.addAll(getTradeActions(gs));


        return actions;
    }

    static List<AbstractAction> getDiscardActions(CatanGameState gs){
        ArrayList<AbstractAction> actions = new ArrayList<>();
        Deck<Card> playerResourceDeck = (Deck<Card>)gs.getComponentActingPlayer(playerHandHash);
        Deck<Card> commonResourceDeck = (Deck<Card>)gs.getComponent(resourceDeckHash);

        int deckSize = playerResourceDeck.getSize();
        if (deckSize <= ((CatanParameters)gs.getGameParameters()).max_cards_without_discard){
            actions.add(new DoNothing());
            return actions;
        } else{
            // list all the combinations
            int n = playerResourceDeck.getSize();
            int r = n / 2; // remove half of the resources

            List<int[]> results = new ArrayList<>();
            getCombination(results, new int[r], 0, 0, n, r);
            for (int[] result: results){
                ArrayList<Card> cardsToDiscard = new ArrayList<>();
                for (int i = 0; i < result.length; i++){
                    cardsToDiscard.add(playerResourceDeck.get(i));
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
                        actions.add(new MoveRobber(tile));
                }
            }
        }

        return actions;
    }

    static boolean checkSettlementPlacement(Settlement settlement, CatanGameState gs){
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
        int[] resources = gs.getPlayerResources();
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

    public static List<AbstractAction> getCardActions(CatanGameState gs){
        // Player can buy dev card and play one
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int turnStep = ((CatanTurnOrder) gs.getTurnOrder()).turnStep;
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList();

        // get playerHand; for each card add a new action
        Deck<Card> playerDevDeck = (Deck<Card>)gs.getComponentActingPlayer(CatanConstants.developmentDeckHash);
        for (Card c: playerDevDeck.getComponents()){
            // victory points are automatically revealed once a player has 10+ points
            String cardType = c.getProperty(CatanConstants.cardType).toString();
            if (!cardType.equals("Victory Points")){
                actions.add(new PlayDevelopmentCard(c));
            }
        }

        return actions;
    }

    public static List<AbstractAction> getTradeActions(CatanGameState gs){
        // Player can buy dev card and play one
        ArrayList<AbstractAction> actions = new ArrayList();

        // get playerHand; for each card add a new action
        int[] resources = gs.getPlayerResources();

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

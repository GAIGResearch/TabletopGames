package games.catan;

import core.CoreConstants;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.Card;
import core.components.Deck;
import games.catan.actions.*;
import games.catan.components.Graph;
import games.catan.components.Road;
import games.catan.components.Settlement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static games.catan.CatanConstants.cardType;

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
        if (!cto.isDevelopmentCardPlayed()){
            actions.addAll(getCardActions(gs));
        }

        return actions;
    }

    /**
     * Calculates regular Player actions in Buy stage
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getBuyStageActions(CatanGameState gs) {
        CatanTurnOrder cto = (CatanTurnOrder)gs.getTurnOrder();

        ArrayList<AbstractAction> actions = new ArrayList();
        actions.add(new DoNothing());
        actions.addAll(getBuyActions(gs));

        if (!cto.isDevelopmentCardPlayed()){
            actions.addAll(getCardActions(gs));
        }

        return actions;
    }

    /**
     * Calculates player's actions when robber is active
     * @return - ArrayList, various action types (unique).
     */
    static List<AbstractAction> getRobberActions(CatanGameState gs) {
        // todo
        CatanParameters catanParameters = (CatanParameters) gs.getGameParameters();
        int activePlayer = gs.getTurnOrder().getCurrentPlayer(gs);
        ArrayList<AbstractAction> actions = new ArrayList();

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
        CatanParameters params = (CatanParameters) gs.getGameParameters();
        ArrayList<AbstractAction> actions = new ArrayList();

        // get playerHand; for each card add a new action
        int[] resources = gs.getPlayerResources();

        // default trade
        for (int i = 0; i < resources.length; i++){
            if (resources[i] >= params.n_resource_to_trade){
                for (int j = 0; j < resources.length; j++){
                    if (j!=i){
                        // list all possible trades
                        actions.add(new DefaultTrade(CatanParameters.Resources.values()[i], CatanParameters.Resources.values()[j]));
                    }
                }
            }
            // todo check if there is a harbor on a sea tile next to player
            // todo make trade offers
        }


        return actions;
    }
}

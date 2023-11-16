package games.catan;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.components.Deck;
import core.interfaces.IExtendedSequence;
import games.catan.actions.build.BuildCity;
import games.catan.actions.build.BuildRoad;
import games.catan.actions.build.BuildSettlement;
import games.catan.actions.build.BuyDevelopmentCard;
import games.catan.actions.dev.*;
import games.catan.actions.discard.DiscardResources;
import games.catan.actions.discard.DiscardResourcesPhase;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.actions.trade.AcceptTrade;
import games.catan.actions.trade.DefaultTrade;
import games.catan.actions.trade.EndNegotiation;
import games.catan.actions.trade.OfferPlayerTrade;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;
import org.apache.commons.lang3.tuple.Triple;
import utilities.ActionTreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class CatanActionTree {

    static int nMaxResources = 4; // Maximum number of resources a player can trade either way


    // Helper Function for ordering Roads
    public static HashMap<Triple<Integer, Integer, Integer>, Integer> orderRoads(CatanGameState cgs) {
        Triple<Integer, Integer, Integer> road;
        HashMap<Triple<Integer, Integer, Integer>, Integer> roadMap = new HashMap<>();
        int edgeCounter = 0;
        CatanTile[][] board = cgs.getBoard();
        for (int x = 0; x < board.length; x++) {
            for (int y = 0; y < board[x].length; y++) {
                CatanTile tile = board[x][y];
                // for every edge on tile
                for (int e = 0; e < 6; e++) {
                    if (tile.getEdgeIDs()[e] != -1) {
                        road = Triple.of(x, y, e);
                        roadMap.put(road, edgeCounter);
                        edgeCounter++;
                    }
                }
            }
        }
        return roadMap;
    }

    // Big Tree Ahead
    public static ActionTreeNode initActionTree(AbstractGameState gameState) {
        CatanGameState catanGameState = (CatanGameState) gameState;

        // Setup Root and add Do Nothing action
        ActionTreeNode root = new ActionTreeNode(0, "root");
        root.addChild(0, "Do Nothing");

        // Common variables
        CatanTile[][] board = catanGameState.getBoard();
        int noPlayers = catanGameState.getNPlayers();

        // Setup Tree Branch
        // 0 - Setup
        // 1 - Tile X
        // 2 - Tile Y
        // 3 - Vertice
        // 4 - Edge
        ActionTreeNode setup = root.addChild(0, "Setup");
        for (int x = 0; x < board.length; x++) {
            ActionTreeNode tileX = setup.addChild(0, "Tile " + x);
            for (int y = 0; y < board[x].length; y++) {
                ActionTreeNode tileY = tileX.addChild(0, "Tile " + y);
                for (int v = 0; v < 6; v++) {
                    ActionTreeNode vertice = tileY.addChild(0, "Vertice " + v);
                    for (int e = 0; e < 6; e++) {
                        vertice.addChild(0, "Edge " + e);
                    }
                }
            }
        }

        // Robber / Knight Branch
        // 0 - Robber / Knight
        // 1 - Tile X
        // 2 - Tile Y
        // 3 - PlayerID
        // 4 - Target PlayerID
        ActionTreeNode robber = root.addChild(0, "Robber");
        ActionTreeNode knight = root.addChild(0, "Play Knight");
        for (int x = 0; x < board.length; x++) {
            ActionTreeNode tileX = robber.addChild(0, "Tile " + x);
            ActionTreeNode knightX = knight.addChild(0, "Tile " + x);
            for (int y = 0; y < board[x].length; y++) {
                ActionTreeNode tileY = tileX.addChild(0, "Tile " + y);
                ActionTreeNode knightY = knightX.addChild(0, "Tile " + y);
                for (int n = 0; n < noPlayers; n++) {
                    ActionTreeNode playerID = tileY.addChild(0, "Player " + n);
                    ActionTreeNode knightID = knightY.addChild(0, "Player " + n);

                    // -1 Target if no valid targets
                    playerID.addChild(0, "Target Player -1");
                    knightID.addChild(0, "Target Player -1");
                    for (int t = 0; t < noPlayers; t++) {
                        // Can't target yourself
                        if (t != n) {
                            playerID.addChild(0, "Target Player " + t);
                            knightID.addChild(0, "Target Player " + t);
                        }

                    }
                }
            }
        }

        // Build Branch
        // 0 - Build Road / City / Settlement
        // 1 - Tile X
        // 2 - Tile Y
        // 3 - Edge
        // 4 - PlayerID
        // 5 - Free (Not for build city)

        ActionTreeNode buildRoad = root.addChild(0, "Build Road");
        ActionTreeNode buildCity = root.addChild(0, "Build City");
        ActionTreeNode buildSettlement = root.addChild(0, "Build Settlement");
        ActionTreeNode[] buildBranches = {buildRoad, buildCity, buildSettlement};
        for (ActionTreeNode parentNode : buildBranches) {
            for (int x = 0; x < board.length; x++) {
                ActionTreeNode tileX = parentNode.addChild(0, "Tile " + x);
                for (int y = 0; y < board[x].length; y++) {
                    ActionTreeNode tileY = tileX.addChild(0, "Tile " + y);
                    for (int e = 0; e < 6; e++) {
                        ActionTreeNode edge = tileY.addChild(0, "N " + e);
                        for (int n = 0; n < noPlayers; n++) {
                            ActionTreeNode playerID = edge.addChild(0, "Player " + n);
                            // Free flag only for road and settlement
                            if (parentNode != buildCity) {
                                playerID.addChild(0, "Free");
                                playerID.addChild(0, "Not Free");
                            }
                        }
                    }
                }
            }
        }

        // Buy Dev Card Branch
        // 0 - Buy Dev Card
        // 1 - PlayerID
        ActionTreeNode buyDevCard = root.addChild(0, "Buy Development Card");
        for (int n = 0; n < noPlayers; n++) {
            buyDevCard.addChild(0, "Player " + n);
        }

        // Discard Resources Branch (Extended Action)
        // 0 - Discard Resources
        // 1 - Resource Type
        // 2 - PlayerID
        ActionTreeNode discardResources = root.addChild(0, "Discard Resources");
        for (CatanParameters.Resource resource : CatanParameters.Resource.values()) {

            // Check for wildcard
            if (resource != CatanParameters.Resource.WILD) {
                ActionTreeNode resourceNode = discardResources.addChild(0, resource.toString());
                for (int n = 0; n < noPlayers; n++) {
                    resourceNode.addChild(0, "Player " + n);
                }
            }

        }

        // Default Trade (e.g, with harbour) Branch
        // 0 - Default Trade
        // 1 - Offered Resource
        // 2 - Desired Resource
        // 3 - Exchange Rate (e.g, 2:1)
        // 4 - PlayerID
        ActionTreeNode defaultTrade = root.addChild(0, "Default Trade");
        for (CatanParameters.Resource offered : CatanParameters.Resource.values()) {

            // Check for wildcard
            if (offered != CatanParameters.Resource.WILD) {

                ActionTreeNode offeredNode = defaultTrade.addChild(0, offered.toString());
                for (CatanParameters.Resource desired : CatanParameters.Resource.values()) {

                    // Check for wildcard and same resource
                    if (desired != CatanParameters.Resource.WILD && desired != offered) {
                        ActionTreeNode desiredNode = offeredNode.addChild(0, desired.toString());

                        // Exchange Rate (2 - 4)
                        for (int e = 2; e <= 4; e++) {
                            ActionTreeNode exchangeNode = desiredNode.addChild(0, "Exchange Rate " + e);
                            for (int n = 0; n < noPlayers; n++) {
                                exchangeNode.addChild(0, "Player " + n);
                            }
                        }
                    }
                }
            }
        }

        // Monopoly Branch
        // 0 - Monopoly
        // 1 - Resource
        // 2 - PlayerID
        ActionTreeNode monopoly = root.addChild(0, "Play Monopoly");
        for (CatanParameters.Resource resource : CatanParameters.Resource.values()) {

            // Check for wildcard
            if (resource != CatanParameters.Resource.WILD) {
                ActionTreeNode resourceNode = monopoly.addChild(0, resource.toString());
                for (int n = 0; n < noPlayers; n++) {
                    resourceNode.addChild(0, "Player " + n);
                }
            }
        }

        // Year of Plenty Branch
        // 0 - Year of Plenty
        // 1 - Resource 1
        // 2 - Resource 2
        // 3 - PlayerID
        ActionTreeNode yop = root.addChild(0, "Play Year of Plenty");
        for (CatanParameters.Resource resource1 : CatanParameters.Resource.values()) {

            // Check for wildcard
            if (resource1 != CatanParameters.Resource.WILD) {
                ActionTreeNode resource1Node = yop.addChild(0, resource1.toString());
                for (CatanParameters.Resource resource2 : CatanParameters.Resource.values()) {
                    // Check for wildcard
                    if (resource2 != CatanParameters.Resource.WILD) {
                        ActionTreeNode resource2Node = resource1Node.addChild(0, resource2.toString());
                        for (int n = 0; n < noPlayers; n++) {
                            resource2Node.addChild(0, "Player " + n);
                        }
                    }
                }
            }
        }

        // Road Building (Dev Card) Branch
        // 0 - Road Building
        // 2 - Road 1

        HashMap<Triple<Integer, Integer, Integer>, Integer> roadMap = orderRoads(catanGameState);
        ActionTreeNode roadBuilding = root.addChild(0, "Play Road Building");
        for (int i : roadMap.values()) {
            roadBuilding.addChild(0, "Road " + i);
        }

        // Player Trade Offer
        // 0 - OfferPlayerTrade
        // 1 - Stage
        // 2 - Resource Offered
        // 3 - Amount Offered (set at 1 - 4 for now, can be changed)
        // 4 - Resource Requested
        // 5 - Amount Requested (set at 1 - 4 for now, can be changed)
        // 6 - Offering Player
        // 7 - Target Player
        ActionTreeNode offerPlayerTradeNode = root.addChild(0, "OfferPlayerTrade");

        // For each trading stage
        for (OfferPlayerTrade.Stage stage : OfferPlayerTrade.Stage.values()) {
            ActionTreeNode stageNode = offerPlayerTradeNode.addChild(0, stage.toString());

            // For each resource offered
            for (CatanParameters.Resource resourceOffered : CatanParameters.Resource.values()) {
                // Check for wildcard
                if (resourceOffered != CatanParameters.Resource.WILD) {
                    ActionTreeNode resourceOfferedNode = stageNode.addChild(0, resourceOffered.toString());

                    // For each amount offered
                    for (int nOffered = 1; nOffered <= nMaxResources; nOffered++) {
                        ActionTreeNode nOfferedNode = resourceOfferedNode.addChild(0, "Amount Offered " + nOffered);

                        // For each resource requested
                        for (CatanParameters.Resource resourceRequested : CatanParameters.Resource.values()) {
                            // Check for wildcard and make sure its not the same resource
                            if (resourceRequested != CatanParameters.Resource.WILD && resourceRequested != resourceOffered) {
                                ActionTreeNode resourceRequestedNode = nOfferedNode.addChild(0, resourceRequested.toString());

                                // For each amount requested
                                for (int nRequested = 1; nRequested <= nMaxResources; nRequested++) {
                                    ActionTreeNode nRequestedNode = resourceRequestedNode.addChild(0, "Amount Requested " + nRequested);

                                    // For possible player combinations
                                    for (int offeringPlayerID = 0; offeringPlayerID < noPlayers; offeringPlayerID++) {
                                        ActionTreeNode offeringPlayerIDNode = nRequestedNode.addChild(0, "Offering Player " + offeringPlayerID);
                                        for (int targetPlayerID = 0; targetPlayerID < noPlayers; targetPlayerID++) {

                                            // Can't trade with yourself
                                            if (offeringPlayerID != targetPlayerID) {
                                                offeringPlayerIDNode.addChild(0, "Target Player " + targetPlayerID);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // End Trade Branch
        // 0 - End Trade
        // 1 - Offering PlayerID
        ActionTreeNode endTradeNode = root.addChild(0, "End Trade");
        for (int offeringPlayerID = 0; offeringPlayerID < noPlayers; offeringPlayerID++) {
            endTradeNode.addChild(0, "Offering Player " + offeringPlayerID);
        }

        // Accept Trade Branch
        // 0 - Accept Trade
        // 2 - Offering PlayerID
        // 3 - Other PlayerID
        // 3 - Resource Requested
        // 4 - Amount Requested
        // 5 - Resource Offered
        // 6 - Amount Offered
        ActionTreeNode acceptTradeNode = root.addChild(0, "Accept Trade");

        for (int offeringPlayerID = 0; offeringPlayerID < noPlayers; offeringPlayerID++) {
            ActionTreeNode offeringPlayerIDNode = acceptTradeNode.addChild(0, "Offering Player " + offeringPlayerID);
            for (int otherPlayerID = 0; otherPlayerID < noPlayers; otherPlayerID++) {

                // Can't trade with yourself
                if (offeringPlayerID != otherPlayerID) {
                    ActionTreeNode otherPlayerIDNode = offeringPlayerIDNode.addChild(0, "Other Player " + otherPlayerID);

                    // For resources requested
                    for (CatanParameters.Resource resourceRequested : CatanParameters.Resource.values()) {
                        // Check for wildcard
                        if (resourceRequested != CatanParameters.Resource.WILD) {
                            ActionTreeNode resourceRequestedNode = otherPlayerIDNode.addChild(0, resourceRequested.toString());
                            for (int nRequested = 1; nRequested <= nMaxResources; nRequested++) {
                                ActionTreeNode nRequestedNode = resourceRequestedNode.addChild(0, "Amount Requested " + nRequested);

                                // For resources offered
                                for (CatanParameters.Resource resourceOffered : CatanParameters.Resource.values()) {
                                    // Check for wildcard and make sure its not the same resource
                                    if (resourceOffered != CatanParameters.Resource.WILD && resourceOffered != resourceRequested) {
                                        ActionTreeNode resourceOfferedNode = nRequestedNode.addChild(0, resourceOffered.toString());
                                        for (int nOffered = 1; nOffered <= nMaxResources; nOffered++) {
                                            resourceOfferedNode.addChild(0, "Amount Offered " + nOffered);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return root;
    }

    public static ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        boolean reached = false;
        root.resetTree();
        CatanGameState catanGameState = (CatanGameState) gameState;
        CatanParameters catanParameters = (CatanParameters) gameState.getGameParameters();
        int playerID = catanGameState.getCurrentPlayer();

        // If in extended action sequence
        if (catanGameState.isActionInProgress()) {

            // If in discard phase extended action
            // Update discard branch
            if (catanGameState.getActionsInProgress().peek() instanceof DiscardResourcesPhase) {
                ActionTreeNode discard = root.findChildrenByName("Discard Resources", true);
                for (CatanParameters.Resource resource : CatanParameters.Resource.values()) {

                    // Check for wildcard
                    if (resource != CatanParameters.Resource.WILD) {
                        ActionTreeNode resourceNode = discard.findChildrenByName(resource.toString(), true);
                        ActionTreeNode playerNode = resourceNode.findChildrenByName("Player " + playerID, true);
                        playerNode.setAction(new DiscardResources(new CatanParameters.Resource[]{resource}, playerID));
                    }
                }
            }

            // If in Deep Road Building Extended Action
            if (catanGameState.getActionsInProgress().peek() instanceof DeepRoadBuilding) {
                DeepRoadBuilding extendedAction = (DeepRoadBuilding) catanGameState.getActionsInProgress().peek();
                ActionTreeNode roadBuilding = root.findChildrenByName("Play Road Building", true);
                HashMap<Triple<Integer, Integer, Integer>, Integer> roadMap = orderRoads(catanGameState);

                // Find valid roads to build
                List<AbstractAction> buildActions = extendedAction._computeAvailableActions(catanGameState);
                for (AbstractAction action : buildActions) {
                    PlayRoadBuilding playRoadBuildingAction = (PlayRoadBuilding) action;
                    BuildRoad road = (BuildRoad) playRoadBuildingAction.roadsToBuild[0];
                    ActionTreeNode road1Node = roadBuilding.findChildrenByName("Road " + roadMap.get(Triple.of(road.x, road.y, road.edge)), true);
                    road1Node.setAction(action);
                }
                int p = 4;
            }
            Stack<IExtendedSequence> actions = catanGameState.getActionsInProgress();
            actions.peek();
        }

        // Not in extended action sequence
        // Update tree normally
        else {

            // In Setup Phase, Update Setup Branch
            if (catanGameState.getGamePhase() == CatanGameState.CatanGamePhase.Setup) {
                ActionTreeNode setup = root.findChildrenByName("Setup", false);
                List<AbstractAction> setupActions = CatanActionFactory.getSetupActions(catanGameState, ActionSpace.Default, playerID);

                // Get the actions created by the factory and validate the leaf nodes they correspond to
                for (AbstractAction action : setupActions) {
                    PlaceSettlementWithRoad psAction = (PlaceSettlementWithRoad) action;
                    ActionTreeNode tileX = setup.findChildrenByName("Tile " + psAction.x, true);
                    ActionTreeNode tileY = tileX.findChildrenByName("Tile " + psAction.y, true);
                    ActionTreeNode vertice = tileY.findChildrenByName("Vertice " + psAction.vertex, true);
                    ActionTreeNode edge = vertice.findChildrenByName("Edge " + psAction.edge, true);
                    edge.setAction(action);
                }
            }

            // In Robber Phase, Update Robber Branch
            else if (catanGameState.getGamePhase() == CatanGameState.CatanGamePhase.Robber) {
                List<AbstractAction> actions = CatanActionFactory.getRobberActions(catanGameState, ActionSpace.Default,
                        playerID, false);

                // Get the actions created by the factory and validate the leaf nodes they correspond to
                for (AbstractAction action : actions) {
                    MoveRobberAndSteal mrAction = (MoveRobberAndSteal) action;
                    ActionTreeNode robber = root.findChildrenByName("Robber", true);
                    ActionTreeNode tileX = robber.findChildrenByName("Tile " + mrAction.x, true);
                    ActionTreeNode tileY = tileX.findChildrenByName("Tile " + mrAction.y, true);
                    ActionTreeNode playerIDNode = tileY.findChildrenByName("Player " + mrAction.player, true);
                    ActionTreeNode targetPlayerID = playerIDNode.findChildrenByName("Target Player " + mrAction.targetPlayer, true);
                    targetPlayerID.setAction(action);
                }
            }

            // If there's trade offer have to respond to it
            else if (catanGameState.tradeOffer != null) {

                // Get trading actions
                List<AbstractAction> tradeActions = CatanActionFactory.getPlayerTradeActions(catanGameState, ActionSpace.Default, playerID);
                for (AbstractAction action : tradeActions) {

                    // Add RejectOffer action to tree
                    if (action instanceof EndNegotiation) {
                        EndNegotiation endNegotiation = (EndNegotiation) action;
                        ActionTreeNode endTradeNode = root.findChildrenByName("End Trade", true);
                        ActionTreeNode offeringPlayerIDNode = endTradeNode.findChildrenByName("Offering Player " + endNegotiation.offeringPlayerID, true);
                        offeringPlayerIDNode.setAction(action);
                    }

                    // Add AcceptOffer action to tree (as long offer does not go above max resources)
                    else if (action instanceof AcceptTrade) {
                        AcceptTrade acceptTrade = (AcceptTrade) action;
                        if (acceptTrade.nOffered < nMaxResources && acceptTrade.nRequested < nMaxResources) {
                            ActionTreeNode acceptTradeNode = root.findChildrenByName("Accept Trade", true);
                            ActionTreeNode offeringPlayerIDNode = acceptTradeNode.findChildrenByName("Offering Player " + acceptTrade.offeringPlayer, true);
                            ActionTreeNode otherPlayerIDNode = offeringPlayerIDNode.findChildrenByName("Other Player " + acceptTrade.otherPlayer, true);
                            ActionTreeNode resourceRequestedNode = otherPlayerIDNode.findChildrenByName(acceptTrade.resourceRequested.toString(), true);
                            ActionTreeNode nRequestedNode = resourceRequestedNode.findChildrenByName("Amount Requested " + acceptTrade.nRequested, true);
                            ActionTreeNode resourceOfferedNode = nRequestedNode.findChildrenByName(acceptTrade.resourceOffered.toString(), true);
                            ActionTreeNode amountOfferedNode = resourceOfferedNode.findChildrenByName("Amount Offered " + acceptTrade.nOffered, true);
                            amountOfferedNode.setAction(action);
                        }
                    }

                    // Add CounterOffer action (same action as normal trade offer) to tree
                    // (as long offer does not go above max resources)
                    else if (action instanceof OfferPlayerTrade) {
                        OfferPlayerTrade offerPlayerTrade = (OfferPlayerTrade) action;
                        if (offerPlayerTrade.nOffered < nMaxResources && offerPlayerTrade.nRequested < nMaxResources) {
                            ActionTreeNode offerPlayerTradeNode = root.findChildrenByName("OfferPlayerTrade", true);
                            ActionTreeNode stageNode = offerPlayerTradeNode.findChildrenByName(offerPlayerTrade.stage.toString(), true);
                            ActionTreeNode resourceOfferedNode = stageNode.findChildrenByName(offerPlayerTrade.resourceOffered.toString(), true);
                            ActionTreeNode nOfferedNode = resourceOfferedNode.findChildrenByName("Amount Offered " + offerPlayerTrade.nOffered, true);
                            ActionTreeNode resourceRequestedNode = nOfferedNode.findChildrenByName(offerPlayerTrade.resourceRequested.toString(), true);
                            ActionTreeNode nRequestedNode = resourceRequestedNode.findChildrenByName("Amount Requested " + offerPlayerTrade.nRequested, true);
                            ActionTreeNode offeringPlayerIDNode = nRequestedNode.findChildrenByName("Offering Player " + offerPlayerTrade.offeringPlayerID, true);
                            ActionTreeNode targetPlayerIDNode = offeringPlayerIDNode.findChildrenByName("Target Player " + offerPlayerTrade.otherPlayerID, true);
                            targetPlayerIDNode.setAction(action);
                        }
                    }
                }
            }

            // All other actions happen in the main phase?
            else {

                // Do nothing action is always valid in the main phase
                ActionTreeNode doNothing = root.findChildrenByName("Do Nothing", true);
                doNothing.setAction(new DoNothing());

                // This Section follows computeAvailableActions in CatanForwardModel

                // Update PORT / BANK (Default) Trade Offer Branch
                List<AbstractAction> defaultTradeActions = CatanActionFactory.getDefaultTradeActions(catanGameState, ActionSpace.Default, playerID);
                //if (defaultTradeActions.size() > 2) reached = true;
                for (AbstractAction action : defaultTradeActions) {
                    DefaultTrade defaultTradeAction = (DefaultTrade) action;
                    ActionTreeNode defaultTradeNode = root.findChildrenByName("Default Trade", true);
                    ActionTreeNode offeredNode = defaultTradeNode.findChildrenByName(defaultTradeAction.resourceOffer.toString(), true);
                    ActionTreeNode desiredNode = offeredNode.findChildrenByName(defaultTradeAction.resourceToGet.toString(), true);
                    ActionTreeNode exchangeNode = desiredNode.findChildrenByName("Exchange Rate " + defaultTradeAction.exchangeRate, true);
                    ActionTreeNode playerNode = exchangeNode.findChildrenByName("Player " + playerID, true);
                    playerNode.setAction(action);
                }


                // Update Trade Offer Branch

                // Get Trading Actions if trades can still be made this turn
                if (catanGameState.nTradesThisTurn < catanParameters.max_trade_actions_allowed) {
                    List<AbstractAction> tradeActions = CatanActionFactory.getPlayerTradeActions(catanGameState, ActionSpace.Default, playerID);

                    // Since only offers can be created in the main phase, only need to add offer actions to tree
                    for (AbstractAction action : tradeActions) {

                        // Add Offer action to tree (as long offer does not go above max resources)
                        if (action instanceof OfferPlayerTrade) {
                            OfferPlayerTrade offerPlayerTrade = (OfferPlayerTrade) action;
                            if (offerPlayerTrade.nOffered < nMaxResources && offerPlayerTrade.nRequested < nMaxResources) {
                                ActionTreeNode offerPlayerTradeNode = root.findChildrenByName("OfferPlayerTrade", true);
                                ActionTreeNode stageNode = offerPlayerTradeNode.findChildrenByName(offerPlayerTrade.stage.toString(), true);
                                ActionTreeNode resourceOfferedNode = stageNode.findChildrenByName(offerPlayerTrade.resourceOffered.toString(), true);
                                ActionTreeNode nOfferedNode = resourceOfferedNode.findChildrenByName("Amount Offered " + offerPlayerTrade.nOffered, true);
                                ActionTreeNode resourceRequestedNode = nOfferedNode.findChildrenByName(offerPlayerTrade.resourceRequested.toString(), true);
                                ActionTreeNode nRequestedNode = resourceRequestedNode.findChildrenByName("Amount Requested " + offerPlayerTrade.nRequested, true);
                                ActionTreeNode offeringPlayerIDNode = nRequestedNode.findChildrenByName("Offering Player " + offerPlayerTrade.offeringPlayerID, true);
                                ActionTreeNode targetPlayerIDNode = offeringPlayerIDNode.findChildrenByName("Target Player " + offerPlayerTrade.otherPlayerID, true);
                                targetPlayerIDNode.setAction(action);
                                reached = true;
                            }
                        }
                    }
                }

                // Update Build Branch
                List<AbstractAction> actions = CatanActionFactory.getBuyActions(catanGameState, ActionSpace.Default, playerID);
                ActionTreeNode buildNode;
                for (AbstractAction action : actions) {

                    // Get Action paramemters
                    int x ;
                    int y;
                    int n;
                    int player;
                    boolean free = false;

                    // If its a development card tree is much simpler
                    if (action instanceof BuyDevelopmentCard) {
                        ActionTreeNode devNode = root.findChildrenByName("Buy Development Card", true);
                        ActionTreeNode playerNode = devNode.findChildrenByName("Player " + playerID, true);
                        playerNode.setAction(action);
                    }
                    else {
                        // Find out what action it is
                        if (action instanceof BuildCity) {
                            BuildCity buildAction = (BuildCity) action;
                            buildNode = root.findChildrenByName("Build City", true);
                            x = buildAction.row;
                            y = buildAction.col;
                            n = buildAction.vertex;
                            player = buildAction.playerID;
                        }
                        else if (action instanceof BuildSettlement) {
                            BuildSettlement buildAction = (BuildSettlement) action;
                            buildNode = root.findChildrenByName("Build Settlement", true);
                            x = buildAction.x;
                            y = buildAction.y;
                            n = buildAction.vertex;
                            player = buildAction.playerID;
                            free = buildAction.free;
                        }
                        else if (action instanceof BuildRoad) {
                            BuildRoad buildAction = (BuildRoad) action;
                            buildNode = root.findChildrenByName("Build Road", true);
                            x = buildAction.x;
                            y = buildAction.y;
                            n = buildAction.edge;
                            player = buildAction.playerID;
                            free = buildAction.free;
                        }
                        else {
                            throw new AssertionError("Invalid Build Action");
                        }

                        // Get the actions created by the factory and validate the leaf nodes they correspond to
                        ActionTreeNode tileX = buildNode.findChildrenByName("Tile " + x, true);
                        ActionTreeNode tileY = tileX.findChildrenByName("Tile " + y, true);
                        ActionTreeNode nNode = tileY.findChildrenByName("N " + n, true);
                        ActionTreeNode playerNode = nNode.findChildrenByName("Player " + player, true);
                        if (action instanceof BuildRoad || action instanceof BuildSettlement) {
                            if (free) {
                                ActionTreeNode freeNode = playerNode.findChildrenByName("Free", true);
                                freeNode.setAction(action);
                            }
                            else {
                                ActionTreeNode notFreeNode = playerNode.findChildrenByName("Not Free", true);
                                notFreeNode.setAction(action);
                            }
                        }
                        else {
                            playerNode.setAction(action);
                        }
                    }



                }

                // Play Development Cards (APART FROM ROAD BUILDING) if no card has been played this turn
                if (catanGameState.noDevelopmentCardPlayed()) {
                    List<AbstractAction> devCardActions = CatanActionFactory.getDevCardActions(catanGameState, ActionSpace.Default, playerID);

                    // Get the actions created by the factory and validate the leaf nodes they correspond to
                    for (AbstractAction action : devCardActions) {

                        // Monopoly
                        if (action instanceof PlayMonopoly) {
                            PlayMonopoly monopolyAction = (PlayMonopoly) action;
                            ActionTreeNode monopolyNode = root.findChildrenByName("Play Monopoly", true);
                            ActionTreeNode resourceNode = monopolyNode.findChildrenByName(monopolyAction.resource.toString(), true);
                            ActionTreeNode playerNode = resourceNode.findChildrenByName("Player " + playerID, true);
                            playerNode.setAction(action);
                        }

                        // Knight
                        if (action instanceof PlayKnightCard) {
                            PlayKnightCard knightAction = (PlayKnightCard) action;
                            ActionTreeNode knight = root.findChildrenByName("Play Knight", true);
                            ActionTreeNode tileX = knight.findChildrenByName("Tile " + knightAction.x, true);
                            ActionTreeNode tileY = tileX.findChildrenByName("Tile " + knightAction.y, true);
                            ActionTreeNode playerIDNode = tileY.findChildrenByName("Player " + knightAction.player, true);
                            ActionTreeNode targetPlayerID = playerIDNode.findChildrenByName("Target Player " + knightAction.targetPlayer, true);
                            targetPlayerID.setAction(action);
                        }

                        // Year of Plenty
                        if (action instanceof PlayYearOfPlenty) {
                            PlayYearOfPlenty yopAction = (PlayYearOfPlenty) action;
                            ActionTreeNode yopNode = root.findChildrenByName("Play Year of Plenty", true);
                            ActionTreeNode resource1Node = yopNode.findChildrenByName(yopAction.resources[0].toString(), true);
                            ActionTreeNode resource2Node = resource1Node.findChildrenByName(yopAction.resources[1].toString(), true);
                            ActionTreeNode playerNode = resource2Node.findChildrenByName("Player " + playerID, true);
                            playerNode.setAction(action);
                        }
                    }
                }
            }


            // Road Buidling (Done Deeply) if they have not played a dev card this turn
            //THIS CAN ONLY HAPPEN IF THEY HAVE THE CARD!!!
            // Maybe it should break if card found?

            if (catanGameState.noDevelopmentCardPlayed()) {
                Deck<CatanCard> playerDevDeck = catanGameState.playerDevCards.get(playerID);
                for (CatanCard card : playerDevDeck.getComponents()) {

                    // If it's a roadbuilding card, and it wasn't bought on the same turn it was played
                    if (card.cardType == CatanCard.CardType.ROAD_BUILDING && card.roundCardWasBought != catanGameState.getTurnCounter()) {

                        ActionSpace roadBuildingSpace = new ActionSpace(ActionSpace.Structure.Deep);
                        List<AbstractAction> roadBuildingActions = CatanActionFactory.getDevCardActions(catanGameState, roadBuildingSpace, playerID, CatanCard.CardType.ROAD_BUILDING);

                        // Roads to build
                        if (!roadBuildingActions.isEmpty()) {
                            HashMap<Triple<Integer, Integer, Integer>, Integer> orderedRoads = orderRoads(catanGameState);
                            for (AbstractAction action : roadBuildingActions) {
                                DeepRoadBuilding roadBuildingAction = (DeepRoadBuilding) action;
                                ActionTreeNode roadBuildingNode = root.findChildrenByName("Play Road Building", true);
                                BuildRoad firstRoad = (BuildRoad) roadBuildingAction.road;
                                ActionTreeNode road1Node = roadBuildingNode.findChildrenByName("Road " + orderedRoads.get(Triple.of(firstRoad.x, firstRoad.y, firstRoad.edge)), true);
                                road1Node.setAction(action);
                            }
                        }
                    }
                }
            }
        }


        //List<ActionTreeNode> test = root.getValidLeaves();
        return root;
    }
}


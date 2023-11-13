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
import games.catan.actions.dev.PlayKnightCard;
import games.catan.actions.dev.PlayMonopoly;
import games.catan.actions.dev.PlayRoadBuilding;
import games.catan.actions.dev.PlayYearOfPlenty;
import games.catan.actions.discard.DiscardResources;
import games.catan.actions.discard.DiscardResourcesPhase;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.actions.trade.DefaultTrade;
import games.catan.components.CatanCard;
import games.catan.components.CatanTile;
import org.apache.commons.lang3.tuple.Triple;
import utilities.ActionTreeNode;

import java.util.HashMap;
import java.util.List;
import java.util.Stack;

public class CatanActionTree {


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
                    if (tile.getEdgeIDs()[e] == -1) {
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
        // 3 - Road 2

        HashMap<Triple<Integer, Integer, Integer>, Integer> roadMap = orderRoads(catanGameState);
        ActionTreeNode roadBuilding = root.addChild(0, "Play Road Building");
        for (int i : roadMap.values()) {
            ActionTreeNode road1Node = roadBuilding.addChild(0, "Road " + i);
            for (int j : roadMap.values()) {
                if (j != i) {
                    road1Node.addChild(0, "Road " + j);
                }
            }
        }
//        for (int n = 0; n < catanGameState.getNPlayers(); n++) {
//            ActionTreeNode playerNode = roadBuilding.addChild(0, "Player " + n);
//
//            // Ordered ID's of roads
//            for (int i : roadMap.values()) {
//                playerNode.addChild(0, "Road " + i);
//                for (int j : roadMap.values()) {
//                    if (j != i) {
//                        playerNode.addChild(0, "Road " + j);
//                    }
//                }
//            }
//        }

        return root;
    }

    public static ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        boolean reached = false;
        root.resetTree();
        CatanGameState catanGameState = (CatanGameState) gameState;
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

            // All other actions happen in the main phase?
            else {

                // Do nothing action is always valid in the main phase
                ActionTreeNode doNothing = root.findChildrenByName("Do Nothing", true);
                doNothing.setAction(new DoNothing());

                // This Section follows computeAvailableActions in CatanForwardModel

                // PORT / BANK (Default) Trade
                List<AbstractAction> defaultTradeActions = CatanActionFactory.getDefaultTradeActions(catanGameState, ActionSpace.Default, playerID);
                if (defaultTradeActions.size() > 2) reached = true;
                for (AbstractAction action : defaultTradeActions) {
                    DefaultTrade defaultTradeAction = (DefaultTrade) action;
                    ActionTreeNode defaultTradeNode = root.findChildrenByName("Default Trade", true);
                    ActionTreeNode offeredNode = defaultTradeNode.findChildrenByName(defaultTradeAction.resourceOffer.toString(), true);
                    ActionTreeNode desiredNode = offeredNode.findChildrenByName(defaultTradeAction.resourceToGet.toString(), true);
                    ActionTreeNode exchangeNode = desiredNode.findChildrenByName("Exchange Rate " + defaultTradeAction.exchangeRate, true);
                    ActionTreeNode playerNode = exchangeNode.findChildrenByName("Player " + playerID, true);
                    playerNode.setAction(action);
                }


                // TODO - Player Trade

                // Update Build Branch
                List<AbstractAction> actions = CatanActionFactory.getBuyActions(catanGameState, ActionSpace.Default, playerID);
                ActionTreeNode buildNode = null;
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

                // Play Development Cards

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

                    // Road Building (Dev Card)
                    if (action instanceof PlayRoadBuilding) {
                        PlayRoadBuilding roadDevAction = (PlayRoadBuilding) action;
                        ActionTreeNode roadBuildingNode = root.findChildrenByName("Play Road Building", true);
                        HashMap<Triple<Integer, Integer, Integer>, Integer> roadMap = orderRoads(catanGameState);

                        // Work out the ID's of valid roads
                        BuildRoad roadAction1 = (BuildRoad) roadDevAction.roadsToBuild[0];
                        BuildRoad roadAction2 = (BuildRoad) roadDevAction.roadsToBuild[1];
                        int roadID1 = roadMap.get(Triple.of(roadAction1.x, roadAction1.y, roadAction1.edge));
                        int roadID2 = roadMap.get(Triple.of(roadAction2.x, roadAction2.y, roadAction2.edge));
                        ActionTreeNode roadNode1 = roadBuildingNode.findChildrenByName("Road " + roadID1, true);
                        ActionTreeNode roadNode2 = roadNode1.findChildrenByName("Road " + roadID2, true);
                        roadNode2.setAction(roadDevAction);
                    }
                }


//                // Need to check if they actually have the card in hand
//
//                Deck<CatanCard> playerDevCards = catanGameState.getPlayerDevCards(playerID);
//
//
//                // Monopoly
//                if (playerDevCards.contains(new CatanCard(CatanCard.CardType.MONOPOLY))) {
//                    List<AbstractAction> monopolyActions = CatanActionFactory.getDevCardActions(catanGameState, ActionSpace.Default, playerID, CatanCard.CardType.MONOPOLY);
//                    for (AbstractAction action : monopolyActions) {
//                        PlayMonopoly monopolyAction = (PlayMonopoly) action;
//                        ActionTreeNode monopolyNode = root.findChildrenByName("Play Monopoly", true);
//                        ActionTreeNode resourceNode = monopolyNode.findChildrenByName(monopolyAction.resource.toString(), true);
//                        ActionTreeNode playerNode = resourceNode.findChildrenByName("Player " + playerID, true);
//                        playerNode.setAction(action);
//                    }
//                }
//
//                // Knight
//                if (playerDevCards.contains(new CatanCard(CatanCard.CardType.KNIGHT_CARD))) {
//                    List<AbstractAction> knightActions = CatanActionFactory.getRobberActions(catanGameState, ActionSpace.Default, playerID, true);
//                    for (AbstractAction action : knightActions) {
//                        PlayKnightCard knightAction = (PlayKnightCard) action;
//                        ActionTreeNode knight = root.findChildrenByName("Play Knight", true);
//                        ActionTreeNode tileX = knight.findChildrenByName("Tile " + knightAction.x, true);
//                        ActionTreeNode tileY = tileX.findChildrenByName("Tile " + knightAction.y, true);
//                        ActionTreeNode playerIDNode = tileY.findChildrenByName("Player " + knightAction.player, true);
//                        ActionTreeNode targetPlayerID = playerIDNode.findChildrenByName("Target Player " + knightAction.targetPlayer, true);
//                        targetPlayerID.setAction(action);
//                    }
//                }
//
//                // Year of Plenty
//                if (playerDevCards.contains(new CatanCard(CatanCard.CardType.YEAR_OF_PLENTY))) {
//                    List<AbstractAction> yopActions = CatanActionFactory.getDevCardActions(catanGameState, ActionSpace.Default, playerID, CatanCard.CardType.YEAR_OF_PLENTY);
//                    for (AbstractAction action : yopActions) {
//                        PlayYearOfPlenty yopAction = (PlayYearOfPlenty) action;
//                        ActionTreeNode yopNode = root.findChildrenByName("Play Year of Plenty", true);
//                        ActionTreeNode resource1Node = yopNode.findChildrenByName(yopAction.resources[0].toString(), true);
//                        ActionTreeNode resource2Node = resource1Node.findChildrenByName(yopAction.resources[1].toString(), true);
//                        ActionTreeNode playerNode = resource2Node.findChildrenByName("Player " + playerID, true);
//                        playerNode.setAction(action);
//                    }
//                }
//
//                // Road Building (Dev Card)
//                if (playerDevCards.contains(new CatanCard(CatanCard.CardType.ROAD_BUILDING))) {
//                    List<AbstractAction> roadBuildingActions = CatanActionFactory.getDevCardActions(catanGameState, ActionSpace.Default, playerID, CatanCard.CardType.ROAD_BUILDING);
//
//                    // No point calculating road map if no actions
//                    if (!roadBuildingActions.isEmpty()) {
//                        HashMap<Triple<Integer, Integer, Integer>, Integer> roadMap = orderRoads(catanGameState);
//
//                        for (AbstractAc dBuilding roadDevAction = (PlayRoadBuilding) action;
//                            ActionTreeNode roadBuildingNode = root.findChildrenByName("Play Road Building", true);
//
//                            // Work out the ID's of valid roads
//                            BuildRoad roadAction1 = (BuildRoad) roadDevAction.roadsToBuild[0];
//                            BuildRoad roadAction2 = (BuildRoad) roadDevAction.roadsToBuild[1];
//                            int roadID1 = roadMap.get(Triple.of(roadAction1.x, roadAction1.y, roadAction1.edge));
//                            int roadID2 = roadMap.get(Triple.of(roadAction2.x, roadAction2.y, roadAction2.edge));
//                            ActionTreeNode roadNode1 = roadBuildingNode.findChildrenByName("Road " + roadID1, true);
//                            ActionTreeNode roadNode2 = roadNode1.findChildrenByName("Road " + roadID2, true);
//                            roadNode2.setAction(roadDevAction);
//                        }
//                    }
//                }
            }
        }


        List<ActionTreeNode> test = root.getValidLeaves();
        return root;
    }
}


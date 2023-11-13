package games.catan;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import core.interfaces.IExtendedSequence;
import games.catan.actions.build.BuildCity;
import games.catan.actions.build.BuildRoad;
import games.catan.actions.build.BuildSettlement;
import games.catan.actions.build.BuyDevelopmentCard;
import games.catan.actions.discard.DiscardResources;
import games.catan.actions.discard.DiscardResourcesPhase;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.actions.trade.DefaultTrade;
import games.catan.components.CatanTile;
import utilities.ActionTreeNode;

import java.util.List;
import java.util.Stack;

public class CatanActionTree {

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
        ActionTreeNode knight = root.addChild(0, "Knight");
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

            // In Robber Phase, Update Robber / Knight Branch
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

                // TODO - Buy Development Card
            }
        }


        List<ActionTreeNode> test = root.getValidLeaves();
        return root;
    }
}


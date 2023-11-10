package games.catan;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.ActionSpace;
import core.actions.DoNothing;
import games.catan.actions.robber.MoveRobberAndSteal;
import games.catan.actions.setup.PlaceSettlementWithRoad;
import games.catan.components.CatanTile;
import utilities.ActionTreeNode;

import java.util.List;

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

        // Setup Robber / Knight Branch
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


        return root;
    }

    public static ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        root.resetTree();
        CatanGameState catanGameState = (CatanGameState) gameState;
        int playerID = catanGameState.getCurrentPlayer();

        // Update Setup Branch
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

        // Update Robber / Knight Branch
        else if (catanGameState.getGamePhase() == CatanGameState.CatanGamePhase.Robber) {
            List<AbstractAction> actions = CatanActionFactory.getRobberActions(catanGameState, ActionSpace.Default,
                    playerID, false);

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
        }

        return root;
    }
}


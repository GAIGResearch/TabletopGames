package games.dotsboxes;

import core.AbstractGameState;
import core.StandardForwardModel;
import core.actions.AbstractAction;
import core.interfaces.ITreeActionSpace;
import utilities.ActionTreeNode;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DBForwardModel extends StandardForwardModel implements ITreeActionSpace {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DBGameState dbgs = (DBGameState) firstState;
        DBParameters dbp = (DBParameters) firstState.getGameParameters();

        dbgs.lastActionScored = false;
        // Generate edge to cell mapping and all cell objects with appropriate constructor
        dbgs.edgeToCellMap = new HashMap<>();
        dbgs.cellToEdgesMap = new HashMap<>();
        dbgs.cellToOwnerMap = new HashMap<>();
        dbgs.edgeToOwnerMap = new HashMap<>();
        dbgs.edges = new HashSet<>();
        dbgs.cells = new HashSet<>();
        for (int i = 0; i < dbp.gridHeight; i++) {
            for (int j = 0; j < dbp.gridWidth; j++) {
                DBCell c = new DBCell(j, i);
                dbgs.cells.add(c);
                HashSet<DBEdge> edges = new HashSet<>(4);
                edges.add(new DBEdge(new Vector2D(j, i), new Vector2D(j, i + 1)));
                edges.add(new DBEdge(new Vector2D(j, i), new Vector2D(j + 1, i)));
                edges.add(new DBEdge(new Vector2D(j + 1, i), new Vector2D(j + 1, i + 1)));
                edges.add(new DBEdge(new Vector2D(j, i + 1), new Vector2D(j + 1, i + 1)));

                for (DBEdge edge : edges) {
                    dbgs.edges.add(edge);
                    if (!dbgs.edgeToCellMap.containsKey(edge)) {
                        dbgs.edgeToCellMap.put(edge, new HashSet<>());
                    }
                    dbgs.edgeToCellMap.get(edge).add(c);
                }

                dbgs.cellToEdgesMap.put(c, edges);
            }
        }
        // Initialise other variables
        dbgs.nCellsPerPlayer = new int[dbgs.getNPlayers()];
    }

    @Override
    protected void _afterAction(AbstractGameState currentState, AbstractAction action) {
        DBGameState dbgs = (DBGameState) currentState;
        DBParameters dbp = (DBParameters) currentState.getGameParameters();

        // Check end of game (when all cells completed)
        if (dbgs.cellToOwnerMap.size() == dbp.gridWidth * dbp.gridHeight) {
            // Game is over. Set status and find winner
            endGame(dbgs);
        } else if (dbgs.getLastActionScored()) {
            // If not returned, check if the action completed one more box, otherwise move to the next player
            endPlayerTurn(currentState);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        HashSet<AbstractAction> actions = new HashSet<>();  // Same edge may appear in multiple cells, ensure unique actions
        DBGameState dbgs = (DBGameState) gameState;

        // Actions in this game are adding edges to the board (that don't already exist)
        for (DBEdge e : dbgs.edges) {
            if (!dbgs.edgeToOwnerMap.containsKey(e)) {
                // Can add this edge
                actions.add(new AddGridCellEdge(e));
            }
        }

        return new ArrayList<>(actions);
    }

    @Override
    public ActionTreeNode initActionTree(AbstractGameState gameState) {
        /*
        Action tree is simple as only one action in the game

        Tree Structure
        0 - Root
        1 - AddGridCellEdge (0 - noEdges)
         */

        DBGameState dbgs = (DBGameState) gameState;
        root = new ActionTreeNode(0, "root");

        // Add all possible actions (edges) to the root
        for (DBEdge edge : dbgs.edges) {
            root.addChild(0, "AddGridCellEdge: " + edge.toString());
        }
        return root;
    }


    @Override
    public ActionTreeNode updateActionTree(ActionTreeNode root, AbstractGameState gameState) {
        root.resetTree();
        DBGameState dbgs = (DBGameState) gameState;

        // Check which actions are valid (i.e., Which edges have no owners)
        for (DBEdge edge : dbgs.edges) {

            // get which child node this edge corresponds to
            ActionTreeNode edgeNode = root.findChildrenByName("AddGridCellEdge: " + edge.toString());

            if (!dbgs.edgeToOwnerMap.containsKey(edge)) {

                // Edge is not owned, so it is a valid action
                edgeNode.setValue(1);
                edgeNode.setAction(new AddGridCellEdge(edge));

            }
        }
        return root;
    }
}

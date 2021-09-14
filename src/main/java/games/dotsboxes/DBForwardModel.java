package games.dotsboxes;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import utilities.Utils;
import utilities.Vector2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DBForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DBGameState dbgs = (DBGameState) firstState;
        DBParameters dbp = (DBParameters) firstState.getGameParameters();

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
                edges.add(new DBEdge(new Vector2D(j, i), new Vector2D(j, i+1)));
                edges.add(new DBEdge(new Vector2D(j, i), new Vector2D(j+1, i)));
                edges.add(new DBEdge(new Vector2D(j+1, i), new Vector2D(j+1, i+1)));
                edges.add(new DBEdge(new Vector2D(j, i+1), new Vector2D(j+1, i+1)));
                
                for (DBEdge edge: edges) {
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
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        DBGameState dbgs = (DBGameState) currentState;
        DBParameters dbp = (DBParameters) currentState.getGameParameters();

        // Will need to check if any cells completed through this action, as that would keep the turn to the current
        // player, otherwise it changes. So keep track of current number of cells completed before action is executed.
        int nCellsCompleteBefore = dbgs.cellToOwnerMap.size();
        // Execute action
        action.execute(currentState);
        // Check end of game (when all cells completed)
        if (dbgs.cellToOwnerMap.size() == dbp.gridWidth * dbp.gridHeight) {
            // Game is over. Set status and find winner
            dbgs.setGameStatus(Utils.GameResult.GAME_END);
            int winner = -1;
            int maxCells = 0;
            for (int i = 0; i < dbgs.getNPlayers(); i++) {
                if (dbgs.nCellsPerPlayer[i] > maxCells) {
                    winner = i;
                    maxCells = dbgs.nCellsPerPlayer[i];
                }
            }
            dbgs.setPlayerResult(Utils.GameResult.WIN, winner);
            for (int i = 0; i < dbgs.getNPlayers(); i++) {
                if (i != winner) {
                    dbgs.setPlayerResult(Utils.GameResult.LOSE, i);
                }
            }
            return;  // No need to do anything else if game is finished
        }

        // If not returned, check if the action completed one more box, otherwise move to the next player
        if (dbgs.cellToOwnerMap.size() == nCellsCompleteBefore) {
            currentState.getTurnOrder().endPlayerTurn(currentState);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        HashSet<AbstractAction> actions = new HashSet<>();  // Same edge may appear in multiple cells, ensure unique actions
        DBGameState dbgs = (DBGameState) gameState;

        // Actions in this game are adding edges to the board (that don't already exist)
        for (DBEdge e: dbgs.edges) {
            if (!dbgs.edgeToOwnerMap.containsKey(e)) {
                // Can add this edge
                actions.add(new AddGridCellEdge(e));
            }
        }

        return new ArrayList<>(actions);
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new DBForwardModel();
    }
}

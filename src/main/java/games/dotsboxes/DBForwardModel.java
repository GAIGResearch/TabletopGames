package games.dotsboxes;

import core.AbstractForwardModel;
import core.AbstractGameState;
import core.actions.AbstractAction;
import core.components.GridBoard;
import utilities.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class DBForwardModel extends AbstractForwardModel {

    @Override
    protected void _setup(AbstractGameState firstState) {
        DBGameState dbgs = (DBGameState) firstState;
        DBParameters dbp = (DBParameters) firstState.getGameParameters();

        // Create initial board
        dbgs.grid = new GridBoard<>(dbp.gridWidth, dbp.gridHeight, DBCell.class);

        // Generate edge to cell mapping and all cell objects with appropriate constructor
        dbgs.edgeToCellMap = new HashMap<>();
        for (int i = 0; i < dbp.gridHeight; i++) {
            for (int j = 0; j < dbp.gridWidth; j++) {
                DBCell c = new DBCell(j, i);
                dbgs.grid.setElement(j, i, c);
                for (DBEdge edge: c.edges) {
                    if (!dbgs.edgeToCellMap.containsKey(edge)) {
                        dbgs.edgeToCellMap.put(edge, new HashSet<>());
                    }
                    dbgs.edgeToCellMap.get(edge).add(c);
                }
            }
        }
        // Initialise other variables
        dbgs.nCellsPerPlayer = new int[dbgs.getNPlayers()];
        dbgs.nCellsComplete = 0;
    }

    @Override
    protected void _next(AbstractGameState currentState, AbstractAction action) {
        DBGameState dbgs = (DBGameState) currentState;
        // Will need to check if any cells completed through this action, as that would keep the turn to the current
        // player, otherwise it changes. So keep track of current number of cells completed before action is executed.
        int nCellsCompleteBefore = dbgs.nCellsComplete;
        // Execute action
        action.execute(currentState);
        // Check end of game (when all cells completed)
        if (dbgs.nCellsComplete == dbgs.grid.getWidth() * dbgs.grid.getHeight()) {
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
        if (dbgs.nCellsComplete == nCellsCompleteBefore) {
            currentState.getTurnOrder().endPlayerTurn(currentState);
        }
    }

    @Override
    protected List<AbstractAction> _computeAvailableActions(AbstractGameState gameState) {
        HashSet<AbstractAction> actions = new HashSet<>();  // Same edge may appear in multiple cells, ensure unique actions
        DBGameState dbgs = (DBGameState) gameState;

        // Actions in this game are adding edges to the board (that don't already exist)
        for (int i = 0; i < dbgs.grid.getHeight(); i++) {
            for (int j = 0; j < dbgs.grid.getWidth(); j++) {
                DBCell c = dbgs.grid.getElement(j, i);
                if (c.nEdgesComplete < 4) {
                    // Can still add an edge in this cell
                    for (int e = 0; e < 4; e++) {
                        DBEdge edge = c.edges.get(e);
                        if (edge.owner == -1) {
                            // This edge does not yet exist, we can add it
                            actions.add(new AddGridCellEdge(edge));
                        }
                    }
                }
            }
        }

        return new ArrayList<>(actions);
    }

    @Override
    protected AbstractForwardModel _copy() {
        return new DBForwardModel();
    }
}

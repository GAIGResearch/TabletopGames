package games.dotsboxes;

import core.AbstractGameState;
import core.actions.AbstractAction;

import java.util.HashSet;
import java.util.Objects;

public class AddGridCellEdge extends AbstractAction {
    DBEdge edge;  // Normally not good practice to keep references, as they wouldn't match the copies. But our equals should match.

    public AddGridCellEdge(DBEdge edge) {
        this.edge = edge;
    }

    @Override
    public boolean execute(AbstractGameState gs) {
        // Find neighbouring cells
        DBGameState dbgs = (DBGameState) gs;

        int nCellsCompleteBefore = dbgs.cellToOwnerMap.size();
        // Mark this edge as complete by current player and check if connected cells are complete too
        dbgs.edgeToOwnerMap.put(edge, gs.getCurrentPlayer());

        HashSet<DBCell> cells = dbgs.edgeToCellMap.get(edge);
        for (DBCell c : cells) {
            int nEdgesComplete = dbgs.countCompleteEdges(c);
            if (nEdgesComplete == 4) {  // A cell has 4 sides
                // All edges complete, this box complete
                dbgs.cellToOwnerMap.put(c, gs.getCurrentPlayer());
                dbgs.nCellsPerPlayer[gs.getCurrentPlayer()]++;
            }
        }
        int nCellsCompleteAfter = dbgs.cellToOwnerMap.size();
        dbgs.setLastActionDidNotScore(nCellsCompleteAfter == nCellsCompleteBefore);
        return true;  // Always able to execute
    }

    @Override
    public AbstractAction copy() {
        return new AddGridCellEdge(edge.copy());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AddGridCellEdge)) return false;
        AddGridCellEdge that = (AddGridCellEdge) o;
        return Objects.equals(edge, that.edge);
    }

    @Override
    public int hashCode() {
        return Objects.hash(edge);
    }

    @Override
    public String getString(AbstractGameState gameState) {
        return toString();
    }

    @Override
    public String toString() {
        return edge.from.toString() + " -> " + edge.to.toString();
    }
}

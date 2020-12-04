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
        HashSet<DBCell> cells = dbgs.edgeToCellMap.get(edge);

        // For each cell, mark this edge as complete by current player and check if whole cell is complete to set owner
        for (DBCell c : cells) {
            c.nEdgesComplete++;
            for (DBEdge e : c.edges) {
                if (e.equals(edge)) {
                    e.owner = gs.getCurrentPlayer();
                    break;  // Only 1 edge would match
                }
            }
            if (c.nEdgesComplete == 4) {  // A cell has 4 sides
                // All edges complete, this box complete
                dbgs.nCellsComplete++;
                dbgs.nCellsPerPlayer[gs.getCurrentPlayer()]++;
                c.owner = gs.getCurrentPlayer();
            }
        }

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

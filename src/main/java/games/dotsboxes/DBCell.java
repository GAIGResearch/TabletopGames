package games.dotsboxes;

import utilities.Vector2D;

import java.util.ArrayList;
import java.util.Objects;

public class DBCell {
    int owner;  // Owner of this cell (-1 means no owner yet, as cell is incomplete)
    ArrayList<DBEdge> edges;  // List of edges for this cell
    Vector2D position;  // Position of this cell in the grid
    int nEdgesComplete;  // Number of edges completed for this cell, for faster computation. If == 4, cell is complete.

    public DBCell() {}  // Default constructor for copies, to avoid creating edges every time

    public DBCell(int x, int y) {
        owner = -1;
        position = new Vector2D(x, y);
        nEdgesComplete = 0;

        // Add possible edges, where (x,y) is top-left corner of this cell. 4 total possible edges
        edges = new ArrayList<>();
        edges.add(new DBEdge(new Vector2D(x, y), new Vector2D(x, y+1)));
        edges.add(new DBEdge(new Vector2D(x, y), new Vector2D(x+1, y)));
        edges.add(new DBEdge(new Vector2D(x+1, y), new Vector2D(x+1, y+1)));
        edges.add(new DBEdge(new Vector2D(x, y+1), new Vector2D(x+1, y+1)));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBCell)) return false;
        DBCell dbCell = (DBCell) o;
        return owner == dbCell.owner &&
                nEdgesComplete == dbCell.nEdgesComplete &&
                Objects.equals(edges, dbCell.edges) &&
                Objects.equals(position, dbCell.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(owner, edges, position, nEdgesComplete);
    }

    public DBCell copy() {
        // For grid board deep copy
        DBCell c = new DBCell();
        c.position = position.copy();
        c.owner = owner;
        c.nEdgesComplete = nEdgesComplete;
        // Deep copy edges
        c.edges = new ArrayList<>();
        for (DBEdge e: edges) {
            c.edges.add(e.copy());
        }
        return c;
    }

    @Override
    public String toString() {
        return owner + "";
    }
}

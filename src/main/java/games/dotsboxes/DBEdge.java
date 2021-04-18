package games.dotsboxes;

import utilities.Vector2D;

import java.util.Objects;

public class DBEdge {
    Vector2D from;  // Edges are lines between 2 points, from -> to. No direction, so to -> from is the same.
    Vector2D to;
    int owner;  // Keep track of who placed this edge on the board for drawing purposes.

    public DBEdge(Vector2D from, Vector2D to) {
        this.from = from;
        this.to = to;
        this.owner = -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBEdge)) return false;
        DBEdge dbEdge = (DBEdge) o;
        // from -> to == to -> from (and we don't care about owner for this comparison)
        return Objects.equals(from, dbEdge.from) && Objects.equals(to, dbEdge.to) ||
                Objects.equals(from, dbEdge.to) && Objects.equals(to, dbEdge.from);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to) + Objects.hash(to, from);
    }

    public DBEdge copy() {
        // We'll need deep copies of the board, so this should also be able to copy itself
        DBEdge e = new DBEdge(from.copy(), to.copy());
        e.owner = owner;
        return e;
    }
}

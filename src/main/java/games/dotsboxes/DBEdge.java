package games.dotsboxes;

import core.CoreConstants;
import core.components.Component;
import org.jetbrains.annotations.NotNull;
import utilities.Vector2D;

import java.util.Objects;

public class DBEdge extends Component implements Comparable<DBEdge>{
    // Edges are lines between 2 points, from -> to. No direction, so to -> from is the same.

    final Vector2D from;
    final Vector2D to;

    public DBEdge(Vector2D from, Vector2D to) {
        super(CoreConstants.ComponentType.TOKEN);
        this.from = from;
        this.to = to;
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
        return this;  // Immutable
    }

    @Override
    public String toString() {return from.toString() + " -> " + to.toString();}

    // Compare edges by their from and to points
    @Override
    public int compareTo(@NotNull DBEdge o) {

        // Check if edges are identical
        if (equals(o)) {
            return 0;
        }

        // Check which starting edge has smaller x value
        if (from.getX() < o.from.getX()) {
            return -1;
        }

        // If starting edge X equal in value, check which starting edge has smaller y value
        else if (from.getX() == o.from.getX() && from.getY() < o.from.getY()) {
            return -1;
        }

        // If starting edge equal in value, check which ending edge has smallest X
        else if (from.equals(o.from) && to.getX() < o.to.getX()) {
            return -1;
        }

        // If starting edge and ending X equal in value, check which ending edge has smallest Y
        else if (from.equals(o.from) && to.getX() == o.to.getX() && to.getY() < o.to.getY()) {
            return -1;
        }

        // If none of the above, then this edge is greater than the other
        else {
            return 1;
        }
    }
}

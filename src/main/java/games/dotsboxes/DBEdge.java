package games.dotsboxes;

import core.CoreConstants;
import core.components.Component;
import utilities.Vector2D;

import java.util.Objects;

public class DBEdge extends Component {
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
}

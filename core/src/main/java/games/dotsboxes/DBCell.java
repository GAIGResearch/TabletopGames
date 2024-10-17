package games.dotsboxes;

import core.CoreConstants;
import core.components.Component;
import utilities.Vector2D;

import java.util.Objects;

public class DBCell extends Component {
    final Vector2D position;  // Position of this cell in the grid

    public DBCell(int x, int y) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Box");
        position = new Vector2D(x, y);
    }

    private DBCell(int componentID, Vector2D position) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Box", componentID);
        this.position = position;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DBCell)) return false;
        DBCell dbCell = (DBCell) o;
        return Objects.equals(position, dbCell.position);
    }

    @Override
    public int hashCode() {
        return Objects.hash(position);
    }

    public DBCell copy() {
        return this;  // Immutable
    }

    @Override
    public String toString() {
        return position.toString();
    }
}

package games.descent2e.components.tokens;

import games.descent2e.DescentTypes;
import utilities.Pair;
import utilities.Vector2D;

import java.util.List;
import java.util.Objects;

public class Door extends DToken {

    Pair<String, String> sides;
    List<Vector2D> location;
    boolean open = false;

    public Door(Vector2D position, int componentID, Pair<String, String> sides) {
        super(DescentTypes.DescentToken.Door, position, componentID);
        this.sides = sides;
    }

    public Door(Vector2D position, int componentID, Pair<String, String> sides, boolean open) {
        super(DescentTypes.DescentToken.Door, position, componentID);
        this.sides = sides;
        this.open = open;
    }

    @Override
    public Door copy() {
        Door copy = new Door(position.copy(), componentID, sides, open);
        copy.location = List.copyOf(location);
        return copy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sides, location, open);
    }
}

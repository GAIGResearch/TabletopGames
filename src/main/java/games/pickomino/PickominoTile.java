package games.pickomino;

import core.CoreConstants;
import core.components.Component;

/**
 * Represents a Pickomino tile component.
 * Tiles in Pickomino typically have values and can be collected by players.
 */
public class PickominoTile extends Component {
    
    final private int value;
    final private int score;

    public int getValue() {
        return value;
    }

    public int getScore() {
        return score;
    }

    /**
     * Creates a new PickominoTile. The component ID will be automatically assigned.
     * @param name - name of the tile.
     * @param value - value of the tile.
     * @param score - score of the tile.
     */
    public PickominoTile(String name, int value, int score) {
        super(CoreConstants.ComponentType.TOKEN, name);
        this.value = value;
        this.score = score;
    }

    /**
     * Creates a new PickominoTile with the given name and component ID.
     * Used for copying components.
     * @param name - name of the tile.
     * @param componentID - unique component ID.
     * @param value - value of the tile.
     * @param score - score of the tile.
     */
    protected PickominoTile(String name, int componentID, int value, int score) {
        super(CoreConstants.ComponentType.TOKEN, name, componentID);
        this.value = value;
        this.score = score;
    }

    /**
     * Creates a deep copy of this tile.
     * @return a new PickominoTile with the same properties.
     */
    @Override
    public PickominoTile copy() {
        PickominoTile copy = new PickominoTile(componentName, componentID, value, score);
        copyComponentTo(copy);
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PickominoTile)) return false;
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return componentName;
    }
}


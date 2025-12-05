package games.pickomino;

import core.CoreConstants;
import core.components.Component;

/**
 * Represents a Pickomino tile component.
 * Tiles in Pickomino typically have values and can be collected by players.
 */
public class PickominoTile extends Component {
    
    private int value;
    private int score;

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    /**
     * Creates a new PickominoTile with the given name.
     * @param name - name of the tile.
     */
    public PickominoTile(String name) {
        super(CoreConstants.ComponentType.TOKEN, name);
    }

    /**
     * Creates a new PickominoTile with the given name and component ID.
     * @param name - name of the tile.
     * @param componentID - unique component ID.
     */
    protected PickominoTile(String name, int componentID) {
        super(CoreConstants.ComponentType.TOKEN, name, componentID);
    }

    /**
     * Creates a deep copy of this tile.
     * @return a new PickominoTile with the same properties.
     */
    @Override
    public PickominoTile copy() {
        PickominoTile copy = new PickominoTile(componentName, componentID);
        copyComponentTo(copy);
        copy.value = value;
        copy.score = score;
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


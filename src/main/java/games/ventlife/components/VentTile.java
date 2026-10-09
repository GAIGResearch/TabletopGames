package games.ventlife.components;

import core.CoreConstants;
import core.components.Component;

import java.util.Objects;

/**
 * A triple-hex tile: a Black Smoker hex with two seafloor terrain hexes below it, to its left and right, all three
 * touching. Immutable, and equal by its terrains.
 */
public class VentTile extends Component {

    public final Terrain left;
    public final Terrain right;

    public VentTile(Terrain left, Terrain right) {
        super(CoreConstants.ComponentType.TOKEN, left + " / " + right);
        this.left = left;
        this.right = right;
    }

    @Override
    public VentTile copy() {
        return this;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof VentTile t && left == t.left && right == t.right;
    }

    @Override
    public int hashCode() {
        return Objects.hash(left, right) + 618843;
    }

    @Override
    public String toString() {
        return componentName;
    }
}

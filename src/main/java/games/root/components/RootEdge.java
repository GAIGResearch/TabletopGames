package games.root.components;

import core.CoreConstants;
import core.components.Component;

public class RootEdge extends Component {
    public RootEdge() {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge");
        setOwnerId(-1);
    }
    private RootEdge(int ownerID, int id) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge", id);
        setOwnerId(ownerID);
    }

    @Override
    public String toString() {
        // prints references
        return "Edge (" + ownerId + ")";
    }

    public RootEdge copy(){
        return new RootEdge(ownerId, componentID);
    }

    @Override
    public int hashCode() {
        return componentID + ownerId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof RootEdge && super.equals(o);
    }
}


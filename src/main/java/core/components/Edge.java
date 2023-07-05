package core.components;

import core.CoreConstants;

public class Edge extends Component {

    public Edge() {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge");
        setOwnerId(-1);
    }
    private Edge(int ownerID, int id) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge", id);
        setOwnerId(ownerID);
    }

    @Override
    public String toString() {
        // prints references
        return "Edge (" + ownerId + ")";
    }

    public Edge copy(){
        return new Edge(ownerId, componentID);
    }

    @Override
    public int hashCode() {
        return componentID + ownerId;
    }
}

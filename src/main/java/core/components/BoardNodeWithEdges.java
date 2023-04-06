package core.components;

import core.CoreConstants;

import java.util.*;

public class BoardNodeWithEdges extends Component {

    protected final Map<Edge, BoardNodeWithEdges> neighbourEdgeMapping;  // Neighbours mapping to edge object encapsulating edge information, connecting this node to the one in the map key

    public BoardNodeWithEdges() {
        super(CoreConstants.ComponentType.BOARD_NODE, "");
        neighbourEdgeMapping = new HashMap<>();
    }

    protected BoardNodeWithEdges(int owner, int ID) {
        super(CoreConstants.ComponentType.BOARD_NODE, "", ID);
        setOwnerId(owner);
        neighbourEdgeMapping = new HashMap<>();
    }

    /**
     * Adds a neighbour for this node.
     * @param neighbour - new neighbour of this node.
     */
    public void addNeighbour(BoardNodeWithEdges neighbour, Edge edge) {
        neighbourEdgeMapping.put(edge, neighbour);
        neighbour.neighbourEdgeMapping.put(edge, this);
    }

    /**
     * Removes neighbour of this node.
     * @param neighbour - neighbour to remove.
     * @return - true if removed successfully, false otherwise. may fail if neighbour didn't exist in the first place.
     */
    public void removeNeighbour(BoardNodeWithEdges neighbour, Edge edge) {
        neighbourEdgeMapping.remove(edge);
        neighbour.neighbourEdgeMapping.remove(edge);
    }

    /**
     * @return the neighbours of this node.
     */
    public Set<BoardNodeWithEdges> getNeighbours() {
        return new HashSet<>(neighbourEdgeMapping.values());
    }

    public Map<Edge, BoardNodeWithEdges> getNeighbourEdgeMapping() {
        return neighbourEdgeMapping;
    }

    public Set<Edge> getEdges() {
        return new HashSet<>(neighbourEdgeMapping.keySet());
    }

    /**
     * Copies all node properties to a new instance of this node.
     * @return - a new instance of this node.
     */
    @Override
    public BoardNodeWithEdges copy() {
        // WARNING: DO not copy this directly, the GraphBoard copies it to correctly assign neighbour references!
        return null;
    }

    @Override
    public String toString() {
        return "{id: " + componentID + "; owner: " + ownerId + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardNodeWithEdges)) return false;
        if (!super.equals(o)) return false;
        BoardNodeWithEdges that = (BoardNodeWithEdges) o;
        return Objects.equals(neighbourEdgeMapping, that.neighbourEdgeMapping);
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentID, ownerId);
    }

    public Edge getEdgeByID(int edgeID) {
        for (Edge e: neighbourEdgeMapping.keySet()) {
            if (e.componentID == edgeID) return e;
        }
        return null;
    }

    public BoardNodeWithEdges getNeighbour(Edge edge) {
        return neighbourEdgeMapping.get(edge);
    }

    public Edge getEdge(BoardNodeWithEdges neighbour) {
        for (Map.Entry<Edge, BoardNodeWithEdges> e: neighbourEdgeMapping.entrySet()) {
            if (e.getValue().equals(neighbour)) return e.getKey();
        }
        return null;
    }
}

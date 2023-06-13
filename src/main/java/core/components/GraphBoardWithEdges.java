package core.components;

import core.CoreConstants;
import core.interfaces.IComponentContainer;
import java.util.*;

public class GraphBoardWithEdges extends Component implements IComponentContainer<BoardNodeWithEdges> {

    // List of nodes in the board graph, mapping component ID to object reference
    protected Map<Integer, BoardNodeWithEdges> boardNodes;

    public GraphBoardWithEdges(String name)
    {
        super(CoreConstants.ComponentType.BOARD, name);
        boardNodes = new HashMap<>();
    }

    public GraphBoardWithEdges()
    {
        super(CoreConstants.ComponentType.BOARD);
        boardNodes = new HashMap<>();
    }

    protected GraphBoardWithEdges(String name, int ID)
    {
        super(CoreConstants.ComponentType.BOARD, name, ID);
        boardNodes = new HashMap<>();
    }

    GraphBoardWithEdges(int ID)
    {
        super(CoreConstants.ComponentType.BOARD, ID);
        boardNodes = new HashMap<>();
    }

    /**
     * Copy method, to be implemented by all subclasses.
     * @return - a new instance of this Board, deep copy.
     */
    @Override
    public GraphBoardWithEdges copy()
    {
        GraphBoardWithEdges b = new GraphBoardWithEdges(componentName, componentID);
        HashMap<Integer, BoardNodeWithEdges> nodeCopies = new HashMap<>();
        HashMap<Integer, Edge> edgeCopies = new HashMap<>();
        // Copy board nodes
        for (BoardNodeWithEdges bn: boardNodes.values()) {
            BoardNodeWithEdges bnCopy = bn.copy();
            if (bnCopy == null) bnCopy = new BoardNodeWithEdges(bn.ownerId, bn.getComponentID());
            bn.copyComponentTo(bnCopy);
            nodeCopies.put(bn.getComponentID(), bnCopy);
            // Copy edges
            for (Edge e: bn.neighbourEdgeMapping.keySet()) {
                edgeCopies.put(e.componentID, e.copy());
            }
        }
        // Assign neighbours and edges
        for (BoardNodeWithEdges bn: boardNodes.values()) {
            BoardNodeWithEdges bnCopy = nodeCopies.get(bn.getComponentID());
            for (Map.Entry<Edge, BoardNodeWithEdges> e: bn.neighbourEdgeMapping.entrySet()) {
                bnCopy.addNeighbour(nodeCopies.get(e.getValue().getComponentID()), edgeCopies.get(e.getKey().componentID));
            }
        }

        // Assign new neighbours
        b.setBoardNodes(nodeCopies);

        // Copy properties
        copyComponentTo(b);
        return b;
    }

    /**
     * @return the list of board nodes
     */
    public Collection<BoardNodeWithEdges> getBoardNodes() {
        return boardNodes.values();
    }

    /**
     * Returns the node in the list which matches the given ID
     * @param id - ID of node to search for.
     * @return - node matching ID.
     */
    public BoardNodeWithEdges getNodeByID(int id) {
        return boardNodes.get(id);
    }

    /**
     * Sets the list of board nodes to the given list.
     * @param boardNodes - new list of board nodes.
     */
    public void setBoardNodes(List<BoardNodeWithEdges> boardNodes) {
        for (BoardNodeWithEdges bn: boardNodes) {
            this.boardNodes.put(bn.getComponentID(), bn);
        }
    }
    public void setBoardNodes(Map<Integer, BoardNodeWithEdges> boardNodes) {
        this.boardNodes = boardNodes;
    }

    public void addBoardNode(BoardNodeWithEdges bn) {
        this.boardNodes.put(bn.getComponentID(), bn);
    }

    public void removeBoardNode(BoardNodeWithEdges bn) {
        this.boardNodes.remove(bn.getComponentID());
    }

    public void breakConnection(BoardNodeWithEdges bn1, BoardNodeWithEdges bn2, Edge edge) {
        bn1.removeNeighbour(bn2, edge);
        bn2.removeNeighbour(bn1, edge);

        // Check if they have at least 1 more neighbour on this board. If not, remove node from this board
        boolean inBoard = false;
        for (BoardNodeWithEdges n: bn1.getNeighbours()) {
            if (boardNodes.containsKey(n.getComponentID())) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn1.getComponentID());

        inBoard = false;
        for (BoardNodeWithEdges n: bn2.getNeighbours()) {
            if (boardNodes.containsKey(n.getComponentID())) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn2.getComponentID());
    }

    public Edge addConnection(BoardNodeWithEdges bn1, BoardNodeWithEdges bn2) {
        Edge edge = new Edge();
        addConnection(bn1, bn2, edge);
        return edge;
    }

    public Edge addConnection(int bn1id, int bn2id) {
        BoardNodeWithEdges bn1 = boardNodes.get(bn1id);
        BoardNodeWithEdges bn2 = boardNodes.get(bn2id);
        Edge edge = new Edge();
        addConnection(bn1, bn2, edge);
        return edge;
    }

    public void addConnection(BoardNodeWithEdges bn1, BoardNodeWithEdges bn2, Edge edge) {
        bn1.addNeighbour(bn2, edge);
        bn2.addNeighbour(bn1, edge);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GraphBoardWithEdges) {
            GraphBoardWithEdges other = (GraphBoardWithEdges) o;
            return componentID == other.componentID && other.boardNodes.equals(boardNodes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentID, boardNodes);
    }

    @Override
    public List<BoardNodeWithEdges> getComponents() {
        return new ArrayList<>(getBoardNodes());
    }

    public Map<Integer, BoardNodeWithEdges> getBoardNodeMap() {
        return boardNodes;
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }
}

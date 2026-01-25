package core.components;

import core.CoreConstants;
import core.properties.Property;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


import java.util.HashMap;

public class BoardNode extends Component {

    final static double defaultCost = 1.0;
    private HashMap<BoardNode, Double> neighbours;  // Neighbours of this board node, <component IDs, cost to traverse>
    private HashMap<BoardNode, Integer> neighbourSideMapping;  // Neighbours mapping to a side of this board node, component ID -> side idx
    private int maxNeighbours;  // Maximum number of neighbours for this board node

    public BoardNode(int maxNeighbours, String name) {
        super(CoreConstants.ComponentType.BOARD_NODE, name);
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashMap<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    public BoardNode(String name) {
        super(CoreConstants.ComponentType.BOARD_NODE, name);
        this.maxNeighbours = -1;
        this.neighbours = new HashMap<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    // Copy constructor of properties (incl. other component ID)
    public BoardNode(BoardNode other) {
        super(CoreConstants.ComponentType.BOARD_NODE, other.componentName, other.componentID);
        this.maxNeighbours = other.maxNeighbours;
        this.neighbours = new HashMap<>();
        this.neighbourSideMapping = new HashMap<>();
        other.copyComponentTo(this);
    }

    public BoardNode() {
        this(-1, "");
    }

    protected BoardNode(int maxNeighbours, String name, int ID) {
        super(CoreConstants.ComponentType.BOARD_NODE, name, ID);
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashMap<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    /**
     * Adds a neighbour for this node.
     * @param neighbour - new neighbour of this node.
     */
    public void addNeighbourWithCost(BoardNode neighbour) {
        addNeighbourWithCost(neighbour, defaultCost);
    }


    /**
     * Adds a neighbour for this node, with a cost to reach it.
     * @param neighbour - new neighbour of this node.
     * @param cost - cost to reach this neighbour from 'this'
     */
    public void addNeighbourWithCost(BoardNode neighbour, double cost) {
        if (neighbours.size() <= maxNeighbours || maxNeighbours == -1) {
            neighbours.put(neighbour, cost);
        }
    }

    /**
     * Removes neighbour of this node.
     * @param neighbour - neighbour to remove.
     * @return - true if removed successfully, false otherwise. may fail if neighbour didn't exist in the first place.
     */
    public boolean removeNeighbour(BoardNode neighbour) {
        if (neighbours.containsKey(neighbour)) {
            neighbours.remove(neighbour);
            neighbourSideMapping.remove(neighbour);
            return true;
        }
        return false;
    }

    /**
     * Adds neighbour to specific side of this node.
     * @param neighbour - new neighbour to be added.
     * @param side - side of this node to be added in.
     * @return - true if added successfully, false otherwise. may fail if too many neighbours added already.
     */
    public boolean addNeighbourOnSide(BoardNode neighbour, int side) {
        return addNeighbourOnSideWithCost(neighbour, side, defaultCost);
    }

    /**
     * Adds neighbour to specific side of this node.
     * @param neighbour - new neighbour to be added.
     * @param side - side of this node to be added in.
     * @param cost - cost to reach this neighbour from 'this'
     * @return - true if added successfully, false otherwise. may fail if too many neighbours added already.
     */
    public boolean addNeighbourOnSideWithCost(BoardNode neighbour, int side, double cost) {
        if (neighbours.size() <= maxNeighbours && side <= maxNeighbours || maxNeighbours == -1) {
            if (!(neighbours.containsKey(neighbour)) && !(neighbourSideMapping.containsKey(neighbour))) {
                neighbours.put(neighbour, cost);
                neighbourSideMapping.put(neighbour, side);
                return true;
            }
        }
        return false;
    }

    /**
     * Copies all node properties to a new instance of this node.
     * @return - a new instance of this node.
     */
    @Override
    public BoardNode copy() {
        // WARNING: DO not copy this directly, the GraphBoard/GridBoard copies it to correctly assign neighbour references!
        return new BoardNode(this);
    }

    @Override
    public void copyComponentTo(Component copyTo) {
        if (!(copyTo instanceof BoardNode))
            throw new RuntimeException("BoardNode.copyComponentTo(): Trying to copy to an incompatible component type");
        super.copyComponentTo(copyTo);
    }

    /**
     * @return the neighbours of this node.
     */
    public HashMap<BoardNode, Double> getNeighbours() {
        return neighbours;
    }

    public void clearNeighbours() {
        neighbours.clear();
        neighbourSideMapping.clear();
    }

    /**
     * Returns the cost of a neighbour to this node. Throws exception if the neighbour is not.
     * @param neighbour neighbour to return the cost of traveling to that neighbour
     * @return the cost.
     */
    public double getNeighbourCost(BoardNode neighbour)
    {
        if(neighbours.containsKey(neighbour))
            return neighbours.get(neighbour);
        throw new RuntimeException("BoardNode.getNeighbourCost(): Accessing cost of a non-neighbour");
    }

    /**
     * @return the neighbours mapping to sides of this node.
     */
    public HashMap<BoardNode, Integer> getNeighbourSideMapping() {
        return neighbourSideMapping;
    }

    /**
     * @return - maximum number of neighbours for this board node.
     */
    public int getMaxNeighbours() {
        return maxNeighbours;
    }

    /**
     * Set the maximum number of neighbours.
     * @param maxNeighbours - new maximum.
     */
    public void setMaxNeighbours(int maxNeighbours) {
        this.maxNeighbours = maxNeighbours;
    }

    public void loadBoardNode(JSONObject node) {
        this.componentName = (String) ( (JSONArray) node.get("name")).get(1);
        parseComponent(this, node);
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("{id: " + componentID + "; maxNeighbours: " + maxNeighbours + "; ");
        for(int prop_key : properties.keySet()) {
            Property prop = properties.get(prop_key);
            sb.append(prop.getHashString() + ": " + prop + "; ");
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return componentID;
    }
}

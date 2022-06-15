package core.components;

import core.properties.Property;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import utilities.Utils;

import java.util.HashMap;
import java.util.HashSet;

public class BoardNode extends Component {

    final static double defaultCost = 1.0;
    private HashMap<Integer, Double> neighbours;  // Neighbours of this board node, <component IDs, cost to traverse>
    private HashMap<Integer, Integer> neighbourSideMapping;  // Neighbours mapping to a side of this board node, component ID -> side idx
    private int maxNeighbours;  // Maximum number of neighbours for this board node


    public BoardNode(int maxNeighbours, String name) {
        super(Utils.ComponentType.BOARD_NODE, name);
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashMap<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    public BoardNode() {
        this(-1, "");
    }

    protected BoardNode(int maxNeighbours, String name, int ID) {
        super(Utils.ComponentType.BOARD_NODE, name, ID);
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashMap<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    /**
     * Adds a neighbour for this node.
     * @param neighbour - new neighbour of this node.
     */
    public void addNeighbour(BoardNode neighbour) {
        addNeighbour(neighbour, defaultCost);
    }


    /**
     * Adds a neighbour for this node, with a cost to reach it.
     * @param neighbour - new neighbour of this node.
     * @param cost - cost to reach this neighbour from 'this'
     */
    public void addNeighbour(BoardNode neighbour, double cost) {
        if (neighbours.size() <= maxNeighbours || maxNeighbours == -1) {
            neighbours.put(neighbour.componentID, cost);
        }
    }

    /**
     * Removes neighbour of this node.
     * @param neighbour - neighbour to remove.
     * @return - true if removed successfully, false otherwise. may fail if neighbour didn't exist in the first place.
     */
    public boolean removeNeighbour(BoardNode neighbour) {
        if (neighbours.containsKey(neighbour.componentID)) {
            neighbours.remove(neighbour.componentID);
            neighbourSideMapping.remove(neighbour.componentID);
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
    public boolean addNeighbour(BoardNode neighbour, int side) {
        return addNeighbour(neighbour, side, defaultCost);
    }

    /**
     * Adds neighbour to specific side of this node.
     * @param neighbour - new neighbour to be added.
     * @param side - side of this node to be added in.
     * @param cost - cost to reach this neighbour from 'this'
     * @return - true if added successfully, false otherwise. may fail if too many neighbours added already.
     */
    public boolean addNeighbour(BoardNode neighbour, int side, double cost) {
        if (neighbours.size() <= maxNeighbours && side <= maxNeighbours || maxNeighbours == -1) {
            if (!(neighbours.containsKey(neighbour.componentID)) && !(neighbourSideMapping.containsKey(neighbour.componentID))) {
                neighbours.put(neighbour.componentID, cost);
                neighbourSideMapping.put(neighbour.componentID, side);
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
        BoardNode bn = new BoardNode(maxNeighbours, componentName, componentID);
        bn.neighbours = new HashMap<>(neighbours);
        bn.neighbourSideMapping = new HashMap<>(neighbourSideMapping);
        copyComponentTo(bn);
        return bn;
    }
    public BoardNode copyNewID() {
        BoardNode bn = new BoardNode(maxNeighbours, componentName);
        bn.neighbours = new HashMap<>(neighbours);
        bn.neighbourSideMapping = new HashMap<>(neighbourSideMapping);
        copyComponentTo(bn);
        return bn;
    }

    /**
     * @return the neighbours of this node.
     */
    public HashMap<Integer, Double> getNeighbours() {
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
        if(neighbours.containsKey(neighbour.componentID))
            return neighbours.get(neighbour.componentID);
        throw new RuntimeException("BoardNode.getNeighbourCost(): Accessing cost of a non-neighbour");
    }

    /**
     * @return the neighbours mapping to sides of this node.
     */
    public HashMap<Integer, Integer> getNeighbourSideMapping() {
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

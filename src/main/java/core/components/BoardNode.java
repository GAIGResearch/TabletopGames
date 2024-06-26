package core.components;

import core.CoreConstants;
import core.properties.Property;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.HashMap;
import java.util.HashSet;

public class BoardNode extends Component {

    protected HashSet<BoardNode> neighbours;  // Neighbours of this board node
    private HashMap<BoardNode, Integer> neighbourSideMapping;  // Neighbours mapping to a side of this board node
    protected int maxNeighbours;  // Maximum number of neighbours for this board node

    public BoardNode(int maxNeighbours, String name) {
        super(CoreConstants.ComponentType.BOARD_NODE, name);
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    public BoardNode() {
        this(-1, "");
    }

    protected BoardNode(int maxNeighbours, String name, int ID) {
        super(CoreConstants.ComponentType.BOARD_NODE, name, ID);
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    /**
     * Adds a neighbour for this node.
     * @param neighbour - new neighbour of this node.
     */
    public void addNeighbour(BoardNode neighbour) {
        if (neighbours.size() <= maxNeighbours || maxNeighbours == -1) {
            neighbours.add(neighbour);
        }
    }

    /**
     * Removes neighbour of this node.
     * @param neighbour - neighbour to remove.
     * @return - true if removed successfully, false otherwise. may fail if neighbour didn't exist in the first place.
     */
    public boolean removeNeighbour(BoardNode neighbour) {
        if (neighbours.contains(neighbour)) {
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
    public boolean addNeighbour(BoardNode neighbour, int side) {
        if (neighbours.size() <= maxNeighbours && side <= maxNeighbours || maxNeighbours == -1) {
            if (!(neighbours.contains(neighbour)) && !(neighbourSideMapping.containsKey(neighbour))) {
                neighbours.add(neighbour);
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
        // WARNING: DO not copy this directly, the GraphBoard copies it to correctly assign neighbour references!
        return null;
    }

    /**
     * @return the neighbours of this node.
     */
    public HashSet<BoardNode> getNeighbours() {
        return neighbours;
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
            sb.append(prop.getHashString() + ": " +  prop.toString() + "; ");
        }

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return componentID;
    }
}

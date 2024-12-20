package games.chinesecheckers.components;

import core.CoreConstants;
import core.components.BoardNode;
import core.components.Component;
import core.properties.Property;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.*;

public class CCNode extends Component {

    private Peg.Colour colour;

    private Peg occupiedPeg;

    private int x;
    private int y;

    private Set<CCNode> neighbours;  // Neighbours of this board node
    private Map<CCNode, Integer> neighbourSideMapping;  // Neighbours mapping to a side of this board node
    private int maxNeighbours = 6;  // Maximum number of neighbours for this board node


    public CCNode() {
        super(CoreConstants.ComponentType.BOARD_NODE, "CC", 0);
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    public CCNode(int id) {
        super(CoreConstants.ComponentType.BOARD_NODE, "CC", id);
        colour = Peg.Colour.neutral;
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();
    }

    public void setOccupiedPeg(Peg peg) {
        occupiedPeg = peg;
    }

    public void setColourNode(Peg.Colour colour) {
        this.colour = colour;
    }

    public void setCoordinates(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public Peg getOccupiedPeg() {
        if (occupiedPeg == null) return null;
        return occupiedPeg;
    }

    public int getID() {
        return componentID;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Peg.Colour getBaseColour() {
        return colour;
    }

    public boolean isNodeOccupied() {
        return occupiedPeg != null;
    }


    /**
     * Adds a neighbour for this node.
     *
     * @param neighbour - new neighbour of this node.
     */
    public void addNeighbour(CCNode neighbour) {
        if (neighbours.size() <= maxNeighbours || maxNeighbours == -1) {
            neighbours.add(neighbour);
        }
    }

    /**
     * Removes neighbour of this node.
     *
     * @param neighbour - neighbour to remove.
     */
    public void removeNeighbour(CCNode neighbour) {
        if (neighbours.contains(neighbour)) {
            neighbours.remove(neighbour);
            neighbourSideMapping.remove(neighbour);
        }
    }

    /**
     * Adds neighbour to specific side of this node.
     *
     * @param neighbour - new neighbour to be added.
     * @param side      - side of this node to be added in.
     * @return - true if added successfully, false otherwise. may fail if too many neighbours added already.
     */
    public void addNeighbour(CCNode neighbour, int side) {
        if (neighbours.size() <= maxNeighbours && side <= maxNeighbours || maxNeighbours == -1) {
            if (!(neighbours.contains(neighbour)) && !(neighbourSideMapping.containsKey(neighbour))) {
                neighbours.add(neighbour);
                neighbourSideMapping.put(neighbour, side);
            }
        }
    }

    /**
     * Copies all node properties to a new instance of this node.
     *
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
    public Set<CCNode> getNeighbours() {
        return neighbours;
    }

    /**
     * @return the neighbours mapping to sides of this node.
     */
    public Map<CCNode, Integer> getNeighbourSideMapping() {
        return neighbourSideMapping;
    }

    public void loadBoardNode(JSONObject node) {
        this.componentName = (String) ((JSONArray) node.get("name")).get(1);
        parseComponent(this, node);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{id: " + componentID + "; maxNeighbours: " + maxNeighbours + "; ");
        for (int prop_key : properties.keySet()) {
            Property prop = properties.get(prop_key);
            sb.append(prop.getHashString() + ": " + prop.toString() + "; ");
        }
        return sb.toString();
    }

    @Override
    public final int hashCode() {
        return Objects.hash(componentID);
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof CCNode) {
            CCNode other = (CCNode) o;
            return componentID == other.componentID && occupiedPeg.equals(other.occupiedPeg) && x == other.x && y == other.y && Objects.equals(neighbours, other.neighbours) && Objects.equals(neighbourSideMapping, other.neighbourSideMapping) && maxNeighbours == other.maxNeighbours;
        }
        return false;
    }


}

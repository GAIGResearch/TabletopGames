package components;

import content.Property;
import utilities.Vector2D;

import java.util.HashMap;
import java.util.HashSet;

public class BoardNode extends Component{

    public static int idCounter = 0;  // Used to set unique ids for all nodes added.

    private int id;  // unique id of this board node

    private Vector2D position;  // physical position of this board node

    private HashSet<BoardNode> neighbours;  // neighbours of this board node
    private HashMap<BoardNode, Integer> neighbourSideMapping;  // neighbours mapping to a side of this board node
    private int maxNeighbours;  // maximum number of neighbours for this board node

    public BoardNode() {
        this.properties = new HashMap<>();
        this.id = idCounter;
        this.position = new Vector2D();
        this.maxNeighbours = -1;
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();

        idCounter++;
    }

    public BoardNode(int maxNeighbours, Vector2D position) {
        this.properties = new HashMap<>();
        this.id = idCounter;
        this.position = position;
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();

        idCounter++;
    }



    /**
     * Adds a neighbour for this node.
     * @param neighbour - new neighbour of this node.
     * @return - true if added successfully, false otherwise. may fail if too many neighbours added.
     */
    public boolean addNeighbour(BoardNode neighbour) {
        if (neighbours.size() <= maxNeighbours || maxNeighbours == -1) {
            if (!(neighbours.contains(neighbour))) {
                neighbours.add(neighbour);
                return true;
            }
        }
        return false;
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
    public BoardNode copy() {
        BoardNode copy = new BoardNode(maxNeighbours, position.copy());
        copy.id = id;
        idCounter--;  // This increases automatically in constructor, but we don't need that if we're copying the ID
        copy.neighbours = new HashSet<>(neighbours);
        copy.neighbourSideMapping = new HashMap<>(neighbourSideMapping);

        //copy type and component.
        copyComponentTo(copy);

        return copy;
    }

    /**
     * @return the ID of this node
     */
    public int getId() {
        return id;
    }


    /**
     * @return the neighbours of this node
     */
    public HashSet<BoardNode> getNeighbours() {
        return neighbours;
    }

    /**
     * @return the neighbours mapping to sides of this node
     */
    public HashMap<BoardNode, Integer> getNeighbourSideMapping() {
        return neighbourSideMapping;
    }

    /**
     * @return the physical position of this board node
     */
    public Vector2D getPosition() {
        return position;
    }

    /**
     * Hashcode of this node.
     * @return its unique id.
     */
    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof BoardNode)) return false;

        BoardNode otherBoardNode = (BoardNode) obj;

        if(otherBoardNode.id != id)
            return false;

        for (BoardNode n: otherBoardNode.neighbours) {
            if (!(neighbours.contains(n))) {
                return false;
            }
        }

        if(properties.size() != otherBoardNode.getNumProperties())
            return false;

        for(int prop_key : properties.keySet())
        {
            Property prop = properties.get(prop_key);
            Property otherProp = otherBoardNode.getProperty(prop_key);

            if(!prop.equals(otherProp))
                return false;
        }

        return true;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("{id: " + id + "; maxNeighbours: " + maxNeighbours + "; ");
        for(int prop_key : properties.keySet()) {
            Property prop = properties.get(prop_key);
            sb.append(prop.getHashString() + ": " +  prop.toString() + "; ");
        }

        return sb.toString();
    }


    public void setMaxNeighbours(int maxNeighbours) {
        this.maxNeighbours = maxNeighbours;
    }
}

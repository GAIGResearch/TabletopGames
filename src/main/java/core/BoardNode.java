package core;

import utilities.Vector2D;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

public class BoardNode {
    public static int idCounter = 0;  // Used to set unique ids for all nodes added.

    private int id;  // unique id of this board node
    private String name;  // name of this board node
    private Color color;  // color of this board node
    private Vector2D position;  // physical position of this board node

    private HashSet<BoardNode> neighbours;  // neighbours of this board node
    private HashMap<BoardNode, Integer> neighbourSideMapping;  // neighbours mapping to a side of this board node
    private int maxNeighbours;  // maximum number of neighbours for this board node

    public BoardNode() {
        this.name = "";
        this.color = Color.GRAY;
        this.id = idCounter;
        this.position = new Vector2D();
        this.maxNeighbours = -1;
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();

        idCounter++;
    }

    public BoardNode(int maxNeighbours, String name, Color color, Vector2D position) {
        this.name = name;
        this.color = color;
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
        BoardNode copy = new BoardNode(maxNeighbours, name, color, position.copy());
        copy.id = id;
        idCounter--;  // This increases automatically in constructor, but we don't need that if we're copying the ID
        copy.neighbours = new HashSet<>(neighbours);
        copy.neighbourSideMapping = new HashMap<>(neighbourSideMapping);

        return copy;
    }

    /**
     * @return the ID of this node
     */
    public int getId() {
        return id;
    }

    /**
     * @return the color of this node
     */
    public Color getColor() {
        return color;
    }

    /**
     * @return the name of this node
     */
    public String getName() {
        return name;
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
        for (BoardNode n: ((BoardNode) obj).neighbours) {
            if (!(neighbours.contains(n))) {
                return false;
            }
        }
        return ((BoardNode) obj).id == id && ((BoardNode) obj).color == color && ((BoardNode) obj).name.equals(name);
    }

    @Override
    public String toString() {
        String[] neighbourNames = new String[neighbours.size()];
        int i = 0;
        for (BoardNode b: neighbours) {
            neighbourNames[i] = b.getName();
            i++;
        }
        return "{id: " + id + "; name: " + name + "; color: " + color + "; neighbours: " + Arrays.toString(neighbourNames) +
                "; maxNeighbours: " + maxNeighbours + "}";
    }
}

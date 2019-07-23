package core;

import java.awt.*;
import java.util.HashMap;
import java.util.HashSet;

public class BoardNode {
    public static int idCounter = 0;

    private int id;
    private String name;
    private Color color;

    private HashSet<BoardNode> neighbours;
    private HashMap<BoardNode, Integer> neighbourSideMapping;
    private int maxNeighbours;

    public BoardNode(int maxNeighbours, String name, Color color) {
        this.name = name;
        this.color = color;
        this.id = idCounter;
        this.maxNeighbours = maxNeighbours;
        this.neighbours = new HashSet<>();
        this.neighbourSideMapping = new HashMap<>();

        idCounter++;
    }

    public boolean addNeighbour(BoardNode neighbour) {
        if (neighbours.size() <= maxNeighbours || maxNeighbours == -1) {
            neighbours.add(neighbour);
            return true;
        }
        return false;
    }

    public boolean removeNeighbour(BoardNode neighbour) {
        if (neighbours.contains(neighbour)) {
            neighbours.remove(neighbour);
            neighbourSideMapping.remove(neighbour);
            return true;
        }
        return false;
    }

    public boolean addNeighbour(BoardNode neighbour, int side) {
        if (neighbours.size() <= maxNeighbours && side <= maxNeighbours || maxNeighbours == -1) {
            neighbours.add(neighbour);
            neighbourSideMapping.put(neighbour, side);
            return true;
        }
        return false;
    }

    public BoardNode copy() {
        BoardNode copy = new BoardNode(maxNeighbours, name, color);
        copy.neighbours = new HashSet<>(neighbours);
        copy.neighbourSideMapping = new HashMap<>(neighbourSideMapping);

        return copy;
    }

    public int getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public String getName() {
        return name;
    }

    public HashSet<BoardNode> getNeighbours() {
        return neighbours;
    }

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
}

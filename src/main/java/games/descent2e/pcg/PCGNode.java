package games.descent2e.pcg;

import org.netlib.blas.Dcopy;

import java.util.*;

public class PCGNode {

    public static Connection[] connections = {Connection.NORTH, Connection.EAST, Connection.SOUTH, Connection.WEST};

    public String name;
    public List<Connection> connects = new ArrayList<>();
    public HashMap<Connection, String> neighbours = new HashMap<>(); // <Connection, Tile>
    int orientation = 0; // Rotate clockwise, 0 = 0°, 1 = 90°, 2 = 180°, 3 = 270°
    int maxConnections = 0;
    int size = 0;
    int nodeID = 0;

    public PCGNode(String name) {
        this.name = name;
    }

    public PCGNode(String name, List<Connection> connects, int orientation) {
        this.name = name;
        this.connects.addAll(connects);
        this.orientation = orientation;
    }

    public PCGNode(String name, List<Connection> connects, HashMap<Connection, String> neighbours, int orientation, int maxConnections, int size, int nodeID) {
        this.name = name;
        this.connects.addAll(connects);
        this.orientation = orientation;
        for (Connection c : neighbours.keySet()) {
            this.neighbours.put(c, neighbours.get(c));
        }
        this.maxConnections = maxConnections;
        this.size = size;
        this.nodeID = nodeID;
    }

    public PCGNode copy() {
        return new PCGNode(name, connects, neighbours, orientation, maxConnections, size, nodeID);
    }

    public void rotate(int r) {
        orientation = (orientation + r) % 4;
        for (int i = 0; i < connects.size(); i ++) {
            Connection c = connects.get(i);
            int id = Arrays.asList(connections).indexOf(c);
            if (id > -1) {
                c = connections[(id + r) % 4];
                connects.set(i, c);
            }
        }
    }

    public void addNeighbour(Connection c, String tile) {
        neighbours.put(c, tile);
    }

    public void clearNeighbours() {
        neighbours.clear();
    }
}

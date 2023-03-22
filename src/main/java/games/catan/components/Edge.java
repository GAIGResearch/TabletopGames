package games.catan.components;

import core.CoreConstants;
import core.components.Component;

import java.util.Objects;

/*
 * Generic implementation of an edge between 2 nodes of the same type
 * Edge encapsulates another object
 * */
public class Edge extends Component {
    Building src;
    Building dest;
    Road value;

    public Edge(Building srcNode, Building destNode, Road edgeValue) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge");
        this.src = srcNode;
        this.dest = destNode;
        this.value = edgeValue;
    }
    public Edge(Building srcNode, Building destNode, Road edgeValue, int id) {
        super(CoreConstants.ComponentType.BOARD_NODE, "Edge", id);
        this.src = srcNode;
        this.dest = destNode;
        this.value = edgeValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Edge)) return false;
        Edge edge = (Edge) o;
        return Objects.equals(src, edge.src) && Objects.equals(dest, edge.dest) && Objects.equals(value, edge.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(src, dest, value);
    }

    @Override
    public String toString() {
        // prints references
        return "Edge{" +
                "src=" + src +
                ", dest=" + dest +
                ", value=" + value +
                '}';
    }

    public Building getDest() {
        return dest;
    }

    public Building getSrc() {
        return src;
    }

    public Road getRoad() {
        return value;
    }

    public Edge copy(){
        return new Edge(src.copy(), dest.copy(), value.copy(), componentID);
    }
}

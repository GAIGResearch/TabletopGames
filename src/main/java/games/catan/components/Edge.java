package games.catan.components;

import core.components.Component;

import java.util.Objects;

/*
 * Generic implementation of an edge between 2 nodes of the same type
 * Edge encapsulates another object
 * */
public class Edge<N extends Copiable, E extends Copiable> {
    N src;
    N dest;
    E value;

    public Edge(N srcNode, N destNode, E edgeValue) {
        this.src = srcNode;
        this.dest = destNode;
        this.value = edgeValue;
    }

    @Override
    public boolean equals(Object o) {
        // todo (mb) probably road setting is wrong - should look into it -> they never have the same refs
        // compare by reference
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge<?, ?> edge = (Edge<?, ?>) o;
        return src == (edge.src) &&
                dest == (edge.dest); // &&
//                value == (edge.value);
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

    public N getSrc() {
        return src;
    }

    public N getDest() {
        return dest;
    }

    public E getValue() {
        return value;
    }

    public Edge<N, E> copy(){
        Edge<N, E> copy = new Edge(src.copy(), dest.copy(), value.copy());
        return copy;
    }
}

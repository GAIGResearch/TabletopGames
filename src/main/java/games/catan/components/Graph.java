package games.catan.components;

import java.util.*;

/* Implementation of a generic Graph using generic edges */
public class Graph<N, E> {
    private Map<N, List<Edge<N, E>>> map;

    public Graph(){
        map = new HashMap<>();
    }

    public void addEdge(N src, N dest, E value){
        Edge<N, E> edge = new Edge<>(src, dest, value);
        if (map.get(src) == null){
            ArrayList<Edge<N, E>> list = new ArrayList<>();
            list.add(edge);
            map.put(src, list);
        }
        else{
            List<Edge<N, E>> edges = map.get(src);
            if (!edges.contains(edge)){
                edges.add(edge);
            }
        }
    }

    /* Returns the connected nodes from a node
    *  */
    public List<N> getNeighbourNodes(N src){
        List<Edge<N, E>> edges = map.get(src);
        ArrayList<N> destinations = new ArrayList<>();
        for (Edge<N, E> edge: edges){
            destinations.add(edge.dest);
        }
        return destinations;
    }

    /* Returns the edges starting from the current node
     *  */
    public List<E> getEdges(N src){
        List<Edge<N, E>> edges = map.get(src);
        ArrayList<E> nodes = new ArrayList<>();
        for (Edge<N, E> edge: edges){
            nodes.add(edge.value);
        }
        return nodes;
    }

    /* Iterates over all entries and prints the result */
    public void printGraph(){
        Set<N> set = map.keySet();
        for (N vertex : set) {
            System.out.println("Vertex " + vertex + " is connected to ");
            List<Edge<N, E>> list = map.get(vertex);
            for (Edge<N, E> neEdge : list) {
                System.out.print(neEdge + " ");
            }
            System.out.println();
        }
    }

}

/*
* Generic implementation of an edge between 2 nodes of the same type
* Edge encapsulates another object
* */
class Edge<N, E>{
    N src;
    N dest;
    E value;

    public Edge(N srcNode, N destNode, E edgeValue){
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
}

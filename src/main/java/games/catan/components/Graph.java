package games.catan.components;

import java.util.*;

/* Implementation of a generic Graph using generic edges */
public class Graph<N extends Copiable, E extends Copiable> {
    private final Map<N, List<Edge<N, E>>> map;

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
//        for (Map.EntrySet entry: map.entrySet){System.out.println((List<Settlement>)entry.getValue()))}
//        for (Map.EntrySet entry: map.entrySet) {
//            System.out.println(((Settlement) entry.getKey()).id);
//        }
        List<Edge<N, E>> edges = map.get(src);
        ArrayList<N> destinations = new ArrayList<>();
        for (Edge<N, E> edge: edges){
            destinations.add(edge.dest);
        }
        return destinations;
    }

    /* Returns the edges starting from the current node
     *  */
    public List<E> getConnections(N src){
        List<Edge<N, E>> edges = map.get(src);
        ArrayList<E> nodes = new ArrayList<>();
        for (Edge<N, E> edge: edges){
            nodes.add(edge.value);
        }
        return nodes;
    }

    /* Returns the the edges [src, dest, edge]
    *  */
    public List<Edge<N, E>> getEdges(N src){
        return map.get(src);
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

    public Graph<N, E> copy(){
        // todo problem seem to be memory location vs ID
        Graph<N, E> copy = new Graph<>();
        for (Map.Entry<N, List<Edge<N, E>>> entry : map.entrySet()) {
            List<Edge<N, E>> edgeList = new ArrayList<>();
            for (Edge<N, E> edge: entry.getValue()){
                edgeList.add(edge.copy());
            }
            copy.map.put((N) entry.getKey().copy(), edgeList);
        }
        return copy;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Graph) {
            Graph<N, E> o = (Graph<N, E>) other;
            if (o.map.size() == ((Graph<?, ?>) other).map.size()) {
                for (N key : map.keySet()) {
                    List<Edge<N, E>> edges = map.get(key);
                    List<Edge<N, E>> otherEdges = o.map.get(key);
                    if (edges != null && otherEdges != null && edges.size() == otherEdges.size()) {
                        if (!edges.equals(otherEdges)) return false;
                    } else {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (Map.Entry<N, List<Edge<N, E>>> entry : map.entrySet()) {
            result = result * 31 + entry.hashCode();
        }
        return result;
    }
}


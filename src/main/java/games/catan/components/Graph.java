package games.catan.components;

import core.components.GraphBoard;

import java.util.*;

/* Implementation of a generic Graph using generic edges */
public class Graph extends GraphBoard {
    private final Map<Building, List<Edge>> map;

    public Graph(){
        super("Graph");
        map = new HashMap<>();
    }
    public Graph(int id){
        super("Graph", id);
        map = new HashMap<>();
    }

    public void addEdge(Building src, Building dest, Road value){
        Edge edge = new Edge(src, dest, value);
        if (map.get(src) == null){
            ArrayList<Edge> list = new ArrayList<>();
            list.add(edge);
            map.put(src, list);
        }
        else{
            List<Edge> edges = map.get(src);
            if (!edges.contains(edge)){
                edges.add(edge);
            }
        }
    }

    /* Returns the connected nodes from a node
    *  */
    public List<Building> getNeighbourNodes(Building src){
//        for (Map.EntrySet entry: map.entrySet){System.out.println((List<Settlement>)entry.getValue()))}
//        for (Map.EntrySet entry: map.entrySet) {
//            System.out.println(((Settlement) entry.getKey()).id);
//        }
        List<Edge> edges = map.get(src);
        ArrayList<Building> destinations = new ArrayList<>();
        for (Edge edge: edges){
            destinations.add(edge.dest);
        }
        return destinations;
    }

    /* Returns the edges starting from the given node
     *  */
    public List<Edge> getConnections(Building src){
        return map.get(src);
    }

    /* Returns the edges [src, dest, edge]
    *  */
    public List<Edge> getEdges(Building src){
        return map.get(src);
    }

    /* Iterates over all entries and prints the result */
    public void printGraph(){
        Set<Building> set = map.keySet();
        for (Building vertex : set) {
            System.out.println("Vertex " + vertex + " is connected to ");
            List<Edge> list = map.get(vertex);
            for (Edge neEdge : list) {
                System.out.print(neEdge + " ");
            }
            System.out.println();
        }
    }

    public Graph copy(){
        // todo problem seem to be memory location vs ID
        Graph copy = new Graph(componentID);
        for (Map.Entry<Building, List<Edge>> entry : map.entrySet()) {
            List<Edge> edgeList = new ArrayList<>();
            for (Edge edge: entry.getValue()){
                edgeList.add(edge.copy());
            }
            copy.map.put(entry.getKey().copy(), edgeList);
        }
        return copy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Graph)) return false;
        Graph graph = (Graph) o;
        return Objects.equals(map, graph.map);
    }

    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
}


package utilities;
import core.AbstractGameState;
import core.components.BoardNode;
import core.components.GridBoard;
import core.properties.PropertyVector2D;

import java.util.*;

import static core.CoreConstants.coordinateHash;
/**
 * Path finding utility. Pre-computes and caches shortest paths between nodes.
 * At the moment, it uses GridBoard objects as nodes in the pathfinding graph.
 */
public class Pathfinder {

    /**
     * Graph to be used for the path finding.
     * TODO: This could become a GraphBoard at some point.
     */
    private GridBoard graph;

    /**
     * Cache of shortest path from node i to the others
     */
    private TreeMap<Integer, TreeMap<Integer, Path>> shortestPaths;

    /**
     * Utility for comparing nodes in A*'s priority queue.
     */
    private NodeComparator nodeComparator;


    /**
     * Constructor of the pathfinder.
     * @param g Graph of the game.
     */
    public Pathfinder(GridBoard g)
    {
        graph = g;
        shortestPaths = new TreeMap<>();
        nodeComparator = new NodeComparator();

        initShortestPaths();
    }

    private void initShortestPaths()
    {
        int w = graph.getWidth();
        int h = graph.getHeight();
        for(int i = 0; i < w; i++)
        {
            for (int j = 0; j < h; j++)
            {
                BoardNode n = graph.getElement(i,j);
                if(n != null)
                    initShortestPaths(n);
            }
        }
    }

    /**
     * Initializes the cache of shortest paths from a given node.
     * @param origin node the paths start in.
     */
    private void initShortestPaths(BoardNode origin)
    {
        HashMap<BoardNode, Double> nodeNeighbours = origin.getNeighbours();
        int originID = origin.getComponentID();

        for(BoardNode node : nodeNeighbours.keySet()) {
            assignCost(originID, node.getComponentID(), nodeNeighbours.get(node));
        }

        //Default one, to itself
        assignCost(originID, originID, 0);
    }

    public void notifyNewNode()
    {
        initShortestPaths();
    }

    /**
     * Assigns a cost to a path between origin and destination.
     * @param originID Origin node id.
     * @param destID Destination node id.
     * @param cost Cost from a_originID to a_destID
     */
    private void assignCost(int originID, int destID, double cost)
    {
        //1st, get the origin:
        TreeMap<Integer, Path> originPaths = shortestPaths.get(originID);
        if(originPaths == null)
        {
            originPaths = new TreeMap<>();

            //There will be no destination, for sure, so lets assign it.
            Path newPath = new Path(originID, destID, cost);
            originPaths.put(destID, newPath);

            //To the array!
            shortestPaths.put(originID, originPaths);

            //And that's all
            return;
        }

        //2nd, destination
        Path pathToDest = originPaths.get(destID);
        if(pathToDest == null)
        {
            //no path, create it and insert.
            Path newPath = new Path(originID, destID, cost);
            originPaths.put(destID, newPath);

            //And that's all
            return;
        }

        //3rd, there is a path, no cost; assign.
        pathToDest.cost = cost;
    }

    /**
     * Gets the shortest path from origin to destination, stored in the shortestPath cache.
     * Returns an empty path with maximum cost (Double.MAX_VALUE) if no path can be found.
     * @param origin origin node id.
     * @param destination destination node id.
     * @return Path from origin to destination, an empty path with maximum cost (Double.MAX_VALUE) if not found.
     */
    private Path getShortestPath(int origin, int destination)
    {
        Path p = shortestPaths.get(origin).get(destination);
        if(p == null)
            return new Path(origin, destination, Double.MAX_VALUE);
        return p;
    }

    /**
     * Returns all the shortest paths in cache from a given node.
     * @param origin origin node id.
     * @return a TreeMap with all the paths from the origin node.
     */
    private TreeMap<Integer, Path> getShortestPaths(int origin){return shortestPaths.get(origin);}

    /**
     * Includes the given path in the shortest paths cache.
     * @param p Path to include.
     */
    private void setShortestPath(Path p)
    {
        TreeMap<Integer, Path> shortest = getShortestPaths(p.originID);
        shortest.put(p.destinationID, p);
    }

    /**
     * Gets a path between two nodes in the graph.  It checks the cache of shortest paths to see if it was calculated before.
     * @param gState game state with access to the board.
     * @param origin origin node id.
     * @param destination destination node id.
     * @return the path from a_origin to a_destination.
     */
    public Path getPath(AbstractGameState gState, int origin, int destination)
    {
        Path shortestP = getShortestPath(origin, destination);
        if(shortestP.cost != Double.MAX_VALUE)
            //The path was there.
            return shortestP;

        //No path, need to calculate with A*
        Path p = new Path(origin, destination);
        boolean pathFound = _a_star(gState, p);

        if(!pathFound)
            //If path could not be found, clear it
            p.points.clear();
        else
            //Else, extract the path
            p = getShortestPath(origin,destination);

        //Return the path if it is meaningful.
        if (p.points.size() > 0 && p.cost < Integer.MAX_VALUE)
            return p;

        //If not, return an empty path.
        else return new Path(origin, origin);
    }

    /**
     * A* (A Star) method, to calculate the shortest path between the nodes path.originID and path.destinationID
     * @param gState game state
     * @param path Path to fill with intermediate nodes.
     * @return true if the path could be found.
     */
    private boolean _a_star(AbstractGameState gState, Path path)
    {
        //Sets of evaluated and not evaluated nodes.
        TreeSet<Integer> evaluatedSet = new TreeSet<>();
        TreeSet<Integer> toEvaluateSetMarker = new TreeSet<>();
        PriorityQueue<BoardNode> toEvaluateSet = new PriorityQueue<>(1000, nodeComparator);

        //Initialize current node (origin).
        BoardNode currentNode = (BoardNode) gState.getComponentById(path.originID);
        BoardNode destinationNode = (BoardNode) gState.getComponentById(path.destinationID);
        toEvaluateSet.add(currentNode);
        toEvaluateSetMarker.add(currentNode.getComponentID());

        nodeComparator.nodeGCosts.put(currentNode.getComponentID(), 0.0);
        nodeComparator.nodeHCosts.put(currentNode.getComponentID(), heuristic(currentNode, destinationNode));

        //Check while there are still nodes in the array of nods to evaluate.
        while(!toEvaluateSet.isEmpty())
        {
            //Take next node to evaluate.
            currentNode = toEvaluateSet.poll();
            int currentNodeId = currentNode.getComponentID();
            evaluatedSet.add(currentNode.getComponentID());

            //If destination found, that's it.
            if(currentNode.getComponentID() == path.destinationID)
            {
                return true;
            }

            //For all connections from the current node...
            HashMap<BoardNode, Double> conn = currentNode.getNeighbours();
            for(BoardNode connected : conn.keySet())
            {
                //If it has not been evaluated yet.
                if(!evaluatedSet.contains(connected.getComponentID()))
                {
                    // Cost from origin to 'connected' stored
                    Path D1 = getShortestPath(path.originID,connected.getComponentID());
                    // Cost from origin to current node stored
                    Path DA = getShortestPath(path.originID,currentNodeId);
                    // Cost from current to connected (edge cost)
                    double dA1 = conn.get(connected);

                    //Path to this node
                    PathCH pc = new PathCH();
                    pc.p = D1;
                    pc.destID = connected.getComponentID();

                    //If the new cost is smaller.
                    double newCost = DA.cost + dA1;
                    if(D1.cost > newCost)
                    {
                        //update cost
                        Path newD1 = new Path(DA);
                        newD1.destinationID = connected.getComponentID();
                        newD1.cost += dA1;
                        //update path
                        pc.p = newD1;
                        newD1.points.add(connected.getComponentID());
                        setShortestPath(newD1);
                    }

                    //Set cost, used by priority queue to navigate more efficiently
                    nodeComparator.nodeGCosts.put(connected.getComponentID(), pc.p.cost);
                    nodeComparator.nodeHCosts.put(connected.getComponentID(), heuristic(connected, destinationNode));

                    //connected.m_g = pc.p.cost;
                    //connected.m_f = pc.heuristicCost = pc.p.cost + heuristic(currentNode, destinationNode);
                    if(!toEvaluateSetMarker.contains(connected.getComponentID()))
                    {
                        //Mark this node as evaluated.
                        toEvaluateSet.add(connected);
                        toEvaluateSetMarker.add(connected.getComponentID());
                    }

                }
            }
        }

        //If we didn't find the destination, the path could not be extracted.
        return false;
    }

    /**
     * Heuristic for A*: euclidean distance.
     * @param origin origin node.
     * @param destination destination node.
     * @return the euclidean distance between origin and destination.
     */
    private double heuristic(BoardNode origin, BoardNode destination)
    {
        Vector2D originLoc = ((PropertyVector2D) origin.getProperty(coordinateHash)).values;
        Vector2D destLoc = ((PropertyVector2D) destination.getProperty(coordinateHash)).values;
        return Distance.euclidian_distance(  new double[]{originLoc.getX(),  originLoc.getY()},
                                             new double[]{destLoc.getX(),    destLoc.getY()});
    }

}


/**
 * CLASS to compare two nodes by the heuristic cost.
 */
class NodeComparator implements Comparator<BoardNode>
{

    /**
     * Cache for graph costs (no heuristic)
     */
    public TreeMap<Integer, Double> nodeGCosts;

    /**
     * Cache for heuristic costs
     */
    public TreeMap<Integer, Double> nodeHCosts;

    /**
     * Constructor that initializes the above caches.
     */
    public NodeComparator()
    {
        nodeGCosts = new TreeMap<>();
        nodeHCosts = new TreeMap<>();
    }

    /**
     * Compares two nodes.
     * @param x one node.
     * @param y another node.
     * @return returns -1, 0, or 1 depending on the nodes: -1 if x has a smaller heuristic cost than y, 1 if opposite, 0 if it is the same cost.
     */
    public int compare(BoardNode x, BoardNode y)
    {
        double xCost = nodeGCosts.get(x.getComponentID()) + nodeHCosts.get(x.getComponentID());
        double yCost = nodeGCosts.get(y.getComponentID()) + nodeHCosts.get(y.getComponentID());
        return Double.compare(xCost, yCost);
    }
}

///**
// * CLASS to compare two paths by the heuristic cost.
// */
//class PathCHComparator implements Comparator<PathCH>
//{
//    /**
//     * Compares two paths.
//     * @param x one path.
//     * @param y another path.
//     * @return returns -1, 0, or 1 depending on the pat:hs -1 if x has a smaller heuristic cost than y, 1 if opposite, 0 if it is the same cost.
//     */
//    public int compare(PathCH x, PathCH y)
//    {
//        // Assume neither PathCH is null. Real code should
//        // probably be more robust
//        return Double.compare(x.heuristicCost, y.heuristicCost);
//    }
//}

/**
 * Helper CLASS used by A*
 */
class PathCH
{
    /**
     * Path
     */
    public Path p;

    /**
     * Destination node.
     */
    public int destID;

    /**
     * Heuristic cost
     */
    public double heuristicCost;

    /**
     * Overrides toString() [debug purposes].
     * @return A string representation of the path
     */
    @Override
    public String toString() {return "[" + p.toString() + "] hC: " + heuristicCost; }
}

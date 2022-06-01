package utilities;
import java.util.Vector;

/**
 * Represents a path from one node to another
 * Code adapted from the PTSP Framework (https://github.com/diegopliebana/MultiobjectiveMCTS)
 * Date: 30/05/22
 */
public class Path
{
    /**
     * Identifier of the origin's node of the path
     */
    public int originID;

    /**
     * Identifier of the destination's node of the path
     */
    public int destinationID;

    /**
     * Cost of the complete path.
     */
    public double cost;

    /**
     * IDs of the nodes this path is formed by.
     */
    public Vector<Integer> points;

    /**
     * Creates an empty path from the origin to the end.
     * @param start origin node id of the path.
     * @param end destination node id of the path.
     */
    public Path(int start, int end)
    {
        originID = start;
        destinationID = end;
        cost = Integer.MAX_VALUE;
        points = new Vector<Integer>();

        //These two points MUST be in the path
        points.add(start);
        if(start != end) points.add(end);
    }

    /**
     * Creates an empty path from the origin to the end, giving a cost.
     * @param start origin node id of the path.
     * @param end destination node id of the path.
     * @param costP cost of the path.
     */
    public Path(int start, int end, double costP)
    {
        originID = start;
        destinationID = end;
        cost = costP;
        points = new Vector<Integer>();

        //These two points MUST be in the path!!
        points.add(start);
        if(start != end) points.add(end);
    }

    /**
     * Creates a path as a copy of another path.
     * @param p path to copy from.
     */
    public Path(Path p)
    {
        originID = p.originID;
        destinationID = p.destinationID;
        cost = p.cost;
        points = new Vector<Integer>();
        points.addAll(p.points);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder("Path from " + originID + " to " + destinationID + " with cost "  + cost + " goes through: ");
        for (Integer point : points) {
            sb.append(point).append(", ");
        }
        return sb.toString();
    }

}

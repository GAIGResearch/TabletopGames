package core.components;

import core.properties.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;
import utilities.Utils.ComponentType;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static core.CoreConstants.imgHash;
import static core.CoreConstants.nameHash;

public class GraphBoard extends Component {

    // List of nodes in the board graph
    protected List<BoardNode> boardNodes;

    public GraphBoard(String name)
    {
        super(ComponentType.BOARD, name);
        boardNodes = new ArrayList<>();
    }

    public GraphBoard()
    {
        super(ComponentType.BOARD);
        boardNodes = new ArrayList<>();
    }

    private GraphBoard(String name, int ID)
    {
        super(ComponentType.BOARD, name, ID);
        boardNodes = new ArrayList<>();
    }

    private GraphBoard(int ID)
    {
        super(ComponentType.BOARD, ID);
        boardNodes = new ArrayList<>();
    }

    /**
     * Copy method, to be implemented by all subclasses.
     * @return - a new instance of this Board, deep copy.
     */
    @Override
    public GraphBoard copy()
    {
        GraphBoard b = new GraphBoard(componentName, componentID);
        HashMap<Integer, BoardNode> nodeCopies = new HashMap<>();
        // Copy board nodes
        for (BoardNode bn: boardNodes) {
            BoardNode bnCopy = new BoardNode(bn.getMaxNeighbours(), "", bn.getComponentID());
            copyComponentTo(bnCopy);
            nodeCopies.put(bn.getComponentID(), bnCopy);
        }
        // Assign neighbours
        for (BoardNode bn: boardNodes) {
            BoardNode bnCopy = nodeCopies.get(bn.getComponentID());
            for (BoardNode neighbour: bn.getNeighbours()) {
                bnCopy.addNeighbour(nodeCopies.get(neighbour.getComponentID()));
            }
            for (Map.Entry<BoardNode, Integer> e: bn.getNeighbourSideMapping().entrySet()) {
                bnCopy.addNeighbour(nodeCopies.get(e.getKey().componentID), e.getValue());
            }
        }
        // Assign new neighbours
        b.setBoardNodes(new ArrayList<>(nodeCopies.values()));
        // Copy properties
        copyComponentTo(b);
        return b;
    }

    /**
     * Returns the node in the list which matches the given property
     * @param prop_id - ID of the property to look for.
     * @param p - Property that has the value to look for.
     * @return - node matching property.
     */
    public BoardNode getNodeByProperty(int prop_id, Property p) {
        for (BoardNode n : boardNodes) {
            Property prop = n.getProperty(prop_id);
            if(prop != null)
            {
                if(prop.equals(p))
                    return n;
            }
        }
        return null;
    }

    /**
     * Returns the node in the list which matches the given string property
     * @param prop_id - ID of the property to look for.
     * @param value - String value for the property.
     * @return - node matching property
     */
    public BoardNode getNodeByStringProperty(int prop_id, String value)
    {
        return getNodeByProperty(prop_id, new PropertyString(value));
    }

    /**
     * @return the list of board nodes
     */
    public List<BoardNode> getBoardNodes() {
        return boardNodes;
    }

    /**
     * Returns the node in the list which matches the given ID
     * @param id - ID of node to search for.
     * @return - node matching ID.
     */
    protected BoardNode getNodeByID(int id) {
        for (BoardNode n : boardNodes) {
            if (n.componentID == id) return n;
        }
        return null;
    }

    /**
     * Sets the list of board nodes to the given list.
     * @param boardNodes - new list of board nodes.
     */
    private void setBoardNodes(List<BoardNode> boardNodes) {
        this.boardNodes = boardNodes;
    }

    /**
     * Loads all boards from a JSON file.
     * @param filename - path to file.
     * @return - List of Board objects.
     */
    public static List<GraphBoard> loadBoards(String filename)
    {
        JSONParser jsonParser = new JSONParser();
        ArrayList<GraphBoard> graphBoards = new ArrayList<>();

        try (FileReader reader = new FileReader(filename)) {

            JSONArray data = (JSONArray) jsonParser.parse(reader);
            for(Object o : data) {
                GraphBoard newGraphBoard = new GraphBoard();
                newGraphBoard.loadBoard((JSONObject) o);
                graphBoards.add(newGraphBoard);
            }

        }catch (IOException | ParseException e) {
            e.printStackTrace();
        }

        return graphBoards;
    }

    /**
     * Loads board nodes from a JSON file.
     * @param board - board to load in JSON format
     */
    public void loadBoard(JSONObject board) {
        componentName = (String) board.get("id");
        String boardType = (String) board.get("type");
        String verticesKey = (String) board.get("verticesKey");
        String neighboursKey = (String) board.get("neighboursKey");
        int maxNeighbours = (int) (long) board.get("maxNeighbours");

        properties.put(Hash.GetInstance().hash("boardType"), new PropertyString(boardType));
        if (board.get("img") != null) {
            properties.put(imgHash, new PropertyString((String) board.get("img")));
        }

        JSONArray nodeList = (JSONArray) board.get("nodes");
        for(Object o : nodeList)
        {
            // Add nodes to board nodes
            JSONObject node = (JSONObject) o;
            BoardNode newBN = new BoardNode();
            newBN.loadBoardNode(node);
            newBN.setComponentName(((PropertyString)newBN.getProperty(nameHash)).value);
            newBN.setMaxNeighbours(maxNeighbours);
            boardNodes.add(newBN);
        }

        int _hash_neighbours_ = Hash.GetInstance().hash(neighboursKey);
        int _hash_vertices_ = Hash.GetInstance().hash(verticesKey);

        for (BoardNode bn : boardNodes) {
            Property p = bn.getProperty(_hash_neighbours_);
            if (p instanceof PropertyStringArray) {
                PropertyStringArray psa = (PropertyStringArray) p;
                for (String str : psa.getValues()) {
                    BoardNode neigh = this.getNodeByProperty(_hash_vertices_, new PropertyString(str));
                    if (neigh != null) {
                        bn.addNeighbour(neigh);
                        neigh.addNeighbour(bn);
                    }
                }
            }
        }
    }
}

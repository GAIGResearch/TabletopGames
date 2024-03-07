package core.components;

import core.AbstractGameState;
import core.CoreConstants;
import core.interfaces.IComponentContainer;
import core.properties.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import utilities.Hash;

import java.io.FileReader;
import java.io.IOException;
import java.util.*;

import static core.CoreConstants.imgHash;
import static core.CoreConstants.nameHash;

public class GraphBoard extends Component implements IComponentContainer<BoardNode> {

    // List of nodes in the board graph, mapping component ID to object reference
    protected Map<Integer, BoardNode> boardNodes;

    public GraphBoard(String name)
    {
        super(CoreConstants.ComponentType.BOARD, name);
        boardNodes = new HashMap<>();
    }

    public GraphBoard()
    {
        super(CoreConstants.ComponentType.BOARD);
        boardNodes = new HashMap<>();
    }

    public GraphBoard(String name, int ID)
    {
        super(CoreConstants.ComponentType.BOARD, name, ID);
        boardNodes = new HashMap<>();
    }

    GraphBoard(int ID)
    {
        super(CoreConstants.ComponentType.BOARD, ID);
        boardNodes = new HashMap<>();
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
        for (BoardNode bn: boardNodes.values()) {
            BoardNode bnCopy = new BoardNode(bn.getMaxNeighbours(), "", bn.getComponentID());
            bn.copyComponentTo(bnCopy);
            nodeCopies.put(bn.getComponentID(), bnCopy);
        }
        // Assign neighbours
        for (BoardNode bn: boardNodes.values()) {
            BoardNode bnCopy = nodeCopies.get(bn.getComponentID());
            for (BoardNode neighbour: bn.getNeighbours().keySet()) {
                bnCopy.addNeighbourWithCost(nodeCopies.get(neighbour.getComponentID()));
            }
            for (Map.Entry<BoardNode, Integer> e: bn.getNeighbourSideMapping().entrySet()) {
                bnCopy.addNeighbourWithCost(nodeCopies.get(e.getKey().componentID), e.getValue());
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
        for (BoardNode n : boardNodes.values()) {
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
    public Collection<BoardNode> getBoardNodes() {
        return boardNodes.values();
    }

    /**
     * Returns the node in the list which matches the given ID
     * @param id - ID of node to search for.
     * @return - node matching ID.
     */
    public BoardNode getNodeByID(int id) {
        return boardNodes.get(id);
    }

    /**
     * Sets the list of board nodes to the given list.
     * @param boardNodes - new list of board nodes.
     */
    public void setBoardNodes(List<BoardNode> boardNodes) {
        for (BoardNode bn: boardNodes) {
            this.boardNodes.put(bn.componentID, bn);
        }
    }
    public void setBoardNodes(Map<Integer, BoardNode> boardNodes) {
        this.boardNodes = boardNodes;
    }

    public void addBoardNode(BoardNode bn) {
        this.boardNodes.put(bn.getComponentID(), bn);
    }

    public void removeBoardNode(BoardNode bn) {
        this.boardNodes.remove(bn.getComponentID());
    }

    public void breakConnection(AbstractGameState gs, BoardNode bn1, BoardNode bn2) {
        bn1.removeNeighbour(bn2);
        bn2.removeNeighbour(bn1);

        // Check if they have at least 1 more neighbour on this board. If not, remove node from this board
        boolean inBoard = false;
        for (BoardNode n: bn1.getNeighbours().keySet()) {
            if (boardNodes.containsKey(n.componentID)) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn1.componentID);

        inBoard = false;
        for (BoardNode n: bn2.getNeighbours().keySet()) {
            if (boardNodes.containsKey(n.componentID)) {
                inBoard = true;
                break;
            }
        }
        if (!inBoard) boardNodes.remove(bn2.componentID);
    }

    public void addConnection(BoardNode bn1, BoardNode bn2) {
        bn1.addNeighbourWithCost(bn2);
        bn2.addNeighbourWithCost(bn1);
        if (!boardNodes.containsKey(bn1.componentID)) {
            boardNodes.put(bn1.componentID, bn1);
        }
        if (!boardNodes.containsKey(bn2.componentID)) {
            boardNodes.put(bn1.componentID, bn2);
        }
    }

    public void addConnection(BoardNode bn1, BoardNode bn2, int edgeValue) {
        bn1.addNeighbourWithCost(bn2, edgeValue);
        bn2.addNeighbourWithCost(bn1, edgeValue);
        if (!boardNodes.containsKey(bn1.componentID)) {
            boardNodes.put(bn1.componentID, bn1);
        }
        if (!boardNodes.containsKey(bn2.componentID)) {
            boardNodes.put(bn1.componentID, bn2);
        }
    }

    public void addConnection(int bn1id, int bn2id) {
        BoardNode bn1 = boardNodes.get(bn1id);
        BoardNode bn2 = boardNodes.get(bn2id);
        addConnection(bn1, bn2);
    }

    public void addConnection(int bn1id, int bn2id, int edgeValue) {
        BoardNode bn1 = boardNodes.get(bn1id);
        BoardNode bn2 = boardNodes.get(bn2id);
        addConnection(bn1, bn2, edgeValue);
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

        properties.put(Hash.GetInstance().hash("boardType"), new PropertyString("boardType", boardType));
        if (board.get("img") != null) {
            properties.put(imgHash, new PropertyString("img", (String) board.get("img")));
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
            boardNodes.put(newBN.componentID, newBN);
        }

        int _hash_neighbours_ = Hash.GetInstance().hash(neighboursKey);
        int _hash_vertices_ = Hash.GetInstance().hash(verticesKey);

        for (BoardNode bn : boardNodes.values()) {
            Property p = bn.getProperty(_hash_neighbours_);
            if (p instanceof PropertyStringArray) {
                PropertyStringArray psa = (PropertyStringArray) p;
                for (String str : psa.getValues()) {
                    BoardNode neigh = this.getNodeByProperty(_hash_vertices_, new PropertyString(str));
                    if (neigh != null) {
                        bn.addNeighbourWithCost(neigh);
                        neigh.addNeighbourWithCost(bn);
                    }
                }
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof GraphBoard) {
            GraphBoard other = (GraphBoard) o;
            return componentID == other.componentID && other.boardNodes.equals(boardNodes);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(componentID, boardNodes);
    }

    @Override
    public List<BoardNode> getComponents() {
        return new ArrayList<>(getBoardNodes());
    }

    public Map<Integer, BoardNode> getBoardNodeMap() {
        return boardNodes;
    }

    @Override
    public CoreConstants.VisibilityMode getVisibilityMode() {
        return CoreConstants.VisibilityMode.VISIBLE_TO_ALL;
    }
}
